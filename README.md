# MBOA AGRI

Mboa Agri est une application Android destinée à accompagner les agriculteurs camerounais dans le diagnostic des cultures, le suivi des scans, le tutorat agricole et la réception d’alertes territoriales.

> L’application fonctionne d’abord en local. Les scans sont conservés dans Room et synchronisés automatiquement avec le backend dès qu’une connexion réseau est disponible.

## Fonctionnalités principales

| Fonctionnalité | Description |
|---|---|
| Diagnostic agricole | Analyse d’une culture et conservation du résultat dans l’historique local. |
| Historique hors connexion | Les résultats restent consultables sans réseau. |
| Synchronisation backend | Les scans en attente sont envoyés automatiquement avec WorkManager (envoi immédiat + filet de sécurité périodique toutes les 15 minutes). |
| Alertes territoriales | Les coordonnées et les informations de profil permettent de préparer des alertes par commune et culture. |
| Assistant agronomique | Les requêtes d’intelligence artificielle passent par le backend, sans clé Gemini dans l’APK. |
| Profil utilisateur | La commune, les cultures pratiquées, la langue et le consentement aux alertes sont saisis dès l’inscription, puis modifiables dans Paramètres. |
| Code couleur strict | Vert = sain, Orange = attention, Rouge = urgent, appliqué partout (diagnostics, historique, alertes). |

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

Comme le backend de développement tourne en HTTP (non chiffré), le fichier [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml) autorise le trafic en clair **uniquement** vers `10.0.2.2`, `localhost` et `127.0.0.1` (le HTTP reste interdit vers tout le reste d’Internet depuis Android 9 / API 28).

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

Deux files sont planifiées :

- `enqueueScanSync` (au moment de chaque nouveau scan) : envoi dès que le réseau revient ;
- `enqueuePeriodicScanSync` (au démarrage de l’application, dans `MainActivity`) : nouvelle tentative des scans `PENDING`/`FAILED` toutes les 15 minutes.

Les statuts sont définis dans [`Entities.kt`](app/src/main/java/com/example/data/Entities.kt), les requêtes Room dans [`Daos.kt`](app/src/main/java/com/example/data/Daos.kt) et le Worker dans [`ScanSyncWorker.kt`](app/src/main/java/com/example/sync/ScanSyncWorker.kt).

## Code couleur strict

Une seule source de vérité définit le niveau sanitaire de chaque diagnostic, dans [`DiagnosticSeverity.kt`](app/src/main/java/com/example/data/DiagnosticSeverity.kt) et [`Severity.kt`](app/src/main/java/com/example/ui/theme/Severity.kt) :

| Couleur | Niveau | Signification |
|---|---|---|
| 🟢 Vert | `SAIN` | Plante saine, aucune maladie détectée. |
| 🟠 Orange | `ATTENTION` | Maladie détectée, à surveiller et traiter. |
| 🔴 Rouge | `URGENT` | Maladie grave ou très contagieuse, intervention immédiate. |

Le niveau est déduit du nom de la maladie et des symptômes (analyse insensible aux accents, FR/EN). Il est affiché sur la fiche diagnostic (bandeau), dans l’historique du tableau de bord (pastille + texte coloré) et dans la légende des alertes. Les alertes de zone (3+ cas identiques en 7 jours dans un rayon de 10 km) sont toujours rouges.

## Base de données et migration

Le schéma Room est passé à la version 4. La migration ajoute `syncStatus` à `scan_results` ainsi que `commune`, `cultures`, `langue` et `consentementAlertes` à `users`. La migration est définie dans [`AppDatabase.kt`](app/src/main/java/com/example/data/AppDatabase.kt).

## Tests

Le test de cycle des statuts est disponible dans [`SyncStatusTest.kt`](app/src/test/java/com/example/SyncStatusTest.kt) et le test du code couleur strict dans [`DiagnosticSeverityTest.kt`](app/src/test/java/com/example/DiagnosticSeverityTest.kt). Dans un environnement Android complet, les validations recommandées sont :

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Compiler l’APK

Le dépôt contient le **Gradle Wrapper** ([`gradlew`](gradlew), [`gradlew.bat`](gradlew.bat), [`gradle/wrapper/`](gradle/wrapper/)) : aucune installation manuelle de Gradle n’est nécessaire. Le wrapper télécharge automatiquement Gradle **9.3.1**, la version minimale exigée par AGP 9.1.1.

Prérequis sur la machine de build :

1. **JDK 17 ou plus récent** (fourni par Android Studio, sinon `JAVA_HOME` doit pointer dessus) ;
2. **SDK Android** avec la plateforme 36 et les Build Tools 36 (installés automatiquement par Android Studio, ou via `sdkmanager`).

Puis, depuis la racine du dépôt :

```bash
./gradlew assembleDebug        # APK de débogage → app/build/outputs/apk/debug/
./gradlew assembleRelease      # APK de release (keystore requis)
```

Sous Windows, utiliser `gradlew.bat` au lieu de `./gradlew`.

### Signature

- **Debug** : le keystore `debug.keystore` à la racine du dépôt est **optionnel**. S’il est présent, le build l’utilise ; sinon, le build bascule automatiquement sur le keystore de débogage standard d’Android (`~/.android/debug.keystore`, généré par Android Studio). Un clone propre compile donc `assembleDebug` sans aucune configuration — `debug.keystore` étant ignoré par Git, ce repli évite l’échec du build.
- **Release** : créez un keystore de publication (à conserver hors du dépôt) avec :

```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Le build release lit ensuite le keystore via `KEYSTORE_PATH` (par défaut `my-upload-key.jks` à la racine) et les mots de passe via les variables d’environnement `STORE_PASSWORD` et `KEY_PASSWORD` — jamais en clair dans le code.

## Structure utile

```text
app/src/main/java/com/example/
├── api/       Client Retrofit et modèles de payloads
├── data/      Entités, DAO, convertisseurs, base Room et sévérité des diagnostics
├── sync/      Synchronisation WorkManager
└── ui/        ViewModel, écrans Jetpack Compose et thème (code couleur strict)
```

## Sécurité

Aucune clé Gemini ne doit être ajoutée au code Android, à `BuildConfig` ou à l’APK. Le serveur backend est responsable de conserver les secrets et d’appeler les services d’IA. Les fichiers `.env` locaux contenant des secrets doivent rester ignorés par Git.

## Références du projet

- [`app/build.gradle.kts`](app/build.gradle.kts) — configuration Android et URL backend.
- [`ApiClient.kt`](app/src/main/java/com/example/api/ApiClient.kt) — contrat Retrofit.
- [`AppDatabase.kt`](app/src/main/java/com/example/data/AppDatabase.kt) — schéma et migration Room.
- [`ScanSyncWorker.kt`](app/src/main/java/com/example/sync/ScanSyncWorker.kt) — synchronisation réseau.
