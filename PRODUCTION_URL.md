# Configuration URL de production - MBOA AGRI

## URL de production fournie par Martial (FastAPI Cloud)

```
https://backend-fastapi-d97b775d.fastapicloud.dev/
```

- **Health** : `/health` → `{"status":"Ok","service":"MBOA AGRI API"}` ✅ vérifié le 2026-09-22
- **Docs Swagger** : `/docs` → toutes les routes
- **OpenAPI** : `/openapi.json` → schemas `RegisterRequest`, `LoginRequest`, `ScanCreateRequest`, `DiagnosticResponse`, `AlertResponse`, etc.

## Configuration appliquée (build 1.2.0-prod)

### Option 1 - Au build (défaut : l'APK pointe déjà sur la prod)

```bash
export BACKEND_BASE_URL="https://backend-fastapi-d97b775d.fastapicloud.dev/"
./gradlew assembleDebug
# APK généré avec l'URL prod intégrée dans BuildConfig
# -> app/build/outputs/apk/debug/app-debug.apk
```

Depuis cette version, `BACKEND_BASE_URL` n'est plus obligatoire : la valeur par défaut
de `app/build.gradle.kts` est déjà l'URL de production. L'override env ne sert qu'au dev local :

```bash
export BACKEND_BASE_URL="http://10.0.2.2:8000/"   # dev émulateur uniquement
```

Vérification :
- Dans `app/build.gradle.kts`, un `println` affiche l'URL active au build
- Dans l'app, écran de connexion affiche l'URL active en petit sous le logo + "JWT stocké" si token présent
- Dans Paramètres -> À propos, affiche Backend: <url> et Build: <url>

### Option 2 - Runtime (sans rebuild, pour tests rapides)

1. Installer l'APK
2. Ouvrir Paramètres -> Configuration Backend (Production)
3. Saisir l'URL prod (avec / final) : `https://backend-fastapi-d97b775d.fastapicloud.dev/`
4. Sauvegarder -> ApiClient reconstruit automatiquement
5. Se déconnecter / se reconnecter pour obtenir un nouveau JWT sur la nouvelle URL
6. Tester : créer scan -> PENDING -> SYNCED

### Stockage

- BuildConfig : `BACKEND_BASE_URL` (valeur au build, depuis env ou défaut prod)
- SharedPreferences `mboa_agri_backend` : `custom_backend_url` (surchage runtime)
- SharedPreferences `mboa_agri_auth` : `jwt_token`, `commune_code`, etc.

### Network Security

- `network_security_config.xml` :
  - `base-config cleartextTrafficPermitted=false` (HTTPS obligatoire en prod)
  - `domain-config cleartext=true` uniquement pour 10.0.2.2, localhost, 127.0.0.1 (dev émulateur)
  - `fastapicloud.dev` + `backend-fastapi-d97b775d.fastapicloud.dev` en HTTPS (cleartext=false)

## Routes backend (OpenAPI confirmé)

| Méthode | Route | Auth | Usage |
|---------|-------|------|-------|
| POST | `/auth/register` | – | Inscription (`RegisterRequest`) |
| POST | `/auth/login` | – | Connexion → JWT (`TokenResponse`) |
| POST | `/api/scans` | Bearer | Sync offline scan (`ScanCreateRequest`, idempotent `user_id+local_id`) |
| GET | `/api/scans` | Bearer | Historique serveur |
| GET | `/api/scans/{scan_id}` | Bearer | Détail scan |
| POST | `/api/scans/diagnose` | Bearer | Diagnostic IA (multipart image → `DiagnosticResponse`) |
| GET | `/alerts` | – | Alertes (`commune_code`, `crop_name`, dates...) |
| POST | `/alerts` | – | Créer alerte (`AlertCreateRequest`) |
| GET | `/api/pathologies` | Bearer | Catalogue pathologies |
| GET | `/health` | – | Santé API |

## Checklist terrain

- [x] URL HTTPS avec / final
- [x] Tester `/health` -> `{"status":"Ok"}`
- [x] Configurer `BACKEND_BASE_URL` par défaut + `network_security_config.xml` + `.env.example`
- [ ] Tester POST /auth/register + POST /auth/login + GET /api/scans avec Bearer (hors sandbox : Postman ou téléphone)
- [ ] Builder APK debug sur machine avec JDK 17 (sandbox sans JDK)
- [ ] Installer sur vrai téléphone, tester JWT + sync + alertes (voir `docs/TESTING_JWT_SYNC_ALERTS.md`)
- [ ] Envoyer APK + docs à l'équipe

### Exemple .env pour CI/CD

```bash
BACKEND_BASE_URL=https://backend-fastapi-d97b775d.fastapicloud.dev/
DATABASE_URL=postgresql+psycopg://... (côté backend seulement, jamais dans APK)
JWT_SECRET_KEY=... (backend)
GEMINI_API_KEY=... (backend)
CLOUDINARY_... (backend)
```

### Contact

- Martial : backend FastAPI + PostgreSQL/PostGIS + déploiement (FastAPI Cloud)
- Équipe Android : intégration JWT + sync + alertes (cette branche)

Date : 2026-09-22
