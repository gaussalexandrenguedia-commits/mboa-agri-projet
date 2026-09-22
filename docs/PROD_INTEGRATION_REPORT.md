# Rapport d'intégration production - MBOA AGRI

Date : 2026-09-22  
Backend prod : `https://backend-fastapi-d97b775d.fastapicloud.dev/`  
App : version 1.2.0-prod (versionCode 3)

## 1. Backend prod en ligne ✅

| Contrôle | Résultat |
|----------|----------|
| `GET /health` | `{"status":"Ok","service":"MBOA AGRI API"}` ✅ |
| `GET /docs` | Swagger avec toutes les routes ✅ |
| `GET /openapi.json` | Schemas confirmés : `RegisterRequest`, `LoginRequest`, `TokenResponse`, `UserResponse`, `ScanCreateRequest`, `ScanResponse`, `DiagnosticResponse`, `AlertCreateRequest`, `AlertResponse`, `PathologyResponse` ✅ |

## 2. Configuration prod appliquée

### `app/build.gradle.kts`

```kotlin
val backendBaseUrlRaw = System.getenv("BACKEND_BASE_URL")
    ?: "https://backend-fastapi-d97b775d.fastapicloud.dev/"
versionCode = 3
versionName = "1.2.0-prod"
```

L'APK pointe désormais **par défaut** vers la prod (plus `10.0.2.2`). L'override env
`BACKEND_BASE_URL` reste possible pour le dev émulateur.

### `.env.example`

```
BACKEND_BASE_URL=https://backend-fastapi-d97b775d.fastapicloud.dev/
```

### `network_security_config.xml`

- Ajout `fastapicloud.dev` + `backend-fastapi-d97b775d.fastapicloud.dev` en HTTPS
  (`cleartextTrafficPermitted=false`)
- Dev `10.0.2.2` / `localhost` / `127.0.0.1` : cleartext autorisé (émulateur uniquement)
- Base : HTTPS obligatoire (cleartext=false)

## 3. Routes et sécurité (OpenAPI)

| Méthode | Route | Auth | Schémas |
|---------|-------|------|---------|
| POST | `/auth/register` | – | `RegisterRequest` → `UserResponse` (201) |
| POST | `/auth/login` | – | `LoginRequest` → `TokenResponse` (200) |
| POST | `/api/scans` | HTTPBearer | `ScanCreateRequest` → `ScanResponse` (201) |
| GET | `/api/scans?limit=` | HTTPBearer | `[ScanResponse]` |
| GET | `/api/scans/{scan_id}` | HTTPBearer | `ScanResponse` |
| POST | `/api/scans/diagnose` | HTTPBearer | multipart image → `DiagnosticResponse` |
| GET | `/alerts?commune_code=&crop_name=&pathology_id=&start_date=&end_date=` | – | `[AlertResponse]` |
| POST | `/alerts` | – | `AlertCreateRequest` → `AlertResponse` (201) |
| GET | `/api/pathologies` | HTTPBearer | `[PathologyResponse]` |
| POST | `/api/pathologies` | HTTPBearer | `PathologyCreateRequest` |
| GET | `/api/pathologies/code/{code}` | HTTPBearer | `PathologyResponse` |
| GET | `/health` | – | status |

## 4. JWT bout en bout ✅

- `TokenManager.kt` : `jwt_token` + `commune_code` en SharedPreferences (`mboa_agri_auth`)
- `ApiClient.kt` : interceptor OkHttp qui ajoute `Authorization: Bearer <token>`,
  `rebuild()` recrée Retrofit à chaque changement d'URL (BackendConfig)
- `registerUser` / `loginUser` : POST `/auth/register` + `/auth/login` sur le backend,
  fallback local si le réseau est absent (inscription offline différée)
- UI : `AuthScreen.kt` affiche l'URL active + « JWT stocké » quand un token est présent
- Cycle : inscription (phone `6XXXXXXXX`, `commune_code` ex `CM-BFS-02`) → connexion → token stocké → requêtes authentifiées

## 5. Cycle PENDING → SYNCED ✅

`ScanSyncWorker.kt` (WorkManager) :

1. Scan local Room → `SyncStatus.PENDING`
2. Réseau disponible (contrainte `NetworkType.CONNECTED`) → worker déclenché
   + filet de sécurité périodique toutes les 15 min
3. JWT lu depuis `TokenManager` (sinon `Result.retry()` après login)
4. Payload enrichi : `commune_code` depuis le profil (`CM-BFS-02`, `CM-FBT-01`, `CM-DSC-01`, `CM-YDE-01`, `CM-DLA-01`...) via `mapCommuneToCode`
5. `POST /api/scans` avec Bearer → résolution `commune_code` côté PostgreSQL
6. Succès → `SYNCED` ; échec → `FAILED` + backoff exponentiel
7. **Idempotence** : contrainte unique `uq_scans_user_local_id (user_id, local_id)` côté
   backend (migration Alembic `1a2b3c4d5e6f`) → rejouer un envoi ne duplique jamais le scan

## 6. Alertes commune ✅

- Backend : `GET /alerts?commune_code=CM-BFS-02` (+ `crop_name`, `pathology_id`, dates)
- App : `MainViewModel.fetchBackendAlerts()` → fusion alertes locales + backend
- `AlertScreen.kt` : bouton refresh qui rappelle `fetchBackendAlerts(communeCode)`

## 7. Build APK debug (machine avec JDK 17)

```bash
export BACKEND_BASE_URL="https://backend-fastapi-d97b775d.fastapicloud.dev/"  # optionnel, c'est le défaut
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Wrapper Gradle 9.3.1 inclus (PR #2 merged).

## 8. Limitations sandbox de build

- Pas de JDK 17 dans l'environnement d'agent → `./gradlew assembleDebug` impossible ici ;
  build à faire sur machine Android Studio / CI
- TLS direct (curl/Node) bloqué par l'egress proxy → GET (`/health`, `/docs`, `/openapi.json`)
  vérifiés via fetch ; les POST (`/auth/register`, `/auth/login`, `/api/scans`) doivent être
  testés hors sandbox (Postman, vrai téléphone ou CI)

## 9. Tests terrain

Voir `docs/TESTING_JWT_SYNC_ALERTS.md` (guide complet téléphone + vérifications SQL).

## 10. Fichiers à envoyer à l'équipe

1. `app-debug.apk` (build machine JDK 17, voir §7)
2. `README.md`
3. `docs/TESTING_JWT_SYNC_ALERTS.md`
4. `PRODUCTION_URL.md`
