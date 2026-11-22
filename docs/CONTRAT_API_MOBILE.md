# Contrat API mobile — MBOA AGRI

**Source de référence :** `mobile-api.pdf` — « API MBOA AGRI, Guide pour l'application mobile » (version mobile/frontend).
Ce document est le contrat **officiel**. Tout écart entre le code et ce document est un bug.

- **URL de l'API :** `https://backend-fastapi-d97b775d.fastapicloud.dev` (slash final obligatoire côté Retrofit)
- **Format :** JSON partout, sauf `POST /api/scans/diagnose` (`multipart/form-data`)
- **Authentification :** `Authorization: Bearer <access_token>` sur les routes protégées

## 1. Authentification

| Méthode | Route | Auth | Corps | Réponse |
|---|---|---|---|---|
| `POST` | `/auth/register` | — | `{username, phone_number, password}` | `201 {id, username, phone_number, is_active}` |
| `POST` | `/auth/login` | — | `{phone_number, password}` | `200 {access_token, token_type}` |

Le token est conservé côté app dans `TokenManager` (SharedPreferences `mboa_agri_auth`).
Token absent ou invalide → `401`.

## 2. Scans

| Méthode | Route | Auth | Usage |
|---|---|---|---|
| `POST` | `/api/scans` | Bearer | Synchroniser un scan hors ligne |
| `GET` | `/api/scans?limit=50` | Bearer | Récupérer mes scans (`limit` ∈ [1, 100]) |
| `GET` | `/api/scans/{scan_id}` | Bearer | Récupérer un scan (`404` si absent) |
| `POST` | `/api/scans/diagnose` | Bearer | Diagnostic image (multipart) |

### `POST /api/scans` — corps

```json
{
  "local_id": 27,
  "plant_name": "Maïs",
  "disease_name": "Rouille",
  "confidence": 91,
  "symptoms": "Pustules sur les feuilles",
  "treatment_local": "Retirer les feuilles atteintes",
  "treatment_chemical": "Fongicide homologué",
  "timestamp": 1720000000000,
  "hors_ligne": true,
  "commune_code": "CM-001",
  "latitude": 3.848,
  "longitude": 11.502
}
```

- `local_id` : identifiant Room, **conservé tel quel** pour toutes les tentatives (idempotence `user_id + local_id`).
- `timestamp` : millisecondes Unix. `confidence` : 0–100.
- `commune_code` : **facultatif**, mais s'il est fourni il doit exister dans `communes` — sinon `422 « Code commune inconnu »`.
- Retour `201` avec `sync_status: "created"`, ou `200` avec `sync_status: "already_synced"` → dans les deux cas, marquer `SYNCED` localement.

### `POST /api/scans/diagnose` — champs multipart

| Champ | Type | Obligatoire |
|---|---|---|
| `image` | fichier JPEG/PNG/WEBP ≤ 10 Mo | oui |
| `plant_name` | texte | oui |
| `symptoms` | texte | non |
| `local_id` | entier | non |
| `commune_code` | texte | non |
| `latitude` / `longitude` | nombre | non |

Réponse `200` : `disease_name`, `confidence`, `pathology_code`, `image_url`, `severity_detected`, `information_source` (`CATALOG` ou `AI_PROVISIONAL`), `validation_status` (`CATALOG_MATCH` ou `AI_ONLY`).

## 3. Communes (toutes protégées par JWT)

| Méthode | Route | Usage |
|---|---|---|
| `GET` | `/api/communes?page=1&page_size=20&is_active=true` | Liste paginée `{items, total, page, page_size, pages}` |
| `POST` | `/api/communes` | Créer `{code, name, postal_code}` → `201` |
| `PUT` | `/api/communes/{id}` | Modifier au moins un champ → `200` |
| `PATCH` | `/api/communes/{id}/deactivate` | `is_active: false` |

**Côté app :** la liste officielle est mise en cache par `CommuneDirectory`
(SharedPreferences + rafraîchissement quotidien). Aucun `commune_code` inventé
n'est envoyé : si le code n'est pas dans la liste, on envoie `null`.

## 4. Pathologies (toutes protégées par JWT)

| Méthode | Route | Usage |
|---|---|---|
| `GET` | `/api/pathologies?crop_name=Maïs&is_active=true` | Catalogue |
| `GET` | `/api/pathologies/code/{code}` | Recherche par code |
| `GET` | `/api/pathologies/{id}` | Recherche par identifiant |
| `POST` | `/api/pathologies` | Créer (`code`, `common_name`, `crop_name` obligatoires) |
| `PUT` | `/api/pathologies/{id}` | Modifier |
| `PATCH` | `/api/pathologies/{id}/deactivate` | Désactiver |
| `PATCH` | `/api/pathologies/{id}/activate` | Activer |

Exemple d'entrée du catalogue :

```json
{
  "id": 3,
  "code": "MAIS-RL",
  "technical_name": "Puccinia sorghi",
  "common_name": "Rouille du maïs",
  "crop_name": "Maïs",
  "key_symptoms": "Pustules sur les feuilles",
  "biological_treatment": "Retirer les feuilles atteintes",
  "chemical_treatment": "Fongicide homologué",
  "default_severity": "Attention",
  "is_active": true
}
```

## 5. Alertes territoriales

| Méthode | Route | Auth | Usage |
|---|---|---|---|
| `GET` | `/api/alerts?commune_code=CM-001&pathology_id=3&crop_name=Maïs` | — (public) | Consulter les alertes |
| `POST` | `/api/alerts` | Admin | Créer une alerte |

Filtres facultatifs : `commune`, `commune_code`, `pathology_id`, `crop_name`, `start_date`, `end_date` (ISO 8601).

```json
[
  {
    "id": 1,
    "pathology_id": 3,
    "commune_id": 5,
    "scan_count": 14,
    "alert_level": "Alerte",
    "created_at": "2026-11-20T10:30:00Z"
  }
]
```

`alert_level` est un libellé en clair (`Alerte`, `Attention`, `Info`) ; les anciennes
valeurs `HIGH` / `MEDIUM` / `LOW` restent tolérées par l'app.

> Le préfixe historique `/alerts` reste exposé par le backend pour compatibilité,
> mais **l'app mobile utilise `/api/alerts`**.

## 6. Codes d'erreur à gérer côté mobile

| Code | Signification | Comportement app |
|---|---|---|
| `401` | Token absent/invalide | Retry après reconnexion |
| `403` | Compte désactivé / accès refusé | Retry après reconnexion |
| `404` | Ressource introuvable | Ne pas rejouer |
| `409` | Code ou nom déjà utilisé | Message à l'utilisateur |
| `413` | Image trop volumineuse | Compresser avant envoi |
| `415` | Format d'image non accepté | Convertir en JPEG |
| `422` | Données invalides (ex. commune inconnue) | Marquer `FAILED`, corriger la donnée |
| `503` | Diagnostic temporairement indisponible | Réessayer plus tard |

## 7. Mode hors ligne

En cas d'erreur réseau : conserver le scan localement (`PENDING`) et réessayer avec
le **même `local_id`**. Une réponse `already_synced` signifie que le serveur possède
déjà le scan : marquer `SYNCED`.

## 8. État de conformité du code

| Élément | État |
|---|---|
| Routes `/auth/*` | ✅ conformes |
| `POST /api/scans` + idempotence | ✅ conforme |
| `POST /api/scans/diagnose` | ✅ conforme |
| `/api/communes` (service Retrofit + sélecteur UI + cache) | ✅ conforme |
| `/api/pathologies` | ✅ conforme |
| `/api/alerts` | ✅ corrigé (l'app appelait `/alerts`) |
| Niveaux d'alerte en clair | ✅ gérés (`Alerte` / `Attention` / `Info`) |
| Gestion `already_synced` | ✅ `SYNCED` |
| Codes `401/403/413/415/422` | ✅ traités dans `ScanSyncWorker` |

Vérifications automatiques : `backend/tests/test_mobile_contract.py` (routes + schémas).
