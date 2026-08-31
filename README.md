# MBOA AGRI

Mboa Agri est une application Android destinée à accompagner les agriculteurs camerounais dans le diagnostic des cultures, le suivi des scans, le tutorat agricole et la réception d’alertes territoriales.

> L’application fonctionne d’abord en local. Les scans sont conservés dans Room et synchronisés automatiquement avec le backend dès qu’une connexion réseau est disponible.

## Fonctionnalités principales

| Fonctionnalité | Description |
|---|---|
| Diagnostic agricole | Analyse d’une culture et conservation du résultat dans l’historique local. |
| Historique hors connexion | Les résultats restent consultables sans réseau. |
| Synchronisation backend | Les scans en attente sont envoyés automatiquement avec WorkManager. |
| Alertes territoriales | Les coordonnées et les informations de profil permettent de préparer des alertes par commune et culture. |
| Assistant agronomique | Les requêtes d’intelligence artificielle passent par le backend, sans clé Gemini dans l’APK. |
| Profil utilisateur | Le profil contient la commune, les cultures, la langue et le consentement aux alertes. |

## Technologies

Le projet utilise Kotlin, Jetpack Compose, Room, Retrofit/Moshi, WorkManager, CameraX et les bibliothèques AndroidX. La configuration des dépendances se trouve dans [`gradle/libs.versions.toml`](gradle/libs.versions.toml), tandis que l’application est définie dans [`app/build.gradle.kts`](app/build.gradle.kts).

## Configuration du backend

L’URL du backend est une configuration publique et ne doit pas être confondue avec une clé secrète. Elle est injectée au moment du build via la variable d’environnement `BACKEND_BASE_URL`.

```bash
export BACKEND_BASE_URL="https://adresse-du-backend-de-martial/"
```

Pour un backend lancé sur la machine hôte depuis l’émulateur Android, la valeur par défaut est :

```text
http://10.0.2.2:8000/
```

Le slash final est obligatoire pour Retrofit. Le fichier [`.env.example`](.env.example) documente cette configuration.

## Contrat backend attendu

L’application attend les routes suivantes :

| Méthode | Route | Rôle |
|---|---|---|
| `POST` | `/api/scans` | Recevoir un résultat de diagnostic à synchroniser. |
| `POST` | `/api/profiles` | Recevoir les informations territoriales et préférences du profil. |
| `POST` | `/api/ai/generate` | Exécuter côté serveur les requêtes adressées à l’assistant agronomique. |

Les détails des payloads se trouvent dans [`ApiClient.kt`](app/src/main/java/com/example/api/ApiClient.kt).

## Synchronisation hors connexion

Chaque nouveau scan est créé avec le statut `PENDING`. Le `ScanSyncWorker` est ensuite planifié avec une contrainte de réseau connecté. Lorsque le réseau revient, WorkManager exécute la synchronisation. Un envoi réussi marque le scan `SYNCED`; une erreur le marque `FAILED` et déclenche une nouvelle tentative avec un délai progressif.

Les statuts sont définis dans [`Entities.kt`](app/src/main/java/com/example/data/Entities.kt), les requêtes Room dans [`Daos.kt`](app/src/main/java/com/example/data/Daos.kt) et le Worker dans [`ScanSyncWorker.kt`](app/src/main/java/com/example/sync/ScanSyncWorker.kt).

## Base de données et migration

Le schéma Room est passé à la version 4. La migration ajoute `syncStatus` à `scan_results` ainsi que `commune`, `cultures`, `langue` et `consentementAlertes` à `users`. La migration est définie dans [`AppDatabase.kt`](app/src/main/java/com/example/data/AppDatabase.kt).

## Tests

Le test de cycle des statuts est disponible dans [`SyncStatusTest.kt`](app/src/test/java/com/example/SyncStatusTest.kt). Dans un environnement Android complet, les validations recommandées sont :

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Le dépôt actuel ne contient pas encore `gradlew`; il faut donc utiliser Android Studio ou ajouter le Gradle Wrapper avant d’exécuter ces commandes directement depuis un clone propre.

## Structure utile

```text
app/src/main/java/com/example/
├── api/       Client Retrofit et modèles de payloads
├── data/      Entités, DAO, convertisseurs et base Room
├── sync/      Synchronisation WorkManager
└── ui/        ViewModel et écrans Jetpack Compose
```

## Sécurité

Aucune clé Gemini ne doit être ajoutée au code Android, à `BuildConfig` ou à l’APK. Le serveur backend est responsable de conserver les secrets et d’appeler les services d’IA. Les fichiers `.env` locaux contenant des secrets doivent rester ignorés par Git.

## Références du projet

- [`app/build.gradle.kts`](app/build.gradle.kts) — configuration Android et URL backend.
- [`ApiClient.kt`](app/src/main/java/com/example/api/ApiClient.kt) — contrat Retrofit.
- [`AppDatabase.kt`](app/src/main/java/com/example/data/AppDatabase.kt) — schéma et migration Room.
- [`ScanSyncWorker.kt`](app/src/main/java/com/example/sync/ScanSyncWorker.kt) — synchronisation réseau.
