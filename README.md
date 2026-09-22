# MBOA AGRI

Mboa Agri est une application Android destinée à accompagner les agriculteurs camerounais dans le diagnostic des cultures, le suivi des scans, le tutorat agricole et la réception d’alertes territoriales.

> L’application fonctionne d’abord en local. Les scans sont conservés dans Room avec statut PENDING et synchronisés automatiquement avec le backend FastAPI dès qu’une connexion réseau est disponible, via JWT.

## Fonctionnalités principales

| Fonctionnalité | Description |
|---|---|
| Diagnostic agricole | Analyse d’une culture via backend /api/scans/diagnose (Gemini + catalogue expert + Cloudinary) et conservation du résultat dans l’historique local. |
| Historique hors connexion | Les résultats restent consultables sans réseau. |
| Synchronisation backend | Les scans PENDING/FAILED sont envoyés automatiquement avec WorkManager (envoi immédiat + filet de sécurité périodique toutes les 15 minutes) vers POST /api/scans avec Bearer JWT. |
| Authentification JWT | Inscription POST /auth/register (username, phone_number, password) → connexion POST /auth/login → token stocké dans TokenManager. |
| Alertes territoriales | GET /alerts?commune_code=CM-BFS-02 (backend) + détection locale 3 cas / 7 jours / 10km. |
| Assistant agronomique | Les requêtes d’IA passent par le backend, sans clé Gemini dans l’APK. |
| Profil utilisateur | Commune, code commune stable (CM-BFS-02), cultures, langue, consentement alertes, téléphone, saisis à l’inscription et modifiables dans Paramètres. |
| Code couleur strict | Vert = sain, Orange = attention, Rouge = urgent, appliqué partout. |

## Technologies

Kotlin, Jetpack Compose, Room 5, Retrofit/Moshi, WorkManager, CameraX, AndroidX. Dépendances dans [`gradle/libs.versions.toml`](gradle/libs.versions.toml), app dans [`app/build.gradle.kts`](app/build.gradle.kts).

## Configuration du backend

L’URL du backend est une configuration publique et ne doit pas être confondue avec une clé secrète. Elle est injectée au build via `BACKEND_BASE_URL` et peut être surchargée à l’exécution dans Paramètres.

```bash
export BACKEND_BASE_URL="https://adresse-du-backend-de-martial/"
./gradlew assembleDebug
```

- Développement émulateur : `http://10.0.2.2:8000/` (défaut)
- Production Railway/Render : `https://api-mboa-agri-production.up.railway.app/` (exemple, fourni par Martial)

Le slash final est obligatoire pour Retrofit. Le fichier [`.env.example`](.env.example) documente cette configuration.

### Configuration runtime (après installation)

L’app permet de configurer l’URL de production à l’exécution :

1. Ouvrir **Paramètres → Configuration Backend (Production)**
2. Saisir l’URL fournie par Martial, ex: `https://mboa-agri-backend.up.railway.app/`
3. Sauvegarder → ApiClient reconstruit
4. Se reconnecter pour obtenir un nouveau JWT sur la nouvelle URL

Cette fonctionnalité est implémentée dans [`BackendConfig.kt`](app/src/main/java/com/example/config/BackendConfig.kt) et affichée dans l’écran de connexion.

Comme le backend de développement tourne en HTTP (non chiffré), le fichier [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml) autorise le trafic en clair **uniquement** vers `10.0.2.2`, `localhost` et `127.0.0.1`. La production en HTTPS n’a pas besoin de cleartext (base-config cleartext=false).

## Contrat backend (aligné avec le rapport d’état)

| Méthode | Route | Rôle | Auth |
|---|---|---|---|
| `POST` | `/auth/register` | Inscription username + phone_number + password | Non |
| `POST` | `/auth/login` | Connexion phone_number + password → JWT | Non |
| `POST` | `/api/scans` | Recevoir un résultat de diagnostic à synchroniser (offline sync idempotent via user_id+local_id) | Bearer |
| `POST` | `/api/scans/diagnose` | Diagnostic image en ligne (multipart: image, plant_name, commune_code, lat/lng) → Gemini + catalogue + Cloudinary + diagnostics | Bearer |
| `GET` | `/api/scans` | Lister mes scans | Bearer |
| `GET` | `/alerts` | Alertes territoriales filtrées par commune_code, pathology_id, crop_name, dates | Non (ou Bearer selon config) |
| `GET` | `/api/pathologies` | Catalogue expert (code stable ex: MAIS_ROUILLE) | Bearer |

Détails des payloads dans [`ApiClient.kt`](app/src/main/java/com/example/api/ApiClient.kt) :
- `RegisterRequest`, `LoginRequest`, `TokenResponse`
- `ScanSyncPayload` : local_id, plant_name, disease_name, confidence, symptoms, treatment_local, treatment_chemical, timestamp, latitude, longitude, commune_code, hors_ligne
- `DiagnosticResponse` : pathology_code, image_url, severity_detected/default, information_source (CATALOG vs AI_PROVISIONAL)
- `AlertResponse` : pathology_id, commune_id, scan_count, alert_level

## Synchronisation hors connexion - Cycle complet testé sur vrai téléphone

Chaque nouveau scan est créé avec le statut `PENDING`. Le `ScanSyncWorker` est ensuite planifié avec une contrainte de réseau connecté.

**Cycle complet :**
```
Créer scan (Room, PENDING)
  ↓
enqueueScanSync (WorkManager, NetworkType.CONNECTED)
  ↓
Réseau disponible → Worker récupère token JWT depuis TokenManager
  ↓
POST /api/scans avec Authorization: Bearer <token> + commune_code
  ↓
Backend vérifie idempotence (user_id PostgreSQL + local_id Room)
  ↓
Succès → Room syncStatus = SYNCED, vérifiable dans PostgreSQL (table scans + diagnostics)
Échec → syncStatus = FAILED + retry exponentiel
```

Deux files sont planifiées :
- `enqueueScanSync` (à chaque nouveau scan) : envoi dès que le réseau revient ;
- `enqueuePeriodicScanSync` (au démarrage dans `MainActivity`) : retry PENDING/FAILED toutes les 15 min.

Les statuts sont définis dans [`Entities.kt`](app/src/main/java/com/example/data/Entities.kt), les requêtes Room dans [`Daos.kt`](app/src/main/java/com/example/data/Daos.kt) et le Worker dans [`ScanSyncWorker.kt`](app/src/main/java/com/example/sync/ScanSyncWorker.kt).

### Test terrain recommandé

1. Installer l’APK debug sur téléphone réel
2. Configurer l’URL de production dans Paramètres si besoin
3. S’inscrire avec téléphone (ex: 6XXXXXXXX) + username + password + commune + code commune (ex: CM-BFS-02)
4. Vérifier token stocké (affiché dans login et settings)
5. Créer un scan (photo feuille) → vérifier PENDING dans historique
6. Activer réseau → observer passage SYNCED (logcat: ScanSyncWorker)
7. Vérifier dans BDD backend : `SELECT * FROM scans WHERE user_id = X ORDER BY received_at DESC`
8. Tester alertes : `GET /alerts?commune_code=CM-BFS-02` et écran Alertes dans l’app

## Authentification JWT de bout en bout

```
Inscription → POST /auth/register {username, phone_number, password} → UserResponse {id, username, phone_number}
  ↓
Connexion → POST /auth/login {phone_number, password} → TokenResponse {access_token}
  ↓
Token stocké → TokenManager (SharedPreferences mboa_agri_auth)
  ↓
Requêtes authentifiées → Interceptor ajoute Authorization: Bearer <token> pour /api/scans et /api/scans/diagnose
  ↓
Vérifiable dans logcat OkHttp (BASIC) en debug
```

Implémentation :
- [`TokenManager.kt`](app/src/main/java/com/example/api/TokenManager.kt) : stockage JWT, user_id, commune_code
- [`ApiClient.kt`](app/src/main/java/com/example/api/ApiClient.kt) : AuthApiService + BackendApiService + authInterceptor + rebuild()
- [`MainViewModel.kt`](app/src/main/java/com/example/ui/MainViewModel.kt) : registerUser() et loginUser() avec backend + fallback local offline-first
- [`AuthScreen.kt`](app/src/main/java/com/example/ui/screens/AuthScreen.kt) : champs phone_number + commune_code + affichage URL backend + JWT présent

## Alertes de commune

Le backend expose `GET /alerts` avec filtres :
```
GET /alerts?commune=Bafoussam%20II
GET /alerts?commune_code=CM-BFS-02
GET /alerts?pathology_id=4
GET /alerts?crop_name=Maïs
GET /alerts?start_date=2026-09-01T00:00:00&end_date=2026-09-13T23:59:59
```

L’app :
- Récupère le code commune depuis le profil (`UserEntity.communeCode`) ou `TokenManager`
- Appelle `viewModel.fetchBackendAlerts(communeCode)` au lancement de l’écran Alertes
- Affiche un bandeau "Backend Alerts (commune)" avec comptage backend vs local
- Bouton refresh pour recharger `GET /alerts?commune_code=...`
- Fusion locale (3 cas / 7j / 10km) + backend dans `activeAlerts`

Voir [`AlertScreen.kt`](app/src/main/java/com/example/ui/screens/AlertScreen.kt) et [`MainViewModel.kt`](app/src/main/java/com/example/ui/MainViewModel.kt).

## Code couleur strict

Source de vérité dans [`DiagnosticSeverity.kt`](app/src/main/java/com/example/data/DiagnosticSeverity.kt) et [`Severity.kt`](app/src/main/java/com/example/ui/theme/Severity.kt) :

| Couleur | Niveau | Signification |
|---|---|---|
| 🟢 Vert | `SAIN` | Plante saine |
| 🟠 Orange | `ATTENTION` | Maladie à surveiller |
| 🔴 Rouge | `URGENT` | Maladie grave, intervention immédiate |

Affiché sur fiche diagnostic, historique dashboard, légende alertes. Alertes de zone toujours rouges.

## Base de données et migration

Room version 5 :
- Migration 3→4 : ajoute `syncStatus` à `scan_results` + `commune`, `cultures`, `langue`, `consentementAlertes` à `users`
- Migration 4→5 : ajoute `phoneNumber`, `communeCode`, `backendUserId`, `lastToken` à `users` pour JWT et code stable partagé avec backend

Définie dans [`AppDatabase.kt`](app/src/main/java/com/example/data/AppDatabase.kt).

## Tests

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Tests existants :
- `SyncStatusTest.kt` : cycle PENDING → SYNCED → FAILED
- `DiagnosticSeverityTest.kt` : classification FR/EN

Tests terrain recommandés (Postman + téléphone) :
```
POST /auth/register
POST /auth/login
GET /health
POST /api/pathologies
GET /api/pathologies
POST /api/scans/diagnose (multipart image)
POST /api/scans (JSON avec JWT)
GET /api/scans
GET /alerts?commune_code=CM-BFS-02
```

Le test du diagnostic online doit vérifier simultanément :
- réponse Gemini
- URL Cloudinary
- enregistrement PostgreSQL (diagnostics.image_url, commune_id, severity)
- source des traitements (catalogue expert prioritaire)
- validation_status

## Compiler l’APK

Le dépôt contient le Gradle Wrapper (Gradle 9.3.1, minimum pour AGP 9.1.1).

Prérequis :
1. JDK 17+
2. SDK Android plateforme 36 + Build Tools 36

**`local.properties` (créé automatiquement)** : ce fichier local (gitignoré) déclare le
chemin du SDK Android (`sdk.dir`). Il n'est pas dans le dépôt ni dans le ZIP GitHub :
au premier build, le garde-fou de [`settings.gradle.kts`](settings.gradle.kts) le crée
automatiquement en détectant le SDK (`ANDROID_HOME` / `ANDROID_SDK_ROOT`, puis
emplacements standards). Si votre SDK est ailleurs, éditez-le :
`sdk.dir=C:/chemin/vers/le/SDK` (template : [`local.properties.example`](local.properties.example),
chemin visible dans **Android Studio → Tools → SDK Manager**).

```bash
# Développement (émulateur)
./gradlew assembleDebug

# Production (URL fournie par Martial)
export BACKEND_BASE_URL="https://api-mboa-agri-production.up.railway.app/"
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk

# Release
./gradlew assembleRelease
```

Signature debug : `debug.keystore` optionnel, sinon `~/.android/debug.keystore` standard.

### Générer l’APK debug pour l’équipe terrain

```bash
export BACKEND_BASE_URL="https://votre-url-prod.up.railway.app/"
./gradlew assembleDebug
ls -lh app/build/outputs/apk/debug/
# Envoyer app-debug.apk à l'équipe + instructions test
```

## Structure utile

```
app/src/main/java/com/example/
├── api/       ApiClient (Auth + Backend + JWT interceptor), TokenManager, GeminiApiClient
├── config/    BackendConfig (production URL runtime + build)
├── data/      Entities (Room 5), Daos, AppDatabase, Converters, DiagnosticSeverity
├── sync/      ScanSyncWorker (PENDING→SYNCED avec JWT + commune_code)
└── ui/        MainViewModel (JWT auth + backend alerts + diagnose online), screens, theme
```

## Sécurité

- Aucune clé Gemini dans l’APK, seulement URL backend publique
- JWT stocké en SharedPreferences (à chiffrer avec EncryptedSharedPreferences en production)
- Secrets backend (DATABASE_URL, JWT_SECRET, GEMINI_API_KEY, CLOUDINARY) hors Git, en variables d’environnement Railway/Render
- network_security_config.xml : cleartext uniquement pour 10.0.2.2/localhost, HTTPS obligatoire en prod

## Références

- [`app/build.gradle.kts`](app/build.gradle.kts) — URL backend + versionCode 2 (1.1.0-jwt-sync)
- [`ApiClient.kt`](app/src/main/java/com/example/api/ApiClient.kt) — contrat Retrofit complet
- [`TokenManager.kt`](app/src/main/java/com/example/api/TokenManager.kt) — JWT storage
- [`BackendConfig.kt`](app/src/main/java/com/example/config/BackendConfig.kt) — config production runtime
- [`AppDatabase.kt`](app/src/main/java/com/example/data/AppDatabase.kt) — Room 5 + migrations
- [`ScanSyncWorker.kt`](app/src/main/java/com/example/sync/ScanSyncWorker.kt) — sync avec JWT
- [`MainViewModel.kt`](app/src/main/java/com/example/ui/MainViewModel.kt) — auth JWT + alerts backend
- [`AuthScreen.kt`](app/src/main/java/com/example/ui/screens/AuthScreen.kt) — test JWT bout en bout
- [`AlertScreen.kt`](app/src/main/java/com/example/ui/screens/AlertScreen.kt) — réception alertes commune
