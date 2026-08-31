# Analyse du planning Mboa Agri et étapes restantes vers l’APK

**Projet :** MBOA AGRI  
**Planning analysé :** Semaine 8, du 31 août au 5 septembre 2026  
**Dépôt analysé :** [gaussalexandrenguedia-commits/mboa-agri-projet](https://github.com/gaussalexandrenguedia-commits/mboa-agri-projet)  
**Dernier commit observé :** [`19d4a82`](https://github.com/gaussalexandrenguedia-commits/mboa-agri-projet/commit/19d4a82)

## 1. Objectif du planning

Le planning indique que l’application Android est un prototype local fonctionnel, mais qu’elle doit être connectée à un backend FastAPI et à une base PostgreSQL avant le terrain avec l’IRAD. L’objectif de la semaine est donc de passer d’un prototype principalement local à une application capable d’échanger avec un serveur, de synchroniser les scans hors connexion et de produire un APK validé.

> Le planning identifie le backend FastAPI comme absent et la connexion App Android ↔ API comme absente. Ces deux éléments sont les principaux facteurs de risque pour la livraison terrain.

## 2. État actuel vérifié dans GitHub

| Élément | État observé dans le dépôt | Conséquence |
|---|---|---|
| Application Android Kotlin/Jetpack Compose | Présente | La base fonctionnelle existe. |
| Retrofit et `ApiClient.kt` | Présents | La communication est préparée, mais elle doit être raccordée au vrai backend. |
| Room et statuts `PENDING`, `SYNCED`, `FAILED` | Présents | Le stockage hors connexion est préparé. |
| WorkManager | Présent | La synchronisation automatique est préparée. |
| Profil utilisateur | Champs ajoutés | Commune, cultures, langue et consentement sont disponibles dans Room. |
| Authentification | Encore principalement locale | Elle doit être reliée aux endpoints JWT du backend. |
| Backend FastAPI | Absent du dépôt | Blocage principal pour les fonctions connectées. |
| Scripts PostgreSQL | Non présents dans le dépôt Android | Les tables et données de référence doivent être vérifiées séparément. |
| Gradle Wrapper `gradlew` | Absent | Le build reproductible depuis un clone propre n’est pas garanti. |
| APK compilé | Non présent | Il faut exécuter un build Android réel. |
| Tests d’intégration de bout en bout | Absents | Le cycle scan → API → PostgreSQL → alerte reste à valider. |
| Maquettes Figma et guide visuel | Absents du dépôt | Non bloquant pour compiler, mais prévu dans le planning. |

## 3. Travail Android déjà réalisé

Le Lot 1 Android a été préparé et publié dans GitHub.

### Sécurité de la clé Gemini

La référence directe à `BuildConfig.GEMINI_API_KEY` a été retirée. L’application ne doit plus embarquer de clé Gemini dans l’APK. Les requêtes d’intelligence artificielle sont préparées pour passer par le backend avec `POST /api/ai/generate`.

### Client Retrofit

Le fichier [`ApiClient.kt`](app/src/main/java/com/example/api/ApiClient.kt) définit les opérations de communication avec le backend :

| Méthode | Route actuelle dans Android | Rôle |
|---|---|---|
| `uploadScan` | `POST /api/scans` | Envoyer un résultat de diagnostic. |
| `uploadProfile` | `POST /api/profiles` | Envoyer le profil territorial de l’agriculteur. |
| `generate` | `POST /api/ai/generate` | Demander une réponse à l’assistant agronomique côté serveur. |

L’URL racine est configurée avec `BACKEND_BASE_URL`. La valeur de développement par défaut est `http://10.0.2.2:8000/`, adaptée à un backend local lancé sur la machine hôte depuis un émulateur Android.

### Synchronisation hors connexion

Les scans sont enregistrés dans Room avec le statut `PENDING`. WorkManager attend une connexion réseau, envoie les scans au backend, puis les marque `SYNCED` en cas de succès ou `FAILED` en cas d’erreur. Les échecs peuvent être rejoués automatiquement avec un délai progressif.

### Base Room et profil

La base Room est passée à la version 4. La migration ajoute `syncStatus` à `scan_results` et les champs `commune`, `cultures`, `langue` et `consentementAlertes` à `users`.

## 4. Étapes restantes obligatoires

### Étape 1 — Créer et publier le backend FastAPI

Martial doit ajouter au dépôt un backend comprenant au minimum :

| Méthode | Route attendue par le planning | Fonction |
|---|---|---|
| `POST` | `/auth/register` | Inscription avec téléphone et mot de passe. |
| `POST` | `/auth/login` | Connexion et émission d’un token JWT. |
| `POST` | `/scans` | Réception et enregistrement des scans. |
| `GET` | `/alerts?commune=X` | Retour des alertes d’une commune. |

Le backend doit être connecté à PostgreSQL avec SQLAlchemy et accompagné d’un README d’installation. Chaque endpoint doit être testé avec `curl` ou Postman.

### Étape 2 — Finaliser la base PostgreSQL

La base `mboa_agri` doit contenir les tables suivantes :

| Table | Contenu attendu |
|---|---|
| `pathologies` | Culture, symptômes, traitement et gravité; au moins cinq fiches initiales, idéalement les 22 prévues. |
| `communes` | Code, nom et département, notamment Foumbot et Dschang. |
| `farmer_profiles` | `user_id`, commune, cultures, langue et consentement aux alertes. |

Les données de connexion PostgreSQL doivent être transmises à Martial par un canal sécurisé. Aucun mot de passe ne doit être ajouté au dépôt GitHub ou à l’APK.

### Étape 3 — Aligner le contrat Android–FastAPI

Un écart doit être résolu avant les tests : le planning mentionne `/scans` et `/alerts`, tandis que le client Android préparé utilise `/api/scans`, `/api/profiles` et `/api/ai/generate`.

L’équipe doit choisir une convention unique. Deux options sont possibles :

| Option | Avantage | Action nécessaire |
|---|---|---|
| Conserver le préfixe `/api` | Convention claire pour une API versionnée ou séparée du frontend. | Martial crée `/api/auth/...`, `/api/scans`, `/api/alerts`; Android conserve ses chemins. |
| Suivre exactement le planning | Moins de modification côté backend documenté. | Android remplace ses chemins par `/auth/...`, `/scans` et `/alerts`. |

Il faut aussi valider les noms JSON, notamment `local_id`, `plant_name`, `disease_name`, `treatment_local`, `treatment_chemical`, `commune`, `cultures` et `consentement_alertes`.

### Étape 4 — Connecter l’authentification Android au JWT

L’écran d’authentification existe, mais les fonctions `registerUser` et `loginUser` utilisent encore Room localement. Il reste à :

1. Ajouter les endpoints d’inscription et de connexion dans Retrofit.
2. Recevoir le token JWT du backend.
3. Stocker le token de manière sécurisée côté Android.
4. Ajouter un interceptor OkHttp pour envoyer `Authorization: Bearer <token>`.
5. Associer chaque scan et chaque profil à l’utilisateur connecté.

Cette étape est nécessaire pour une utilisation multi-utilisateur fiable.

### Étape 5 — Réaliser le test réel hors connexion

Le test unitaire des statuts a été ajouté, mais le test demandé dans le planning doit être exécuté avec le vrai backend et la vraie base :

| Étape | Action | Résultat attendu |
|---:|---|---|
| 1 | Couper le réseau de l’émulateur. | Aucun envoi backend. |
| 2 | Créer un diagnostic. | Le scan est sauvegardé dans Room avec `PENDING`. |
| 3 | Rétablir le réseau. | WorkManager reprend le travail. |
| 4 | Observer FastAPI. | Le serveur reçoit le scan. |
| 5 | Observer PostgreSQL. | Le scan est enregistré en base. |
| 6 | Observer Room. | Le statut passe à `SYNCED`. |
| 7 | Arrêter temporairement le backend. | Le statut passe à `FAILED`. |
| 8 | Relancer le backend. | Le Worker retente et passe le scan à `SYNCED`. |

Le rapport de test devra inclure les résultats, les logs et des captures d’écran.

### Étape 6 — Relier les alertes territoriales

L’écran d’alerte doit consommer `GET /alerts?commune=X`. Il devra filtrer les informations selon la commune, les cultures et le consentement de l’utilisateur, puis afficher les niveaux vert, orange et rouge.

Cette étape est importante pour la démonstration terrain, mais elle dépend de l’endpoint backend et des tables `communes`, `pathologies` et `farmer_profiles`.

### Étape 7 — Ajouter le Gradle Wrapper et configurer le build

Le dépôt ne contient pas `gradlew`. Il faut ajouter le Gradle Wrapper et utiliser une machine équipée de Java, du SDK Android et des plateformes correspondant au `compileSdk` du projet.

Après configuration, les commandes attendues sont :

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

L’APK debug devrait être généré ici :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Pour le terrain, il faudra générer une version release signée :

```bash
./gradlew assembleRelease
```

La clé de signature doit rester hors de GitHub et être fournie avec des variables sécurisées telles que `KEYSTORE_PATH`, `STORE_PASSWORD` et `KEY_PASSWORD`.

## 5. Ce qui bloque réellement l’APK

| Élément | Bloque la compilation d’un APK ? | Bloque un APK connecté et prêt pour le terrain ? |
|---|---:|---:|
| Backend FastAPI absent | Non | Oui |
| PostgreSQL non raccordée | Non | Oui |
| Contrat des routes non aligné | Non | Oui |
| Authentification JWT non connectée | Non | Oui pour plusieurs utilisateurs |
| Test réel PENDING → SYNCED non réalisé | Non | Oui pour garantir la fiabilité |
| Gradle Wrapper absent | Oui pour un build reproductible depuis GitHub | Oui pour industrialiser la livraison |
| SDK Android/Java non configurés | Oui dans l’environnement de build | Oui |
| Maquettes Figma absentes | Non | Non, mais livrable du planning |
| Rapport TFLite absent | Non | Non pour compiler, mais livrable du planning |

## 6. Ordre recommandé de réalisation

L’ordre le plus sûr est le suivant :

1. Martial publie le backend FastAPI et ses endpoints testés.
2. Naimi finalise PostgreSQL, les tables et les données de référence.
3. L’équipe choisit une convention unique pour les routes API et valide les payloads JSON.
4. Android connecte l’authentification JWT et ajoute le token aux requêtes.
5. Android relie l’écran d’alertes à l’endpoint territorial.
6. Joyce exécute le test complet scan → Room → WorkManager → FastAPI → PostgreSQL → `SYNCED`.
7. L’équipe ajoute le Gradle Wrapper et configure une machine de build Android.
8. Les tests unitaires, d’intégration et de régression sont exécutés.
9. Un APK debug est généré pour validation interne.
10. Un APK release signé est généré pour le terrain.
11. Le rapport de test, le README backend, les maquettes et les supports agriculteurs sont déposés sur GitHub.

## 7. Conclusion

Le dépôt contient maintenant une préparation Android solide pour le Lot 1 : clé Gemini retirée, client Retrofit, modèle Room synchronisable, WorkManager et profil enrichi. Cependant, **l’APK connecté et prêt pour le terrain n’est pas encore finalisable** tant que le backend FastAPI, PostgreSQL, l’authentification JWT, l’alignement du contrat API et le test de bout en bout ne sont pas terminés.

L’APK debug est proche sur le plan du code Android, mais sa production doit encore être réalisée dans un environnement équipé du SDK Android et de Gradle. L’APK release nécessitera en plus une signature sécurisée.

## Références

- [Planning MBOA AGRI — Semaine 8](file:///home/ubuntu/upload/Planning_MBOA_AGRI_S8_Sept.pdf)
- [Dépôt GitHub Mboa Agri](https://github.com/gaussalexandrenguedia-commits/mboa-agri-projet)
- [README du projet](https://github.com/gaussalexandrenguedia-commits/mboa-agri-projet/blob/main/README.md)
- [Documentation Lot 1 Android](https://github.com/gaussalexandrenguedia-commits/mboa-agri-projet/blob/main/docs/LOT-1-BACKEND-ANDROID.md)
- [Client Retrofit Android](https://github.com/gaussalexandrenguedia-commits/mboa-agri-projet/blob/main/app/src/main/java/com/example/api/ApiClient.kt)
- [Worker de synchronisation](https://github.com/gaussalexandrenguedia-commits/mboa-agri-projet/blob/main/app/src/main/java/com/example/sync/ScanSyncWorker.kt)
