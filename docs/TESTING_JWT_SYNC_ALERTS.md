# Guide de tests terrain - MBOA AGRI (JWT + Sync + Alertes)

Ce guide couvre les 6 tâches demandées après la mise en ligne du backend.

## 1) Fusion PR #2 + build APK debug

PR #2 (Gradle Wrapper + Retrofit + SyncStatus + Profil agriculteur + Code couleur strict) est déjà mergée dans main (état: MERGED).

Validation build :

```bash
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

Prérequis : JDK 17+, SDK Android 36. Le wrapper Gradle 9.3.1 est inclus.

### Si le build échoue avec « SDK location not found » (local.properties manquant)

`local.properties` est un fichier **local** (gitignoré) : sur un clone à froid /
ZIP GitHub il peut être absent. Le garde-fou de `settings.gradle.kts` (PR #8)
le crée automatiquement au premier build s'il n'existe pas. Procédure
**manuelle** (SDK dans un emplacement non standard, ou base sans le garde-fou) :

```powershell
# Windows PowerShell (chemin = Android Studio -> Tools -> SDK Manager)
Set-Content -Path local.properties -Value "sdk.dir=C:/Users/PC/AppData/Local/Android/Sdk" -Encoding ASCII
```

```bash
# macOS / Linux
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # chemin à adapter
```

Barres obliques `/` dans le chemin = OK dans tous les cas.

En cas de problème Java dans sandbox sans JDK, le build est validable sur machine Android Studio. Le code compile (migrations Room 4->5, ApiClient avec JWT, etc.).

## 2) Configurer l'URL de production

### Option A - Au build (recommandé pour APK équipe)

```bash
export BACKEND_BASE_URL="https://backend-fastapi-d97b775d.fastapicloud.dev/"
./gradlew assembleDebug
```

Cette URL est aussi la valeur **par défaut** de `app/build.gradle.kts` : sans variable
d'environnement, l'APK pointe déjà sur la prod. Le `println` dans le gradle confirme l'URL active au build.

### Option B - Runtime dans l'app (sans rebuild)

1. Installer l'APK
2. Ouvrir Paramètres -> Configuration Backend (Production)
3. Saisir l'URL de Martial : `https://backend-fastapi-d97b775d.fastapicloud.dev/`
4. Sauvegarder -> ApiClient reconstruit
5. Se reconnecter pour obtenir un JWT sur la nouvelle URL

Implémentation :
- `BackendConfig.kt` : stockage SharedPreferences `mboa_agri_backend`, gestion slash final, détection prod (https + pas 10.0.2.2)
- `TokenManager.kt` : token + commune_code
- `ApiClient.init(context).rebuild()` : reconstruction Retrofit avec nouvelle baseUrl + interceptor Bearer
- `AuthScreen.kt` affiche l'URL active et "JWT stocké" si présent
- `network_security_config.xml` : cleartext false en base, cleartext true uniquement pour 10.0.2.2/localhost, domaines prod en HTTPS

## 3) Tester le cycle complet sur vrai téléphone : PENDING -> SYNCED -> BDD

### Étapes téléphone

1. Installer APK debug
2. Configurer URL prod si nécessaire
3. S'inscrire (username, phone 6XXXXXXXX, password 8+ chars, commune, code commune ex: CM-BFS-02, cultures)
4. Aller dans Dashboard -> Scanner une feuille -> Prendre photo -> Analyser
5. Vérifier dans Dashboard historique : pastille PENDING (jaune/orange)
6. Activer WiFi/data -> WorkManager déclenche ScanSyncWorker (logcat : `adb logcat | grep ScanSyncWorker`)
7. Après sync : pastille SYNCED (vert) ou FAILED (rouge) si erreur
8. Vérifier BDD backend :

```sql
-- Table scans (historique mobile)
SELECT id, local_id, user_id, plant_name, disease_name, commune_id, hors_ligne, received_at
FROM scans WHERE user_id = <id> ORDER BY received_at DESC LIMIT 5;

-- Table diagnostics (analytique, si offline branché ou online via /diagnose)
SELECT id, local_id, plant_name, disease_name, pathology_code, commune_id, image_url, severity_detected, information_source
FROM diagnostics WHERE user_id = <id> ORDER BY created_at DESC LIMIT 5;
```

### Logs Android

```bash
adb logcat | grep -E "ScanSyncWorker|MainViewModel|ApiClient"
# Attendu :
# ScanSyncWorker: Scan 12 sync status: created -> Scan synchronisé avec succès.
# ou already_synced si idempotence
```

### Idempotence

Renvoyer deux fois le même `local_id` avec même `user_id` JWT doit retourner 200 + même scan, pas de doublon (contrainte unique `uq_scans_user_local_id`).

Test Postman :

```json
POST /api/scans
Authorization: Bearer <token>
{
  "local_id": 42,
  "plant_name": "Maïs",
  "disease_name": "Rouille",
  "confidence": 90,
  "symptoms": "Taches orange",
  "treatment_local": "Neem",
  "treatment_chemical": "Mancozèbe",
  "timestamp": 1727000000000,
  "commune_code": "CM-BFS-02",
  "hors_ligne": true,
  "latitude": 5.683,
  "longitude": 10.633
}
# 1er appel -> 201 created
# 2e appel même local_id -> 200 already_synced
```

## 4) Tester l'authentification JWT de bout en bout

### Flux

```
Inscription -> Connexion -> Token stocké -> Requêtes authentifiées
```

### Postman

```bash
# Register
POST /auth/register
{
  "username": "agri_test",
  "phone_number": "699123456",
  "password": "Password123!"
}
# -> 201 {id, username, phone_number, is_active}

# Login
POST /auth/login
{
  "phone_number": "699123456",
  "password": "Password123!"
}
# -> 200 {access_token, token_type: "bearer"}

# Utiliser token
GET /api/scans
Authorization: Bearer <access_token>
# -> 200 [...]
```

### Android

1. Écran Auth : saisir username + phone + password + commune + code commune
2. Inscription : appelle `registerUser()` -> tente backend register + login immédiat -> token sauvé dans TokenManager + Room `lastToken`
3. Vérifier dans AuthScreen : "JWT: OK" si token présent, sinon "offline"
4. Déconnexion / Reconnexion : loginUser() tente backend login avec phone, sinon fallback local SHA-256
5. Vérifier Settings -> Backend Configuration : affiche `JWT présent (user: ..., phone: ..., commune_code: ...)`
6. Vérifier que les requêtes suivantes (uploadScan, diagnoseScan, getAlerts) incluent `Authorization: Bearer`

### Stockage

- SharedPreferences `mboa_agri_auth` : jwt_token, user_id, username, phone_number, commune_code
- Room `users.lastToken` : cache pour debug
- `TokenManager.hasToken()` utilisé dans ScanSyncWorker pour décider si sync possible

## 5) Tester la réception des alertes de commune

### Backend

```bash
GET /alerts?commune_code=CM-BFS-02
GET /alerts?commune=Bafoussam%20II
GET /alerts?crop_name=Maïs&is_active=true
```

La génération automatique des alertes (3 diagnostics concordants même pathologie + même commune + 7 jours) n'est pas encore branchée selon le rapport, mais le modèle `TerritorialAlert` existe et la route GET fonctionne.

### Android

1. Dans l'app, profil avec `communeCode = CM-BFS-02`
2. Ouvrir écran Alertes
3. Au lancement : `LaunchedEffect` appelle `fetchBackendAlerts(commune_code)` -> `GET /alerts?commune_code=CM-BFS-02`
4. UI affiche :
   - Bandeau "Backend Alerts (commune)" avec comptage backend vs local
   - Bouton refresh (icône Refresh) qui relance `fetchBackendAlerts`
   - Liste fusionnée local + backend (source LOCAL/BACKEND)
5. Si backend vide (pas encore d'alertes automatiques), l'écran montre "Aucune alerte majeure actuellement" + map Google + légende code couleur

### Test avec données factices

Si le backend n'a pas encore d'alertes, on peut créer manuellement :

```bash
POST /alerts
{
  "pathology_id": 1,
  "commune_id": 1,
  "scan_count": 5,
  "alert_level": "HIGH"
}
```

Puis vérifier dans l'app après refresh.

## 6) Générer l'APK debug et l'envoyer à l'équipe

```bash
export BACKEND_BASE_URL="https://backend-fastapi-d97b775d.fastapicloud.dev/"  # optionnel : c'est le défaut
./gradlew assembleDebug

# Vérifier
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Optionnel : info APK
aapt dump badging app/build/outputs/apk/debug/app-debug.apk | grep -E "package|version|application"

# Envoyer à l'équipe
# - app-debug.apk
# - README.md (section Configuration backend + Tests)
# - docs/TESTING_JWT_SYNC_ALERTS.md (ce fichier)
# - .env.example (avec BACKEND_BASE_URL prod)
```

### Checklist avant envoi

- [ ] BACKEND_BASE_URL prod configurée au build ou documentée pour runtime
- [ ] JWT flow testé : register -> login -> token stocké (logcat + UI)
- [ ] Scan cycle testé : PENDING -> SYNCED -> BDD (PostgreSQL scans + diagnostics)
- [ ] Idempotence testée : même local_id ne crée pas de doublon
- [ ] Alertes testées : GET /alerts?commune_code=CM-BFS-02 + écran Alertes avec refresh
- [ ] APK debug installé sur vrai téléphone (pas seulement émulateur)
- [ ] network_security_config.xml autorise prod HTTPS et dev HTTP 10.0.2.2
- [ ] Aucune clé Gemini dans APK (vérifier BuildConfig)
- [ ] VersionCode 2 / VersionName 1.1.0-jwt-sync dans build.gradle.kts

## Annexes - Commandes utiles

```bash
# Logs sync
adb logcat -s ScanSyncWorker MainViewModel ApiClient

# Clear app data pour retest
adb shell pm clear com.aistudio.mboaagri.tkplnz

# Vérifier token stocké (root)
adb shell run-as com.aistudio.mboaagri.tkplnz cat /data/data/com.aistudio.mboaagri.tkplnz/shared_prefs/mboa_agri_auth.xml

# Vérifier backend URL
adb shell run-as com.aistudio.mboaagri.tkplnz cat /data/data/com.aistudio.mboaagri.tkplnz/shared_prefs/mboa_agri_backend.xml
```

## Références code

- ApiClient : `app/src/main/java/com/example/api/ApiClient.kt`
- TokenManager : `app/src/main/java/com/example/api/TokenManager.kt`
- BackendConfig : `app/src/main/java/com/example/config/BackendConfig.kt`
- MainViewModel : `app/src/main/java/com/example/ui/MainViewModel.kt`
- ScanSyncWorker : `app/src/main/java/com/example/sync/ScanSyncWorker.kt`
- AuthScreen : `app/src/main/java/com/example/ui/screens/AuthScreen.kt`
- AlertScreen : `app/src/main/java/com/example/ui/screens/AlertScreen.kt`
- SettingsScreen : `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
- AppDatabase : `app/src/main/java/com/example/data/AppDatabase.kt` (v5)
