# YAY360 — Backend

**Thématique :** Conception et développement d'une plateforme intelligente de suivi de la santé maternelle
destinée aux femmes enceintes.

## Stack technique

| Composant | Technologie |
|---|---|
| Framework | Spring Boot 3.3.4 |
| Langage | Java 17 (compilé, testé sur JDK 21) |
| Sécurité | Spring Security + JWT (JJWT 0.12.x) |
| Base de données | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Documentation API | Swagger / OpenAPI (springdoc) |
| Build | Maven |
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
| 1 | Socle | Utilisateurs, rôles, authentification JWT |
| 2 | Grossesse intelligente | Déclaration, calcul auto DPA/semaine/trimestre, calendrier, conseils personnalisés |
| 3 | Suivi médical | Consultations, rendez-vous, établissements de santé |
| 4 | Dossier familial | Invitation du conjoint avec un compte distinct et accès au dossier prénatal partagé |
| 5 | Assistant IA | Chat textuel ou vocal d'information générale sur la grossesse, avec orientation vers un professionnel et consentement préalable |
| 6 | Intelligence & sécurité | Alertes médicales, notifications automatiques, rappels planifiés |
| 7 | Pilotage | Tableau de bord statistique administrateur |
| 8 | Finitions | Swagger/JWT, Docker, tests, documentation |

## Démarrage rapide

### Avec Docker (recommandé)

```bash
docker compose up --build
```

L'API démarre sur `http://localhost:8080`, PostgreSQL sur le port `5432`.

### En local (sans Docker)

Prérequis : JDK 21, Maven, PostgreSQL 16 en local (base `yay360`, user `postgres`, mot de passe `admin123`).

```bash
mvn clean install
mvn spring-boot:run
```

## Compte administrateur par défaut

Créé automatiquement au premier démarrage (`DataInitializer`) :

- Email : `admin@yay360.sn`
- Mot de passe : `Admin@2026`

## Documentation API (Swagger)

Une fois l'application démarrée :

- Swagger UI : `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON : `http://localhost:8080/api-docs`

Cliquer sur **Authorize** et coller le token JWT obtenu via `POST /auth/login`
(sans le préfixe `Bearer`).

## Rôles de la plateforme

| Rôle | Description |
|---|---|
| `ADMIN` | Administration générale, statistiques, gestion des comptes et établissements |
| `FEMME_ENCEINTE` | Déclaration et suivi de grossesse, rendez-vous, alertes, notifications |
| `CONJOINT` | Compte personnel lié à une femme enceinte ; accès au même dossier prénatal, aux consultations, rendez-vous, alertes et notifications |
| `SAGE_FEMME` | Consultations, rendez-vous, suivi des patientes |
| `MEDECIN` | Idem sage-femme + diagnostic, prescriptions, déclaration de grossesse à risque |

## Tâches planifiées (rappels automatiques)

| Tâche | Fréquence | Description |
|---|---|---|
| Rappel de rendez-vous | Tous les jours à 8h | Notifie les rendez-vous prévus dans les 24h |
| Conseils hebdomadaires | Chaque lundi à 9h | Envoie les conseils personnalisés de la semaine en cours |

## Dossier familial

- Une femme connectée peut inviter son conjoint avec `POST /famille/inviter-conjoint` et un corps `{ "email": "conjoint@example.com" }`.
- Le conjoint crée son propre compte avec `POST /auth/accept-invitation-conjoint`, en fournissant `token`, `email`, `nom`, `prenom`, `password`, `telephone` et `dateNaissance` (format `YYYY-MM-DD`). Le compte reste lié au dossier de la femme ; il n'a pas accès aux autres dossiers.
- L'envoi d'e-mail n'étant pas encore configuré, le token d'invitation de 72 heures est journalisé dans la console pour les tests. Il devra être transmis au conjoint par le futur fournisseur d'e-mail.

## Variables d'environnement (profil Docker)

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur base de données |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe base de données |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Stratégie de génération du schéma |
| `YAY360_ASSISTANT_ENABLED` | Active l'assistant IA (`true` ou `false`, désactivé par défaut) |
| `YAY360_ASSISTANT_ENDPOINT` | URL de l'API de chat compatible OpenAI |
| `YAY360_ASSISTANT_API_KEY` | Clé secrète du fournisseur IA |
| `YAY360_ASSISTANT_MODEL` | Identifiant du modèle compatible avec le fournisseur choisi |
| `YAY360_ASSISTANT_TRANSCRIPTION_ENDPOINT` / `YAY360_ASSISTANT_TRANSCRIPTION_MODEL` | Endpoint et modèle de transcription audio |
| `YAY360_ASSISTANT_TRANSCRIPTION_LANGUAGE` | Langue attendue pour la transcription (`fr` par défaut) |
| `YAY360_ASSISTANT_SPEECH_ENDPOINT` / `YAY360_ASSISTANT_SPEECH_MODEL` / `YAY360_ASSISTANT_SPEECH_VOICE` | Endpoint, modèle et voix de synthèse audio |
| `YAY360_ASSISTANT_MAX_AUDIO_BYTES` | Taille maximale d'un fichier audio en octets (10 Mio par défaut) |

## Tests

```bash
mvn test
```

Tests unitaires couvrant la logique métier critique :
- Calculs obstétricaux automatiques (DPA, semaine, trimestre, jours restants)
- Authentification (inscription, connexion, comptes désactivés)
- Déclaration de grossesse (unicité, validation, calculs)
- Invitations et contrôle d'accès au dossier familial
- Consentement, taille et format des demandes vocales
- Initialisation des étapes de grossesse et des conseils prénataux

## Avertissement médical

Conformément au cahier des charges, toute réponse contenant des recommandations
personnalisées (suivi hebdomadaire, alertes médicales) inclut la mention :

> Ces recommandations ne remplacent jamais l'avis d'un professionnel de santé.

## Assistant IA textuel et vocal

- L'API `POST /assistant-ia/chat` est réservée aux rôles `FEMME_ENCEINTE` et `CONJOINT` et nécessite un JWT.
- Le corps comprend `message`, `canal` (`TEXTE` ou `VOIX`), `consentementTransmission: true` et un `historique` optionnel d'au plus 12 messages alternant `user` puis `assistant`. Le consentement doit être demandé à l'utilisatrice avant chaque appel.
- Les messages et l'historique soumis sont envoyés au fournisseur externe configuré. Le backend ne charge pas le dossier médical et ne conserve pas l'historique de chat en base. Ne pas inclure de données directement identifiantes ; afficher la notice de confidentialité avant de demander le consentement.
- Le flux vocal complet est : `POST /assistant-ia/audio/transcription` avec multipart `audio` + `consentementTransmission=true`, puis envoyer la transcription obtenue à `POST /assistant-ia/chat` (`canal: "VOIX"`, consentement vrai), enfin transmettre `texteALire` à `POST /assistant-ia/audio/synthese-vocale` avec `consentementTransmission: true` pour recevoir un MP3 (`audio/mpeg`). La reconnaissance et la synthèse sont donc traitées par le backend et le fournisseur configuré, non par l'appareil. Les trois opérations sont distinctes pour permettre à la personne de vérifier la transcription et la réponse avant lecture.
- La transcription accepte `audio/mpeg`, `audio/mp3`, `audio/mp4`, `audio/m4a`, `audio/wav`, `audio/webm`, `audio/ogg` et `audio/flac` jusqu'à 10 Mio. Les fichiers ne sont pas enregistrés par YAY360 ; leur contenu est transmis au fournisseur externe avec consentement explicite. La transcription n'est pas conservée par le backend.
- Le fournisseur n'est pas contacté tant que `YAY360_ASSISTANT_ENABLED` n'est pas à `true` et qu'une clé d'API et un modèle ne sont pas configurés. Un endpoint compatible OpenAI peut être remplacé par `YAY360_ASSISTANT_ENDPOINT`.
- Pour Docker Compose, définissez les variables IA dans le shell avant `docker compose up`, ou dans un fichier `.env` à la racine du projet ; ne publiez jamais ce fichier contenant la clé.
- Le message système limite l'assistant aux informations générales sur la grossesse, interdit les diagnostics et prescriptions, oriente vers un professionnel pour les questions individuelles et indique de rechercher immédiatement une aide médicale en cas de signes potentiellement urgents pendant la grossesse. Un avertissement non généré par le modèle est également renvoyé à chaque réponse. Ces consignes réduisent les risques, mais ne garantissent pas l'exactitude de toutes les réponses IA.

Exemple de requête :

```json
{
  "message": "Quels signes devraient m'amener à appeler une sage-femme ?",
  "canal": "TEXTE",
  "consentementTransmission": true,
  "historique": []
}
```
