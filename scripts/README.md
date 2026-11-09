# Scripts de vérification (plateforme locale uniquement)

Ces scripts rejouent des parcours réels contre la **plateforme Docker locale** (`docker compose up -d` dans
`yay360-backend`), avec les **comptes de démonstration fictifs**. Ils refusent de s'exécuter contre une autre
adresse que `localhost` : ils créent des données de test et, pour la connexion mobile, lisent le code dans les
journaux du conteneur.

| Script | Ce qu'il vérifie | Nettoyage |
|---|---|---|
| `scenario_e2e.py` | parcours des alertes : la patiente signale un symptôme, la sage-femme le prend en charge, le médecin le résout ; cloisonnement entre structures, droits du conjoint, refus anonyme (20 vérifications) | `nettoyer_e2e.sql` |
| `parcours_pin.py` | connexion mobile : code par e-mail, PIN (refus d'un PIN trop simple), usage unique, reconnexion par PIN, rotation et révocation de la session longue (13 vérifications) | `nettoyer_parcours_pin.sql` |

## Pré-requis

- Plateforme lancée : `docker compose up -d` (dans `yay360-backend`).
- `.env` : `YAY360_DEMO_DATA_ENABLED=true`. Pour `parcours_pin.py` : `YAY360_MAIL_JOURNALISER_LIENS=true`, sans `BREVO_API_KEY` ni `SMTP_HOST`. Le code est alors écrit dans les journaux au lieu d'être envoyé.
- Python 3, sans dépendance supplémentaire.

## Utilisation (Git Bash, depuis `yay360-backend`)

```bash
# Alertes : lit le mot de passe de démonstration dans .env (jamais affiché)
PYTHONIOENCODING=utf-8 python -I scripts/scenario_e2e.py .env http://localhost:4200/api
docker exec -i yay360-db psql -U postgres -d yay360 < scripts/nettoyer_e2e.sql

# Connexion mobile, avec le compte d'Awa Ndiaye (+221770010013, sans PIN)
PYTHONIOENCODING=utf-8 python -I scripts/parcours_pin.py http://localhost:8080 +221770010013
docker exec -i yay360-db psql -U postgres -d yay360 -v tel=+221770010013 < scripts/nettoyer_parcours_pin.sql
```

Chaque vérification affiche `OK` ou `ÉCHEC`. Lancez toujours le nettoyage après le script, pour garder les
données de démonstration intactes avant une soutenance.
