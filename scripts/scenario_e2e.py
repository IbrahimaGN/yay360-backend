"""Scénario de bout en bout YAY360 contre la plateforme Docker locale (données fictives).
Usage : python scenario_e2e.py <chemin .env> <url de base>  (ex. http://localhost:4200/api)
Le mot de passe de démonstration est lu dans .env et n'est jamais affiché."""
import datetime, json, pathlib, sys, urllib.request, urllib.error

# Le premier argument doit désigner un fichier .env existant (pas un chemin arbitraire).
fichier_env = pathlib.Path(sys.argv[1]).resolve(strict=True)
if fichier_env.name != '.env' or not fichier_env.is_file():
    sys.exit("Refus : le premier argument doit être le chemin d'un fichier .env.")
env = dict(l.split('=', 1) for l in fichier_env.read_text(encoding='utf-8').splitlines()
           if '=' in l and not l.lstrip().startswith('#'))
MDP = env['YAY360_DEMO_PASSWORD'].strip()
BASE = sys.argv[2].rstrip('/')
if not any(h in BASE for h in ('://localhost', '://127.0.0.1')):
    sys.exit("Refus : ce scénario crée des alertes de test. Il ne s'exécute que contre une plateforme locale.")
resultats = []

def appel(methode, chemin, jeton=None, corps=None):
    req = urllib.request.Request(BASE + chemin, method=methode,
                                 data=json.dumps(corps).encode() if corps is not None else None)
    req.add_header('Content-Type', 'application/json')
    if jeton:
        req.add_header('Authorization', 'Bearer ' + jeton)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            brut = r.read()
            return r.status, json.loads(brut) if brut else None
    except urllib.error.HTTPError as e:
        brut = e.read()
        try:
            return e.code, json.loads(brut)
        except Exception:
            return e.code, brut.decode(errors='replace')[:200]

def verifier(etape, condition, detail=''):
    resultats.append(condition)
    print(f"{'OK ' if condition else 'ÉCHEC'} | {etape}" + (f" | {detail}" if detail else ''))

def connexion(email):
    s, c = appel('POST', '/auth/login', corps={'email': email, 'password': MDP})
    verifier(f"connexion {email}", s == 200 and c and c.get('token'), f"HTTP {s}, rôle {c.get('role') if isinstance(c, dict) else c}")
    return c.get('token') if isinstance(c, dict) else None

print("== 1. Patiente : Awa Ndiaye")
awa = connexion('awa.ndiaye.demo@yay360.sn')
s, g = appel('GET', '/grossesses/ma-grossesse', awa)
verifier("grossesse en cours", s == 200, f"id {g.get('id')}, {g.get('semaineActuelle', g.get('semaine'))} SA, à risque : {g.get('grossesseARisque')}" if s == 200 else f"HTTP {s}")
gid = g.get('id') if s == 200 else None
s, plan = appel('GET', '/grossesses/mon-plan-cpn', awa)
verifier("plan des 8 contacts OMS", s == 200, f"HTTP {s}, clés {list(plan)[:6] if isinstance(plan, dict) else type(plan).__name__}")

print("== 2. Signalement d'un symptôme")
s, a = appel('POST', '/alertes', awa, {'typeSymptome': 'MAUX_DE_TETE_INTENSES', 'grossesseId': gid,
                                       'description': 'Scénario de test e2e (données fictives)'})
verifier("alerte créée", s == 201, f"HTTP {s}")
aid = a.get('id') if s == 201 else None
if s == 201:
    print(f"      niveau : {a['niveauGravite']} | statut : {a['statut']}")
    print(f"      recommandation : {a['recommandation']}")
    for r in a.get('justificationTriage') or []:
        print(f"      règle : {r}")
    print(f"      avertissement : {a['avertissement']}")
s, _ = appel('POST', '/alertes', awa, {'typeSymptome': 'INCONNU'})
verifier("symptôme inconnu refusé", s == 400, f"HTTP {s}")

print("== 3. Sage-femme du même établissement : Ndèye Fatou Diop")
sf = connexion('ndeye.fatou.diop.demo@yay360.sn')
s, liste = appel('GET', '/alertes?statut=NOUVELLE', sf)
visible = s == 200 and any(x.get('id') == aid for x in (liste if isinstance(liste, list) else liste.get('content', [])))
verifier("l'alerte apparaît dans sa liste", visible, f"HTTP {s}")
s, a2 = appel('PUT', f'/alertes/{aid}/prendre-en-charge', sf)
verifier("prise en charge", s == 200 and a2.get('statut') == 'PRISE_EN_CHARGE', f"HTTP {s}, statut {a2.get('statut') if isinstance(a2, dict) else a2}")

print("== 4. Cloisonnement : sage-femme d'un autre établissement (Astou Cissé)")
autre = connexion('astou.cisse.demo@yay360.sn')
s, _ = appel('GET', f'/alertes/{aid}', autre)
verifier("accès refusé à l'alerte d'une autre structure", s in (403, 404), f"HTTP {s}")

print("== 5. Médecin : Mamadou Diallo résout l'alerte")
med = connexion('mamadou.diallo.demo@yay360.sn')
s, _ = appel('PUT', f'/alertes/{aid}/resoudre', med, {'notesPriseEnCharge': ''})
verifier("résolution sans observations refusée", s == 400, f"HTTP {s}")
s, a3 = appel('PUT', f'/alertes/{aid}/resoudre', med, {'notesPriseEnCharge': 'Test e2e : patiente rappelée, TA contrôlée.'})
verifier("résolution avec observations", s == 200 and a3.get('statut') == 'RESOLUE', f"HTTP {s}, statut {a3.get('statut') if isinstance(a3, dict) else a3}")

print("== 6. Retour côté patiente")
s, mes = appel('GET', '/alertes/mes-alertes', awa)
mienne = next((x for x in mes if x.get('id') == aid), None) if s == 200 else None
verifier("la patiente voit son alerte résolue", mienne is not None and mienne.get('statut') == 'RESOLUE',
         f"statut {mienne.get('statut') if mienne else None}")
verifier("prise en charge et résolution attribuées aux bons soignants",
         mienne is not None and 'Fatou' in (mienne.get('professionnelNomComplet') or '')
         and 'Mamadou' in (mienne.get('resoluParNomComplet') or ''),
         f"prise en charge : {mienne.get('professionnelNomComplet') if mienne else None}, résolue par : {mienne.get('resoluParNomComplet') if mienne else None}")

print("== 7. Conjoint : Ibrahima Fall")
cj = connexion('ibrahima.fall.demo@yay360.sn')
s, g2 = appel('GET', '/grossesses/ma-grossesse', cj)
verifier("le conjoint suit la grossesse de sa compagne", s == 200 and g2.get('id') == gid, f"HTTP {s}")
ddr = (datetime.date.today() - datetime.timedelta(days=70)).isoformat()
s, _ = appel('POST', '/grossesses', cj, {'dateDernieresRegles': ddr})
verifier("le conjoint ne peut pas déclarer de grossesse (requête valide)", s == 403, f"HTTP {s}")
s, _ = appel('GET', '/alertes?statut=NOUVELLE', cj)
verifier("le conjoint n'accède pas à la liste des soignants", s == 403, f"HTTP {s}")

print("== 8. Sans jeton")
s, _ = appel('GET', '/grossesses/ma-grossesse')
verifier("requête anonyme refusée", s == 401, f"HTTP {s}")

print(f"\nBilan : {sum(bool(r) for r in resultats)}/{len(resultats)} vérifications réussies")
