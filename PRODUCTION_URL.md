# Configuration URL de production - MBOA AGRI

## URL fournie par Martial

Quand le backend est déployé sur Railway/Render, Martial fournit une URL HTTPS stable, par exemple :

```
https://mboa-agri-backend-production.up.railway.app/
ou
https://api-mboa-agri.onrender.com/
```

## Comment configurer

### Option 1 - Au build (recommandé pour APK équipe)

```bash
export BACKEND_BASE_URL="https://votre-url-prod.up.railway.app/"
./gradlew assembleDebug
# APK généré avec l'URL prod intégrée dans BuildConfig
```

Vérification :
- Dans `app/build.gradle.kts`, un `println` affiche l'URL active au build
- Dans l'app, écran de connexion affiche l'URL active en petit sous le logo
- Dans Paramètres -> À propos, affiche Backend: <url> et Build: <url>

### Option 2 - Runtime (sans rebuild, pour tests rapides)

1. Installer l'APK (même avec URL dev par défaut)
2. Ouvrir Paramètres -> Configuration Backend (Production)
3. Saisir l'URL prod (avec / final) : `https://votre-url-prod.up.railway.app/`
4. Sauvegarder -> ApiClient reconstruit automatiquement
5. Se déconnecter / se reconnecter pour obtenir un nouveau JWT sur la nouvelle URL
6. Tester : créer scan -> PENDING -> SYNCED

### Stockage

- BuildConfig : `BACKEND_BASE_URL` (valeur au build, depuis env)
- SharedPreferences `mboa_agri_backend` : `custom_backend_url` (surchage runtime)
- SharedPreferences `mboa_agri_auth` : `jwt_token`, `commune_code`, etc.

### Network Security

- `network_security_config.xml` :
  - `base-config cleartextTrafficPermitted=false` (HTTPS obligatoire en prod)
  - `domain-config cleartext=true` uniquement pour 10.0.2.2, localhost, 127.0.0.1 (dev émulateur)
  - Domaines prod `railway.app`, `onrender.com` en HTTPS (pas de cleartext)

### Checklist quand Martial fournit l'URL

- [ ] URL HTTPS avec / final
- [ ] Tester `/health` -> `{"status":"Ok"}`
- [ ] Tester POST /auth/register + POST /auth/login + GET /api/scans avec Bearer
- [ ] Exporter BACKEND_BASE_URL et builder APK debug
- [ ] Installer sur vrai téléphone, tester JWT + sync + alertes
- [ ] Envoyer APK + docs à l'équipe

### Exemple .env pour CI/CD

```bash
BACKEND_BASE_URL=https://mboa-agri-backend-production.up.railway.app/
DATABASE_URL=postgresql+psycopg://... (côté backend seulement, jamais dans APK)
JWT_SECRET_KEY=... (backend)
GEMINI_API_KEY=... (backend)
CLOUDINARY_... (backend)
```

### Contact

- Martial : backend FastAPI + PostgreSQL/PostGIS + déploiement
- Équipe Android : intégration JWT + sync + alertes (cette branche)

Date : 2026-09-22
