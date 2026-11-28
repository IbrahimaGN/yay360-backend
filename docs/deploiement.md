# Guide de déploiement YAY360

Ce guide met YAY360 en production sur trois services gratuits :

| Brique | Service | Rôle |
|---|---|---|
| Base de données | **Neon** (PostgreSQL 16, région Francfort) | stocke les données, schéma créé par Flyway |
| API Spring Boot | **Render** (Web Service gratuit, image Docker, région Francfort) | exécute l'image publiée sur GHCR |
| Portail web Angular | **Vercel** | sert le site et relaie `/api/*` vers Render |
| Application mobile | **Releases GitHub** | APK Android construit pour l'API de production |

```
Navigateur ──HTTPS──► Vercel (Angular) ── /api/* ──► Render (API, image ghcr.io/ibrahimagn/yay360-api:<sha>)
                                                         │ JDBC + SSL
Application mobile ──HTTPS──────────────────────────────►│
                                                         ▼
                                                      Neon (PostgreSQL)
```

## Principes

- **Aucun secret dans le code ni dans les journaux.** Chaque secret vit dans le coffre du service qui l'utilise :
  - variables d'environnement de **Render** ;
  - **secrets d'environnement GitHub** (`production`).
- **Les secrets de production sont neufs.** Ne réutilisez jamais les valeurs du fichier `.env` de développement.
- **Un commit donne un artefact.** L'image Docker est étiquetée avec le SHA du commit. C'est **cette image précise** qui est déployée, et `/actuator/info` affiche le SHA en ligne.
- **Validation humaine.** Les workflows `cd.yml` attendent l'approbation de l'environnement `production` avant de livrer.

## Gestion des secrets : choix et justification

### Le choix

Les secrets de production (clé de signature JWT, mots de passe, clé Brevo, accès à la base, jetons de déploiement) ne sont stockés **ni dans le code, ni dans l'image Docker, ni dans un fichier du dépôt**. Chacun est rangé dans le coffre du service qui en a besoin :

| Coffre | Secrets qu'il garde | Qui les lit |
|---|---|---|
| **Secrets d'environnement GitHub** (`production`) | jetons de déploiement : `RENDER_DEPLOY_HOOK_URL`, `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID` | les workflows `cd.yml` des trois dépôts, uniquement après approbation |
| **Variables d'environnement Render** | secrets de l'API : `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD` (base Neon), `BREVO_API_KEY`, `YAY360_ADMIN_PASSWORD`, `YAY360_DEMO_PASSWORD` | l'API, au démarrage du conteneur |
| **Vercel** | aucun secret : le portail web est un site statique, et sa configuration est dans `vercel.json` | sans objet |

Les valeurs d'origine sont conservées hors ligne dans un **gestionnaire de mots de passe**, jamais dans un message ni dans un fichier du projet.

### Pourquoi ce choix est sûr

- **Chiffrement.** GitHub chiffre chaque secret dès sa saisie et le masque dans les journaux des workflows ; Render chiffre ses variables d'environnement.
- **Injection au dernier moment.** Les secrets ne sont fournis qu'à l'exécution. L'image publiée sur GHCR peut donc être publique sans rien révéler.
- **Validation humaine.** L'environnement GitHub `production` exige une approbation avant tout déploiement, et seule la branche `main` peut y accéder.
- **Chaîne d'approvisionnement maîtrisée.** Les actions tierces des workflows sont épinglées par SHA : une action compromise ne peut pas être substituée pour lire les secrets.
- **Secrets propres à la production.** Ils sont générés spécialement (`openssl rand`) et ne réutilisent jamais les valeurs de développement.
- **Moindre privilège.** Chaque service ne reçoit que les secrets dont il a besoin.

### Pourquoi pas un coffre dédié

| Option | Avantage | Raison de ne pas la retenir |
|---|---|---|
| **HashiCorp Vault / OpenBao** | secrets dynamiques, rotation automatique | serveur à héberger et à sécuriser soi-même : plus de risques que de bénéfices pour un développeur seul |
| **Infisical** (offre gratuite) | coffre central, historique des versions, journal d'accès, synchronisation vers GitHub et Render | service supplémentaire à configurer ; gain modeste à l'échelle du projet. Piste d'amélioration |
| **Doppler** (offre gratuite) | même principe de coffre central synchronisé | service propriétaire, même gain modeste |

Les coffres intégrés aux plateformes couvrent le besoin sans serveur ni service supplémentaire, et sans coût.

### Limites connues

- **Dispersion.** Les secrets sont répartis entre GitHub (trois dépôts) et Render : une modification se fait à la main à chaque endroit.
- **Pas de rotation automatique.** Un secret reste le même tant qu'il n'est pas changé manuellement.
- **Pas de journal d'accès centralisé.**
- **Le risque principal est le compte.** Quiconque prend le contrôle du compte GitHub ou Render accède à ses secrets ; un compte Vercel compromis permet de modifier le site.

### Mesures complémentaires

- Activer l'**authentification à deux facteurs** sur GitHub, Render, Vercel, Neon et Brevo. C'est la mesure la plus efficace.
- **Changer immédiatement** tout secret exposé par erreur (message, capture d'écran, commit), puis redéployer.
- Faire tourner `JWT_SECRET` et la clé Brevo **après la soutenance** : changer `JWT_SECRET` invalide toutes les sessions ouvertes.
- À terme, centraliser les secrets dans **Infisical** si le projet grandit.

## Ordre des opérations

L'ordre compte : la CD a besoin de l'environnement GitHub, et Render a besoin d'une première image publiée.

1. Générer les secrets de production.
2. Créer la base Neon.
3. Créer l'environnement `production` dans les 3 dépôts GitHub.
4. Fusionner le code de la CD dans `main` : la première image est publiée sur GHCR.
5. Rendre l'image GHCR publique.
6. Créer le service Render.
7. Brancher le deploy hook Render, puis déployer l'API.
8. Créer le projet Vercel et déployer le web.
9. Compléter le CORS de l'API et publier l'APK mobile.
10. Vérifier.

---

## 1. Générer les secrets de production

Dans **Git Bash**, lancez une commande par secret. Chaque exécution affiche une nouvelle valeur.

```bash
openssl rand -base64 48     # JWT_SECRET (au moins 32 caractères)
openssl rand -base64 18     # YAY360_ADMIN_PASSWORD
openssl rand -base64 12     # YAY360_DEMO_PASSWORD (mot de passe des 34 comptes de démonstration)
```

Gardez ces valeurs dans un **gestionnaire de mots de passe** (Bitwarden, KeePass, celui du navigateur…). Ne les mettez ni dans un fichier du dépôt, ni dans un message.

## 2. Base de données Neon

1. Créez un compte sur https://neon.tech, par exemple avec GitHub.
2. **Create project** :
   - **Name** : `yay360` ;
   - **Postgres version** : **16** ;
   - **Region** : **AWS Europe Central 1 (Frankfurt)**.
3. Dans **Databases**, créez une base nommée `yay360`. Vous pouvez aussi garder la base `neondb` proposée.
4. Ouvrez **Connect**, puis **désactivez « Connection pooling »**. Flyway a besoin d'une connexion directe : les verrous de migration ne fonctionnent pas à travers le pooler.
5. Relevez les trois informations affichées :
   - l'**hôte**, de la forme `ep-xxxx-xxxx.eu-central-1.aws.neon.tech` (sans `-pooler`) ;
   - l'**utilisateur**, par exemple `neondb_owner` ;
   - le **mot de passe**.
6. L'URL JDBC à donner à Render est alors :
   ```
   jdbc:postgresql://<hôte>/yay360?sslmode=require
   ```

Il n'y a rien à créer à la main : au premier démarrage, Flyway applique les migrations V1 à V7.

## 3. Environnement GitHub `production` (dans les 3 dépôts)

Pour `yay360-backend`, `yay360-web` et `yay360-mobile` :

1. Ouvrez **Settings → Environments → New environment**, nom `production`, puis **Configure environment**.
2. Cochez **Required reviewers**, ajoutez-vous (`IbrahimaGN`), puis **Save protection rules**.
3. Laissez **Prevent self-review** décoché. Sinon, seul sur le projet, vous ne pourriez jamais valider.
4. Dans **Deployment branches and tags**, choisissez **Selected branches and tags** → **Add deployment branch rule** → `main`.

Les secrets et variables de chaque environnement seront ajoutés aux étapes 7, 8 et 9.

> Créez ces environnements **avant** l'étape 4. Si un workflow démarre alors que l'environnement n'existe pas, GitHub le crée automatiquement, **sans** validateur.

## 4. Fusionner la CD dans `main`

Ouvrez la pull request `initial` → `main` de chaque dépôt et fusionnez-la une fois la CI verte. Dans le backend, la fusion lance **CD backend**, qui :

1. **publie l'image** `ghcr.io/ibrahimagn/yay360-api:<sha>` ;
2. **attend votre validation** pour déployer.

Pour ce premier passage, Render n'existe pas encore. Dans l'onglet **Actions**, ouvrez l'exécution **CD backend**, puis choisissez **Review deployments → Reject**. Vous relancerez le déploiement à l'étape 7.

Faites de même pour **CD web** et **CD mobile** s'ils démarrent avant que Vercel et la variable `API_URL` soient prêts.

## 5. Rendre l'image publique

Render doit pouvoir télécharger l'image sans identifiant.

1. Ouvrez https://github.com/IbrahimaGN?tab=packages → **yay360-api** → **Package settings**.
2. Dans **Danger Zone**, cliquez sur **Change visibility → Public**.

Le code est déjà public, et l'image ne contient aucun secret : ils sont fournis par Render au démarrage.

## 6. Service Render

1. Créez un compte sur https://render.com avec GitHub.
2. Cliquez sur **New → Web Service → Existing Image**.
   - **Image URL** : `ghcr.io/ibrahimagn/yay360-api:latest`
3. Renseignez :
   - **Name** : `yay360-api`. L'URL sera `https://yay360-api.onrender.com`. Si le nom est pris, notez l'URL réellement attribuée.
   - **Region** : **Frankfurt (EU Central)**
   - **Instance Type** : **Free**
4. Dans **Environment Variables**, ajoutez les variables du tableau ci-dessous. Elles sont chiffrées par Render.

| Variable | Valeur |
|---|---|
| `PORT` | `8080` |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<hôte Neon>/yay360?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | utilisateur Neon |
| `SPRING_DATASOURCE_PASSWORD` | mot de passe Neon |
| `JWT_SECRET` | valeur générée à l'étape 1 |
| `YAY360_ADMIN_PASSWORD` | valeur générée à l'étape 1 |
| `YAY360_DEMO_DATA_ENABLED` | `true` (à repasser à `false` après la soutenance) |
| `YAY360_DEMO_PASSWORD` | valeur générée à l'étape 1 |
| `YAY360_SWAGGER_ENABLED` | `false` |
| `YAY360_MAIL_JOURNALISER_LIENS` | `false` (ne jamais écrire de lien de réinitialisation dans les journaux) |
| `BREVO_API_KEY` | clé API Brevo (voir « E-mails » ci-dessous) : **indispensable** pour les codes de connexion mobile |
| `YAY360_MAIL_FROM` | l'adresse validée comme expéditeur dans Brevo |
| `YAY360_FRONTEND_URL` | provisoirement `https://yay360.vercel.app`, à corriger à l'étape 9 |
| `YAY360_CORS_ORIGINS` | provisoirement `https://yay360.vercel.app`, à corriger à l'étape 9 |

À quoi servent les deux options techniques :
- `PORT=8080` indique à Render le port sur lequel écoute Spring Boot.
- `JAVA_TOOL_OPTIONS` limite la mémoire Java pour qu'elle tienne dans les 512 Mo de l'offre gratuite, et accélère le démarrage sur 0,1 processeur.

5. Dans **Advanced** :
   - **Health Check Path** : `/actuator/health` ;
   - **Auto-Deploy** : **No**. Seule la CD GitHub déploie.
6. Cliquez sur **Deploy Web Service**. Le premier démarrage prend plusieurs minutes. Ouvrez l'onglet **Logs** et attendez :
   ```
   Successfully applied 7 migrations to schema "public", now at version v7
   [YAY360] Jeu de démonstration fictif chargé : 6 médecins, 6 sages-femmes, 10 patientes et 10 conjoints …
   Started YaY360Application in … seconds
   ```

## 6 bis. E-mails avec Brevo (codes de connexion mobile, invitations, mot de passe oublié)

Render gratuit **bloque l'envoi par SMTP** (ports 25, 465 et 587). L'API envoie donc ses e-mails par l'**API HTTPS de Brevo**, gratuite jusqu'à 300 e-mails par jour.

1. Créez un compte sur https://www.brevo.com.
2. **Expéditeur** : menu **Senders, Domains & Dedicated IPs → Senders → Add a sender**. Saisissez votre adresse e-mail, puis validez le lien reçu. Sans domaine à vous, c'est cette adresse qui apparaîtra comme expéditeur.
3. **Clé API** : menu **SMTP & API → API Keys → Generate a new API key** (nom : `yay360-render`). Copiez-la : elle ne sera plus affichée.
4. Dans Render → **Environment**, ajoutez :
   - `BREVO_API_KEY` = la clé (secret) ;
   - `YAY360_MAIL_FROM` = l'adresse validée à l'étape 2.

   Enregistrez avec **Save, rebuild and deploy**.
5. Test : dans l'application mobile, saisissez le numéro d'un compte de démonstration et choisissez « PIN oublié ». Le code doit arriver dans la boîte du compte. Sans domaine, regardez aussi dans les **courriers indésirables**.

Sans `BREVO_API_KEY`, aucun e-mail ne part : la connexion mobile ne peut pas envoyer de code. Seuls les comptes ayant déjà un PIN pourront alors se connecter.

## 7. Brancher la CD de l'API

1. Dans Render, ouvrez **Settings → Deploy Hook** et copiez l'URL. Elle contient une clé : c'est un secret.
2. Dans GitHub, ouvrez `yay360-backend` → **Settings → Environments → production** :
   - **Environment secrets → Add secret** : `RENDER_DEPLOY_HOOK_URL` = l'URL copiée ;
   - **Environment variables → Add variable** : `API_URL` = `https://yay360-api.onrender.com`, sans `/` final.
3. Relancez le déploiement. Choisissez l'une des deux méthodes :
   - **Actions → CD backend → Run workflow**, avec dans **version** le SHA complet du dernier commit de `main` (visible dans l'historique des commits, bouton « copier ») ;
   - ou rouvrez l'exécution rejetée et cliquez sur **Re-run all jobs**.
4. Approuvez avec **Review deployments → production → Approve and deploy**.
5. Le job attend que `/actuator/info` affiche **ce SHA**, puis lance les tests après déploiement : santé `UP`, route protégée → 401, connexion invalide → 401.

## 8. Projet Vercel et CD du web

1. Créez un compte sur https://vercel.com avec GitHub.
2. Cliquez sur **Add New → Project → Import** `yay360-web`.
   - **Framework Preset** : **Other**. Les commandes viennent de `vercel.json`.
   - **Project Name** : `yay360`, ce qui donne l'URL `https://yay360.vercel.app` si le nom est libre.
   - Cliquez sur **Deploy**. Ce premier déploiement fait par Vercel lui-même est sans conséquence. Ensuite, `vercel.json` désactive les déploiements automatiques par Git.
3. Dans **Project Settings → General** : **Node.js Version** = **22.x**. Relevez aussi le **Project ID**.
4. Dans **Account Settings** (ou **Team Settings**) → **General**, relevez votre **Vercel ID** (ou **Team ID**).
5. Dans **Account Settings → Tokens → Create Token** :
   - nom `github-cd-yay360` ;
   - **Scope** : votre compte ;
   - **Expiration** : 90 jours, par exemple.
6. Dans `yay360-web`, ouvrez **Settings → Environments → production** :
   - **secrets** : `VERCEL_TOKEN`, `VERCEL_ORG_ID` (le Vercel ID ou Team ID), `VERCEL_PROJECT_ID` ;
   - **variable** : `WEB_URL` = `https://yay360.vercel.app`, ou l'URL réelle.
7. **Si l'URL Render n'est pas `https://yay360-api.onrender.com`**, corrigez la destination `/api/:path*` dans `vercel.json` par une pull request.
8. **Actions → CD web → Run workflow**, puis approuvez. Les tests après déploiement vérifient :
   - la page d'accueil et une route Angular (`/connexion`) répondent 200 ;
   - les en-têtes de sécurité sont présents ;
   - le relais `/api` répond, quitte à attendre le réveil de Render.

## 9. Finaliser

**CORS et liens des e-mails.** Dans Render, ouvrez **Environment** et remplacez :
- `YAY360_FRONTEND_URL` par l'URL réelle de Vercel ;
- `YAY360_CORS_ORIGINS` par l'URL réelle de Vercel.

Puis cliquez sur **Save, rebuild and deploy**.

Le web passe par le relais `/api` de Vercel, mais Vercel transmet l'en-tête `Origin` du navigateur : l'API doit donc l'autoriser.

**Mobile.** Dans `yay360-mobile`, ouvrez **Settings → Environments → production → variable** : `API_URL` = `https://yay360-api.onrender.com`. Puis lancez **Actions → CD mobile → Run workflow** et approuvez. L'APK apparaît dans l'onglet **Releases** du dépôt.

## 9 bis. Nom de domaine avec DuckDNS

DuckDNS fournit gratuitement un sous-domaine de la forme `<nom>.duckdns.org`. Il pointe vers le **portail web (Vercel)** ; l'API reste joignable par le relais `/api` du portail, et l'application mobile garde l'adresse Render.

```
https://<nom>.duckdns.org ──► Vercel (Angular) ── /api/* ──► Render (API)
```

**Limite de DuckDNS** : il ne gère que des enregistrements A (adresse IPv4), AAAA et TXT, **pas de CNAME**. On pointe donc le sous-domaine vers l'adresse IP publique que Vercel indique pour un domaine personnalisé.

1. Ouvrez https://www.duckdns.org et connectez-vous (GitHub ou Google).
2. Dans **sub domain**, saisissez le nom choisi (par exemple `yay360`), puis **add domain**.
3. Dans Vercel, ouvrez le projet **yay360 → Settings → Domains → Add**, saisissez `<nom>.duckdns.org`. Vercel affiche l'enregistrement attendu : **type A** et une **adresse IP**.
4. Revenez sur DuckDNS : dans le champ **current ip** de votre sous-domaine, saisissez **l'adresse IP donnée par Vercel**, puis **update ip**.
5. Attendez que Vercel affiche **Valid Configuration** (quelques minutes). Il émet alors seul le certificat HTTPS (Let's Encrypt).
6. Dans Render, **Environment** : ajoutez la nouvelle origine **sans retirer l'ancienne**, séparées par une virgule, puis **Save, rebuild and deploy** :
   - `YAY360_CORS_ORIGINS` = `https://<nom>.duckdns.org,https://yay360.vercel.app` ;
   - `YAY360_FRONTEND_URL` = `https://<nom>.duckdns.org` (liens envoyés par e-mail).
7. Dans `yay360-web`, **Settings → Environments → production → variable** `WEB_URL` = `https://<nom>.duckdns.org`.

**Sécurité** : le **jeton DuckDNS** affiché sur la page d'accueil du site permet de modifier l'adresse du domaine. C'est un secret : ne le copiez dans aucun fichier ni message. Il n'est utile que pour une mise à jour automatique de l'IP, inutile ici puisque l'adresse de Vercel est fixe.

**Pourquoi DuckDNS** : gratuit et sans carte bancaire, compatible avec l'absence de financement. En contrepartie, le service est maintenu par des bénévoles, sans garantie de disponibilité. Pour une mise en service réelle, un nom de domaine payant (par exemple en `.sn`) sera préférable.

## 10. Vérifier

| Vérification | Résultat attendu |
|---|---|
| `https://yay360-api.onrender.com/actuator/health` | `{"status":"UP"}` |
| `https://yay360-api.onrender.com/actuator/info` | `{"app":{"version":"<SHA du commit déployé>"}}` |
| `https://yay360.vercel.app` | page d'accueil, avec le cadenas HTTPS |
| `https://<nom>.duckdns.org` | même page d'accueil, avec le cadenas HTTPS |
| `https://<nom>.duckdns.org/app` | application mobile en version web (PWA) |
| Connexion à `awa.ndiaye.demo@yay360.sn` (mot de passe `YAY360_DEMO_PASSWORD` de production) | tableau de bord de la patiente |
| Swagger `https://yay360-api.onrender.com/swagger-ui.html` | **inaccessible**, ce qui est normal en production |

Les exécutions **CD**, la réponse de `/actuator/info` et le résultat des tests après déploiement servent de preuves du déploiement.

---

## Retour arrière

| Brique | Procédure |
|---|---|
| API | **Actions → CD backend → Run workflow** avec le SHA d'une version précédente : l'image existe déjà sur GHCR, rien n'est reconstruit. Approuvez ensuite le déploiement. |
| Web | Vercel → **Deployments** → déploiement précédent → **⋯ → Promote to Production** (instantané). |
| Mobile | réinstaller l'APK d'une Release précédente. |

**Base de données.** Flyway n'annule pas une migration. Revenir à une image plus ancienne n'est sûr que si le schéma reste compatible. C'est le cas des migrations V1 à V7 : elles ajoutent des tables, des colonnes ou des contraintes sans rien supprimer ni renommer.

Pour une future migration qui supprimerait ou renommerait une colonne, procédez en deux livraisons :
1. ajouter le nouvel élément ;
2. retirer l'ancien.

Avant une migration risquée, créez une **branche Neon** (copie instantanée de la base) : elle sert de point de restauration.

## Limites de l'offre gratuite

- **Mise en veille de Render.** L'API s'endort après 15 minutes sans requête, et le réveil prend environ une minute. **Ouvrez le site quelques minutes avant une démonstration.**
- **Ressources de Render.** 512 Mo de mémoire et 0,1 processeur. Render indique lui-même que l'offre gratuite n'est pas destinée à la production.
- **Suspension de Neon.** La base se suspend après quelques minutes d'inactivité : la première requête est plus lente.
- **Pas de préproduction pour l'API.** Les aperçus Vercel et les branches Neon permettraient d'en construire une.
- **APK signé avec la clé de debug.** Il n'est pas publiable sur le Play Store en l'état.
