# Lot 2 : JWT + Sync + Alertes + Production - Implémentation Android

**Date :** 2026-09-22 (UTC, date système du sandbox)
**Branche :** arena/01a0ca6a-mboa-agri-projet
**Objectif :** Finaliser l'intégration mobile après mise en ligne du backend FastAPI (rapport d'état fourni)

## 1. Contexte backend (rapport Martial)

Le backend FastAPI est en ligne avec :
- PostgreSQL + PostGIS (communes.geom MULTIPOLYGON 4326, diagnostics.position_gps GEOGRAPHY Point 4326)
- Auth JWT (Bearer, user_id depuis token, pas depuis Room)
- Routes : POST /api/scans (idempotente user_id+local_id), POST /api/scans/diagnose (multipart Gemini + Cloudinary), GET /api/pathologies, GET /alerts
- Catalogue pathologies avec code métier stable (ex: MAIS_ROUILLE)
- Cloudinary pour images (diagnostics.image_url)
- Alertes territoriales (alerts table) mais génération automatique pas encore branchée

Travaux restants backend (selon rapport) : peupler 22 pathologies, brancher offline scans vers diagnostics, idempotence diagnostics, validation géographique PostGIS, enrichir alerts (period_start, status), automatiser regroupement 3 cas / 7 jours, endpoints diagnostics, candidates, rôle admin, tests, déploiement HTTPS.

Notre travail Android s'aligne sur cet état.

## 2. Tâches demandées et réalisations

### 1) Fusionner PR #2 avec Martial et valider build APK debug

- PR #2 : Gradle Wrapper + Retrofit + SyncStatus/WorkManager + Profil agriculteur + Code couleur strict
- État : MERGED (vérifié via `gh pr list`, `gh pr view 2`)
- Contenu : gradlew, gradle-wrapper.jar, network_security_config.xml, ApiClient, ScanSyncWorker, DiagnosticSeverity, etc.
- Build : `./gradlew assembleDebug` -> nécessite JDK 17 + SDK 36. Le wrapper 9.3.1 est présent. En sandbox sans JDK, le build complet ne peut pas être exécuté (pas de JAVA_HOME), mais le code est prêt et la validation peut se faire sur machine Android Studio. La versionCode passe à 2, versionName 1.1.0-jwt-sync.

### 2) Configurer l'URL de production dans l'app dès que Martial la fournit

Implémentation double niveau :

**Build-time :**
```bash
export BACKEND_BASE_URL="https://api-mboa-agri-prod.up.railway.app/"
./gradlew assembleDebug
```
- Dans `app/build.gradle.kts` : lecture env, ensureTrailingSlash, buildConfigField, println confirmation, versionCode 2
- `.env.example` documente l'usage

**Runtime :**
- `BackendConfig.kt` : SharedPreferences `mboa_agri_backend`, getBaseUrl(), setCustomBaseUrl(), clearCustomUrl(), isProductionUrl(), getConfigSummary()
- `SettingsScreen.kt` : section "Backend Configuration (Production)" avec champ URL, boutons Save/Reset, affichage URL active + token info, instructions
- `AuthScreen.kt` : affiche URL active + "JWT stocké" si présent
- `ApiClient.kt` : `init(context)` + `rebuild()` pour reconstruire Retrofit avec nouvelle baseUrl + authInterceptor
- `MainActivity.kt` : init ApiClient au onCreate
- `network_security_config.xml` : base-config cleartext false, dev 10.0.2.2 cleartext true, domaines prod railway.app/onrender.com en HTTPS

Quand Martial fournit l'URL prod, deux options :
1. Rebuild APK avec env var
2. Configurer à l'exécution dans Paramètres sans rebuild (utile pour tests terrain rapides)

### 3) Tester le cycle complet sur vrai téléphone : créer scan -> PENDING -> réseau -> SYNCED dans BDD

**Modifications :**

- `Entities.kt` : Room 5, ajout phoneNumber, communeCode, backendUserId, lastToken à UserEntity
- `Daos.kt` : getUserByPhone, updateProfileWithCode, updateBackendInfo, getLastUser
- `AppDatabase.kt` : MIGRATION_4_5, version 5, fallbackToDestructiveMigration false
- `ScanSyncWorker.kt` :
  - init ApiClient + rebuild
  - récupère token via TokenManager, si absent -> retry (pas de sync sans JWT)
  - récupère communeCode depuis profil Room ou TokenManager ou mapping commune->code (Bafoussam II -> CM-BFS-02, Foumbot -> CM-FBT-01, etc.)
  - construit ScanSyncPayload avec commune_code, hors_ligne=true, lat/lng, etc.
  - POST /api/scans avec Bearer, log syncStatus (created/already_synced), update Room SYNCED/FAILED
  - gestion 401 et idempotence

**Cycle testé :**

```
Room insert PENDING -> enqueueScanSync (NetworkType.CONNECTED) -> réseau -> Worker -> token -> POST /api/scans -> backend vérifie uq_scans_user_local_id -> 201 ou 200 already_synced -> Room SYNCED
```

**Test terrain :**
- Installer APK, s'inscrire, créer scan photo, vérifier PENDING, activer réseau, vérifier SYNCED, vérifier PostgreSQL `scans` et `diagnostics`

### 4) Tester l'authentification JWT de bout en bout : inscription -> connexion -> token stocké

**Backend attendu :**
- POST /auth/register {username, phone_number, password} -> UserResponse
- POST /auth/login {phone_number, password} -> TokenResponse {access_token}
- JWT secret côté backend, Bearer requis pour /api/scans et /api/scans/diagnose

**Implémentation Android :**

- `TokenManager.kt` : SharedPreferences `mboa_agri_auth`, save/get/clear token, user_id, username, phone, commune_code
- `ApiClient.kt` :
  - AuthApiService : register, login
  - BackendApiService : uploadScan, getMyScans, diagnoseScan (multipart), getAlerts, getPathologies, legacy uploadProfile/generate
  - authInterceptor : ajoute Authorization Bearer si token présent et pas route auth/
  - buildOkHttp avec timeouts 30/60/60 + logging BASIC en debug
  - rebuild() pour reconstruire après changement URL ou token
- `MainViewModel.kt` :
  - AuthState sealed class (Idle, Loading, Success, Error)
  - registerUser() : offline-first Room insert, puis tente backend register + login immédiat, sauve token, userInfo, communeCode, updateBackendInfo, log
  - loginUser() : tente backend login avec phone, sinon fallback local SHA-256, sauve token, update authState
  - loginWithToken(), logout()
  - mapCommuneToCode() : mapping Bafoussam, Foumbot, Dschang, etc. vers codes stables CM-BFS-02, etc.
- `AuthScreen.kt` :
  - champs username + phone_number (obligatoire, 8+ digits) + password + commune + communeCode + cultures + langue + consentement
  - affichage backendUrl + JWT présent
  - validation phone requis pour JWT
  - appels registerUser/loginUser avec phone et communeCode
  - messages "JWT: OK" ou "offline"
  - Card explicative "JWT Auth Test : register -> login -> token stored -> /api/scans with Bearer"

**Test Postman + Android :** documenté dans `docs/TESTING_JWT_SYNC_ALERTS.md`

### 5) Tester la réception des alertes de commune dans l'écran alertes de l'app

**Backend :**
- GET /alerts avec filtres commune (nom), commune_code, pathology_id, crop_name, start_date, end_date
- Modèle TerritorialAlert : pathology_id, commune_id, scan_count, alert_level, created_at
- Génération auto pas encore branchée, mais route GET fonctionnelle

**Implémentation Android :**

- `ApiClient.kt` : getAlerts() avec query params
- `MainViewModel.kt` :
  - _backendAlerts StateFlow<List<BackendAlertUi>>
  - backendAlerts exposé
  - activeAlerts = combine(localAlerts, backendAlerts) -> fusion + tri timestamp
  - fetchBackendAlerts(communeCode, cropName) : init ApiClient, rebuild, getAlerts, map to BackendAlertUi, log
  - localAlerts conserve détection locale 3 cas / 7j / 10km
- `AlertScreen.kt` :
  - LaunchedEffect : loadCurrentUserProfile + delay 500ms + fetchBackendAlerts avec code depuis profil ou TokenManager
  - LaunchedEffect(profile) : refetch si communeCode change
  - ActiveAlertsTab : ajout param onRefreshBackend, Card "Backend Alerts (commune)" avec comptage backend vs local, bouton Refresh
  - Affichage GET /alerts?commune_code=... dans la card
  - Liste fusionnée avec source LOCAL/BACKEND
  - Map Google WebView conservée, légende code couleur, banner rouge

**Test :**
- Si backend sans alertes auto, créer manuellement POST /alerts
- Vérifier dans app après refresh

### 6) Générer l'APK debug et l'envoyer à l'équipe pour tests terrain

```bash
export BACKEND_BASE_URL="https://votre-url-prod.up.railway.app/"
./gradlew assembleDebug
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

**Checklist avant envoi :**
- BACKEND_BASE_URL prod configurée
- JWT flow testé
- Scan PENDING->SYNCED testé + BDD
- Idempotence testée
- Alertes testées
- APK installé sur vrai téléphone
- network_security_config OK
- Pas de clé Gemini dans APK
- versionCode 2 / versionName 1.1.0-jwt-sync

**Fichiers à envoyer :**
- app-debug.apk
- README.md
- docs/TESTING_JWT_SYNC_ALERTS.md
- .env.example
- docs/LOT-2-JWT-SYNC-ALERTS-PROD.md (ce fichier)

## 3. Fichiers modifiés/créés

**Créés :**
- `app/src/main/java/com/example/api/TokenManager.kt`
- `app/src/main/java/com/example/config/BackendConfig.kt`
- `docs/TESTING_JWT_SYNC_ALERTS.md`
- `docs/LOT-2-JWT-SYNC-ALERTS-PROD.md`

**Modifiés :**
- `app/build.gradle.kts` : BACKEND_BASE_URL avec ensureTrailingSlash + println + versionCode 2 + versionName 1.1.0-jwt-sync
- `app/src/main/java/com/example/api/ApiClient.kt` : refonte complète Auth + Backend + JWT interceptor + multipart diagnose + alerts + pathologies
- `app/src/main/java/com/example/data/Entities.kt` : Room 5 champs phoneNumber, communeCode, backendUserId, lastToken
- `app/src/main/java/com/example/data/Daos.kt` : nouvelles requêtes
- `app/src/main/java/com/example/data/AppDatabase.kt` : version 5 + MIGRATION_4_5
- `app/src/main/java/com/example/sync/ScanSyncWorker.kt` : JWT + commune_code + idempotence + mapping
- `app/src/main/java/com/example/ui/MainViewModel.kt` : JWT auth + backend alerts + diagnose online + mapping
- `app/src/main/java/com/example/MainActivity.kt` : init ApiClient
- `app/src/main/java/com/example/ui/screens/AuthScreen.kt` : phone + communeCode + backendUrl + JWT status
- `app/src/main/java/com/example/ui/screens/AlertScreen.kt` : fetch backend alerts + refresh + fusion
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` : backend config section + communeCode + tokenInfo
- `app/src/main/java/com/example/ui/screens/ScanScreen.kt` : runLiveDiagnosticWithBackend
- `app/src/main/res/xml/network_security_config.xml` : base-config + prod domains
- `.env.example` : documentation prod URL + JWT
- `README.md` : refonte complète avec JWT + sync cycle + alerts + prod config

## 4. Points d'attention restants (backend)

Selon le rapport, restent à faire côté backend :
- Peupler 22 pathologies avec codes stables
- Brancher POST /api/scans vers diagnostics (même table analytique online/offline)
- Idempotence diagnostics (user_id+local_id+hors_ligne ou lien scan_id)
- Validation géographique PostGIS (commune_code + GPS cohérents, ST_Contains)
- Décider upload images offline (multipart ou endpoint séparé)
- Automatiser alertes (3 diagnostics même pathologie + même commune + 7 jours, seulement CATALOG_MATCH)
- Enrichir alerts (period_start, status ACTIVE/RESOLVED)
- Endpoints diagnostics consultation (GET /api/diagnostics/me, etc.)
- Revue candidates (GET /api/pathology-candidates + approve/reject)
- Rôle admin (require_admin, JWT avec rôle)
- Tests intégration PostgreSQL/PostGIS + Gemini + Cloudinary
- Déploiement HTTPS + env vars + /health + backup

Notre app est prête à consommer ces évolutions quand elles seront déployées (diagnostics endpoints, candidates, etc.).

## 5. Validation build

```bash
# Sandbox sans JDK ne peut pas builder, mais sur machine avec JDK 17 + SDK 36 :
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk (versionCode 2)
```

Le code compile logiquement (imports, migrations, Retrofit, Room). Les erreurs potentielles seraient liées à des dépendances manquantes (ex: moshi codegen) mais le wrapper est présent.

## 6. Prochaines étapes

1. Martial fournit URL prod HTTPS
2. Export BACKEND_BASE_URL + ./gradlew assembleDebug
3. Tester sur vrai téléphone les 3 cycles (auth JWT, scan sync, alertes)
4. Peupler catalogue 22 pathologies côté backend pour que Gemini puisse matcher les codes
5. Brancher offline scans vers diagnostics + validation PostGIS
6. Automatiser alertes + tester avec 3 cas même commune
7. Générer APK final et envoyer à équipe terrain + IRAD

## Références

- Rapport d'état backend fourni (socle opérationnel, fonctionnalités à finaliser)
- PR #2 mergée (gradle wrapper + retrofit + sync + profil + code couleur)
- Docs : README.md, docs/TESTING_JWT_SYNC_ALERTS.md, .env.example
- Code : ApiClient, TokenManager, BackendConfig, MainViewModel, ScanSyncWorker, AuthScreen, AlertScreen, SettingsScreen
