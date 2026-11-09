"""Parcours de connexion mobile (téléphone, code par e-mail, PIN, rester connecté) contre l'API LOCALE.

Usage : python parcours_pin.py [url de l'API] [téléphone d'un compte de démonstration sans PIN]
        (par défaut : http://localhost:8080 et +221770010013, le compte d'Awa Ndiaye)

Conditions : plateforme Docker locale lancée, aucune clé Brevo ni SMTP, et YAY360_MAIL_JOURNALISER_LIENS=true
dans .env : le code est alors lu dans les journaux du conteneur yay360-api (mode développement uniquement).
Après le test, remettre le compte dans son état initial : nettoyer_parcours_pin.sql.
"""
import json, re, subprocess, sys, time, urllib.request, urllib.error
B = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080').rstrip('/')
TEL = sys.argv[2] if len(sys.argv) > 2 else '+221770010013'
if not any(h in B for h in ('://localhost', '://127.0.0.1')):
    sys.exit("Refus : ce parcours lit les codes dans les journaux Docker. Il ne s'exécute que contre une API locale.")
def post(path, body):
    req = urllib.request.Request(B + path, data=json.dumps(body).encode(), method='POST', headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req) as r: return r.status, json.loads(r.read() or b'{}')
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b'{}')
def ok(label, cond, detail=''):
    print(('OK ' if cond else 'ÉCHEC'), '|', label, '|', detail)
s, r = post('/auth/mobile/demarrer', {'telephone': TEL})
ok('numéro sans PIN : code envoyé', s == 200 and r.get('etape') == 'CODE_ENVOYE', f"HTTP {s}, e-mail masqué {r.get('emailMasque')}")
time.sleep(2)
logs = subprocess.run(['docker', 'logs', '--since', '30s', 'yay360-api'], capture_output=True, text=True, encoding='utf-8', errors='replace')
codes = re.findall(r'Lien ou code : (\d{6})', logs.stdout + logs.stderr)
ok('code lisible dans les journaux (mode développement)', bool(codes))
code = codes[-1]
s, r = post('/auth/mobile/verifier-code', {'telephone': TEL, 'code': '000000' if code != '000000' else '111111'})
ok('mauvais code refusé', s == 401, r.get('message'))
s, r = post('/auth/mobile/verifier-code', {'telephone': TEL, 'code': code})
ok('bon code accepté', s == 200 and r.get('jetonPin'), f"HTTP {s}")
jeton_pin = r.get('jetonPin')
s, r = post('/auth/mobile/pin', {'jetonPin': jeton_pin, 'pin': '1234', 'resterConnecte': True})
ok('PIN trop simple refusé', s == 400, r.get('message'))
s, r = post('/auth/mobile/pin', {'jetonPin': jeton_pin, 'pin': '2580', 'resterConnecte': True})
ok('PIN défini, session longue ouverte', s == 200 and r.get('token') and r.get('jetonRafraichissement'), f"rôle {r.get('role')}")
s, r = post('/auth/mobile/pin', {'jetonPin': jeton_pin, 'pin': '2580', 'resterConnecte': True})
ok('jeton de PIN à usage unique', s == 401, f"HTTP {s}")
s, r = post('/auth/mobile/demarrer', {'telephone': TEL})
ok('reconnexion : le PIN est demandé, aucun e-mail', s == 200 and r.get('etape') == 'PIN')
s, r = post('/auth/mobile/connexion', {'telephone': TEL, 'pin': '9753', 'resterConnecte': False})
ok('mauvais PIN refusé (message générique)', s == 401, r.get('message'))
s, r = post('/auth/mobile/connexion', {'telephone': TEL, 'pin': '2580', 'resterConnecte': True})
ok('téléphone + PIN : connectée', s == 200 and r.get('jetonRafraichissement'))
r1 = r['jetonRafraichissement']
s, r = post('/auth/mobile/rafraichir', {'jetonRafraichissement': r1})
ok('rester connectée : rotation du jeton', s == 200 and r.get('jetonRafraichissement') not in (None, r1))
r2 = r['jetonRafraichissement']
s, r = post('/auth/mobile/rafraichir', {'jetonRafraichissement': r1})
ok('réutilisation de l’ancien jeton refusée', s == 401)
s, r = post('/auth/mobile/rafraichir', {'jetonRafraichissement': r2})
ok('… et toute la session est fermée (jeton récent révoqué aussi)', s == 401)
