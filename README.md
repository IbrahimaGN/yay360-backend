# YAY360 — Backend

**Thématique :** Conception et développement d'une plateforme intelligente de suivi de la santé maternelle
destinée aux femmes enceintes et aux professionnels de santé.

## Stack technique

| Composant | Technologie |
|---|---|
| Framework | Spring Boot 3.3.4 |
| Langage | Java 21 |
| Sécurité | Spring Security + JWT (JJWT 0.12.x) |
| Base de données | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Documentation API | Swagger / OpenAPI (springdoc) |
| Build | Maven (wrapper `mvnw` fourni) |
| Conteneurisation | Docker / Docker Compose |
| Tests | JUnit 5 + Mockito + AssertJ |

## Architecture

Architecture en couches strictes, appliquée à chaque module :

```
model → repo → service → helper → mapper → dto → controller
```

- **model** : entités JPA
- **repo** : interfaces `JpaRepository`
- **service** : interface + implémentation, orchestration simple (délègue au helper)
- **helper** : toute la logique métier + annotations `@PreAuthorize`
- **mapper** : mapping manuel entité ↔ DTO (pas de MapStruct)
- **dto** : package plat
- **controller** : endpoints REST

## Modules livrés

| # | Module | Contenu |
|---|---|---|
| 1 | Socle | Utilisateurs, rôles, authentification JWT, comptes professionnels créés par l'admin |
| 2 | Grossesse | Déclaration, calcul automatique DPA / semaine / trimestre, calendrier 40 semaines, conseils |
| 3 | Suivi médical | Consultations prénatales, rendez-vous (pris par la patiente ou planifiés par le soignant), établissements |
| 4 | Dossier familial | Invitation du conjoint (code à usage unique, 72 h) et accès partagé au dossier |
| 5 | Alertes | Triage des symptômes, prise en charge, résolution tracée |
| 6 | Intelligence clinique | Moteur de règles explicables : analyse des constantes, triage contextualisé, plan CPN OMS, facteurs de risque |
| 7 | Pilotage | Tableau de bord : ratio de grossesses à risque, taux de résolution, délai de prise en charge |
| 8 | Qualité | Cloisonnement des dossiers, migrations Flyway, Docker avec healthcheck, tests unitaires et d'intégration |

## Sécurité et cloisonnement des dossiers

- **Comptes** : l'inscription publique crée uniquement des comptes `FEMME_ENCEINTE`. Les médecins, sages-femmes
  et administrateurs sont créés par un administrateur (`POST /utilisateurs/professionnels`) avec un mot de passe
  temporaire : tant qu'il n'est pas changé, le compte ne peut rien faire d'autre (403). Avec un serveur
  SMTP, ce mot de passe n'est envoyé qu'au professionnel. Le conjoint rejoint le dossier sur invitation.
- **Cloisonnement** (`AccesDossierHelper`) : un soignant n'accède qu'aux grossesses qu'il **suit** — établissement
  de suivi dont il est membre, ou lien de soin direct (rendez-vous assigné, consultation, alerte prise en charge).
  L'administrateur a une lecture de supervision mais jamais le contenu clinique des consultations.
- **Alertes sans suivi** : l'alerte d'une patiente rattachée à aucune équipe est diffusée à tous les soignants
  **sans donnée identifiante** ; le soignant qui la prend en charge devient son soignant. Une patiente
  rattachée à un établissement où aucun soignant actif n'exerce est traitée comme « sans suivi ».
- **Rattachement** (`PUT /grossesses/{id}/rattacher`) : réservé aux grossesses sans suivi, avec le téléphone
  de la patiente comme preuve de présence (recherche préalable : `GET /grossesses/recherche`).
- **Authentification** : mots de passe BCrypt (8 caractères minimum), verrouillage 15 min après 5 échecs,
  messages d'erreur identiques quel que soit l'email (pas d'énumération), jetons de réinitialisation et
  d'invitation stockés hachés (SHA-256), révocation immédiate des JWT au changement de mot de passe ou à la
  désactivation du compte.
- **Erreurs** : format JSON uniforme, 401 / 403 / 404 / 409 précis ; aucune donnée médicale ni jeton dans les logs.

## Intelligence clinique (moteur de règles explicables)

`MoteurReglesCliniques` ne pose aucun diagnostic : il signale, hiérarchise et suggère. Chaque règle est nommée,
sourcée et accompagnée de sa justification.

| Fonction | Règles | Point d'entrée |
|---|---|---|
| Analyse des constantes à chaque consultation | PA ≥ 140/90 (≥ 160/110 : sévère, prééclampsie après 20 SA) ; fièvre ≥ 38 °C ; RCF hors 110–160/min ; hauteur utérine ± 3 cm de l'âge gestationnel (McDonald, 24–38 SA) ; prise de poids > 1 kg/semaine | `POST /consultations` → `signauxCliniques` + alerte automatique tracée |
| Triage contextualisé des symptômes | Table du cahier des charges, puis : signes de prééclampsie après 20 SA → URGENT ; grossesse à risque → +1 niveau ; 3 signalements en 7 jours → +1 niveau | `POST /alertes` → `justificationTriage` |
| Plan des consultations prénatales | 8 contacts OMS 2016 (12, 20, 26, 30, 34, 36, 38, 40 SA) ; contact réalisé, planifié, à venir ou en retard | `GET /grossesses/mon-plan-cpn`, `GET /grossesses/{id}/plan-cpn` |
| Facteurs de risque (aide à la décision) | Âge < 18 ou > 35 ans, grande multiparité, antécédents (césarienne, HTA, diabète…), signaux relevés en consultation, alertes urgentes répétées | `GET /grossesses/{id}/evaluation-risque` |

## Démarrage rapide

### 1. Configurer les secrets (une seule fois)

```bash
cp .env.example .env
```

Puis renseigner dans `.env` au minimum `DB_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` et `JWT_SECRET`
(32 caractères minimum, par exemple `openssl rand -base64 48`). Le fichier `.env` est ignoré par git.
L'API refuse de démarrer si `JWT_SECRET` est absent ou trop court.

### 2a. Avec Docker (recommandé)

```bash
docker compose up -d --build
docker compose ps        # yay360-api doit être "healthy"
```

- API : `http://localhost:8080` (santé : `http://localhost:8080/actuator/health`)
- Web : `http://localhost:4200`
- Swagger (si `YAY360_SWAGGER_ENABLED=true`) : `http://localhost:8080/swagger-ui.html`
- PostgreSQL : `localhost:5433` (le port 5432 est laissé au PostgreSQL éventuellement installé sur le poste)

### 2b. En local (sans Docker)

Prérequis : JDK 21 et une base PostgreSQL (par exemple celle du compose : `docker compose up -d yay360-db`).
Le Maven Wrapper est fourni, Maven n'a pas besoin d'être installé :

```bash
./mvnw spring-boot:run      # Windows : mvnw.cmd spring-boot:run
```

Le fichier `.env` est lu automatiquement par l'API (`spring.config.import`).

## Compte administrateur

Créé automatiquement au premier démarrage (`DataInitializer`) avec l'email `admin@yay360.sn`.

- Si `YAY360_ADMIN_PASSWORD` est défini, il sert de mot de passe initial.
- Sinon, un mot de passe aléatoire est généré et affiché **une seule fois** dans les logs
  (`docker logs yay360-api | grep administrateur`).

Changez-le dès la première connexion. Aucun mot de passe n'est écrit dans le code.

## Comptes et données de démonstration

Désactivés par défaut. Pour une démonstration ou une soutenance, définir dans `.env` :
`YAY360_DEMO_DATA_ENABLED=true` (mot de passe commun : `YAY360_DEMO_PASSWORD` ; s'il est vide, un mot de passe est généré et affiché une fois dans les logs).
Sont alors créés une médecin, une sage-femme, deux patientes, un conjoint, des grossesses,
rendez-vous, consultation, alertes et notifications, tous fictifs.

| Profil | Email |
|---|---|
| Médecin | `medecin.demo@yay360.sn` |
| Sage-femme | `sagefemme.demo@yay360.sn` |
| Patiente | `patiente.demo@yay360.sn` |
| Deuxième patiente | `patiente2.demo@yay360.sn` |
| Conjoint | `conjoint.demo@yay360.sn` |

Ne jamais activer ces données sur un environnement réel.

## Documentation API (Swagger)

Une fois l'application démarrée :

- Swagger UI : `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON : `http://localhost:8080/api-docs`

Cliquer sur **Authorize** et coller le token JWT obtenu via `POST /auth/login`
(sans le préfixe `Bearer`).

## Rôles de la plateforme

| Rôle | Description |
|---|---|
| `ADMIN` | Comptes professionnels, établissements, tableau de bord ; lecture de supervision sans contenu clinique |
| `FEMME_ENCEINTE` | Déclaration et suivi de grossesse, choix de l'établissement, rendez-vous, signalements, plan CPN |
| `CONJOINT` | Compte personnel lié au dossier de sa compagne : lecture du suivi, rendez-vous, signalement de symptômes |
| `SAGE_FEMME` | Patientes suivies : consultations, rendez-vous, prise en charge des alertes |
| `MEDECIN` | Idem sage-femme + déclaration de grossesse à risque et clôture de la grossesse |

## Tâches planifiées

| Tâche | Fréquence | Description |
|---|---|---|
| Rappel de rendez-vous | Toutes les heures (fuseau Africa/Dakar) | Rappelle une seule fois chaque rendez-vous des 24 prochaines heures |
| Conseils hebdomadaires | Chaque lundi à 9 h | Conseils de la semaine en cours |
| Consultations prénatales manquées | Chaque lundi à 9 h | Un rappel par contact OMS en retard |

Chaque envoi s'exécute dans sa propre transaction : un échec n'interrompt pas les autres envois.

## Dossier familial

- La femme invite son conjoint avec `POST /famille/inviter-conjoint` (`{ "email": "conjoint@example.com" }`).
  La réponse contient un **code à usage unique valable 72 h**, qu'elle peut transmettre elle-même
  (SMS, WhatsApp) ; il est aussi envoyé par email si un serveur SMTP est configuré.
- Le conjoint crée son compte avec `POST /auth/accept-invitation-conjoint` (`token`, `email`, `nom`, `prenom`,
  `password`, `telephone`, `dateNaissance`).

## Base de données et migrations

Le schéma est versionné avec **Flyway** (`src/main/resources/db/migration`) ; Hibernate se contente de le
valider (`ddl-auto: validate`).

- `V1__schema_initial.sql` : tables du modèle.
- `V2__index_et_contraintes.sql` : index de performance, une seule grossesse en cours par patiente,
  unicité des emails sans tenir compte de la casse.

Une base créée avant Flyway est reconnue automatiquement comme étant au niveau V1 (`baseline-on-migrate`).
Toute évolution du modèle passe par une nouvelle migration `V3__description.sql`.

## Variables d'environnement

Toutes sont définies dans `.env` (modèle : `.env.example`).

| Variable | Obligatoire | Description |
|---|---|---|
| `JWT_SECRET` | Oui | Clé de signature des jetons, 32 caractères minimum |
| `DB_PASSWORD` | Oui (Docker) | Mot de passe PostgreSQL du conteneur |
| `SPRING_DATASOURCE_PASSWORD` | Oui (local) | Mot de passe PostgreSQL pour l'API lancée hors Docker |
| `SPRING_DATASOURCE_URL` | Non | URL JDBC (défaut : `jdbc:postgresql://localhost:5432/yay360`) |
| `DB_USER` / `SPRING_DATASOURCE_USERNAME` | Non | Utilisateur PostgreSQL (défaut : `postgres`) |
| `DB_HOST_PORT` | Non | Port de la base Docker sur le poste (défaut : `5433`) |
| `YAY360_ADMIN_PASSWORD` | Non | Mot de passe initial de l'admin (sinon généré) |
| `YAY360_DEMO_DATA_ENABLED` | Non | `true` pour créer les données de démonstration (défaut : `false`) |
| `YAY360_DEMO_PASSWORD` | Non | Mot de passe des comptes de démonstration |
| `JWT_EXPIRATION` | Non | Durée de validité d'un jeton en ms (défaut : 24 h) |
| `YAY360_CORS_ORIGINS` | Non | Origines autorisées à appeler l'API, séparées par des virgules |
| `YAY360_FRONTEND_URL` | Non | URL du web, utilisée dans les liens envoyés par email |
| `YAY360_SWAGGER_ENABLED` | Non | `false` pour masquer Swagger en production |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | Non | Serveur d'envoi des emails ; sans `SMTP_HOST`, aucun email n'est envoyé |
| `YAY360_MAIL_JOURNALISER_LIENS` | Non | Développement uniquement : écrit les liens de réinitialisation dans les logs |

## Tests

```bash
./mvnw test
```

**Tests unitaires** (JUnit 5, Mockito, AssertJ) : authentification et verrouillage, jetons JWT, création des
comptes professionnels, cloisonnement des dossiers, cycle de vie des alertes, transitions des rendez-vous,
cohérence des consultations, moteur de règles cliniques (seuils, triage, plan CPN, facteurs de risque),
tâches planifiées, calculs obstétricaux.

**Tests d'intégration** (`SecuriteIntegrationIT`) : démarrent l'application complète (Flyway, filtre JWT,
`@PreAuthorize`) sur une base PostgreSQL vide dédiée. Ils sont ignorés si aucune base n'est fournie :

```bash
# Base vide dédiée, par exemple dans le conteneur du compose
docker exec yay360-db psql -U postgres -c "create database yay360_it;"
YAY360_IT_DATASOURCE_URL=jdbc:postgresql://localhost:5433/yay360_it \
YAY360_IT_DATASOURCE_PASSWORD=<mot de passe> ./mvnw test -Dtest=SecuriteIntegrationIT
```

## Avertissement médical

Conformément au cahier des charges, toute réponse contenant des recommandations
personnalisées (suivi hebdomadaire, alertes médicales) inclut la mention :

> Ces recommandations ne remplacent jamais l'avis d'un professionnel de santé.
