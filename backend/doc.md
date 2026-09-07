# Documentation - MBOA AGRI Backend API

**Projet** : Application mobile pour le diagnostic des maladies des plantes  
**Statut** : En développement  
**Stack** : FastAPI + PostgreSQL + SQLAlchemy

---

## 📋 Vue d'ensemble

MBOA AGRI est une plateforme de diagnostic des maladies des plantes agricoles. Le backend fourni une API REST pour :

- Authentification des utilisateurs par numéro de téléphone
- Gestion des diagnostics de scans de plantes
- Gestion des alertes territoriales de maladies par commune
- Gestion des bases de communes et pathologies

---

## 🏗️ Architecture du projet

### Structure des répertoires

```
app/
├── main.py                  # Point d'entrée FastAPI
├── config.py               # Configuration (variables d'environnement)
├── database.py             # Connexion SQLAlchemy
├── core/
│   └── security.py         # Hachage/vérification mot de passe, JWT
├── crud/                   # Couche accès données
│   ├── user.py
│   ├── scan.py
│   ├── alert.py
│   └── commune.py
├── models/                 # ORM SQLAlchemy
│   ├── user.py
│   ├── scan.py
│   ├── pathology.py
│   ├── territorial_alert.py
│   └── commune.py
├── schemas/                # Validation Pydantic (requêtes/réponses)
│   ├── auth.py
│   ├── scan.py
│   ├── alert.py
│   └── diagnostic.py
├── routers/                # Endpoints API
│   ├── auth.py            # /auth
│   ├── scan.py            # /api/scans
│   ├── diagnostic.py      # /api/scans/diagnose
│   └── alerts.py          # /alerts
└── services/               # Logique métier (à développer)

alembic/                    # Migrations de base de données
```

### Flux d'une requête

```
Client → Router → Schema (validation) → CRUD (DB) → Model → Schema (sérialisation) → Client
```

---

## 🗄️ Modèles de données

### Users (Utilisateurs)

```
- id (PK)
- username (String)
- phone_number (String, UNIQUE)
- password_hash (String)
- is_active (Boolean, default: True)
- created_at (DateTime)
```

### Scans

```
- id (PK)
- local_id (Integer) - sync côté mobile
- user_id (FK → Users)
- pathology_id (FK → Pathologies)
- commune_id (FK → Communes)
- plant_name (String)
- disease_name (String)
- confidence (Integer) %
- symptoms (Text)
- treatment_local (Text)
- treatment_chemical (Text)
- timestamp (BigInteger) - timestamp du scan mobile
- hors_ligne (Boolean) - synchronisé hors lige
- latitude, longitude (Float)
- received_at (DateTime)
```

### Pathologies

```
- id (PK)
- technical_name (String, nullable)
- common_name (String)
- crop_name (String)
- key_symptoms (Text)
- biological_treatment (Text)
- chemical_treatment (Text)
- default_severity (String: "Attention", "Alerte", "Urgence")
- is_active (Boolean)
```

### Communes

```
- id (PK)
- name (String, UNIQUE)
- postal_code (String, nullable)
```

### TerritorialAlerts (Alertes par commune/pathologie)

```
- id (PK)
- pathology_id (FK → Pathologies)
- commune_id (FK → Communes)
- scan_count (Integer)
- alert_level (String)
- created_at (DateTime)
```

---

## 🔌 Endpoints API implémentés

### ✅ Authentication

| Méthode | Endpoint         | Description                   | Status  |
| ------- | ---------------- | ----------------------------- | ------- |
| POST    | `/auth/register` | Créer un compte utilisateur   | ✅ Fait |
| POST    | `/auth/login`    | Connexion, retourne JWT token | ✅ Fait |

**Requête Register :**

```json
{
  "username": "string",
  "phone_number": "string",
  "password": "string"
}
```

**Requête Login :**

```json
{
  "phone_number": "string",
  "password": "string"
}
```

**Réponse Login :**

```json
{
  "access_token": "string",
  "token_type": "bearer"
}
```

---

### ✅ Scans

| Méthode | Endpoint          | Description                   | Status  |
| ------- | ----------------- | ----------------------------- | ------- |
| POST    | `/api/scans`      | Créer un nouveau scan         | ✅ Fait |
| GET     | `/api/scans/{id}` | Récupérer un scan par ID      | ✅ Fait |
| GET     | `/api/scans`      | Lister tous les scans (limit) | ✅ Fait |

**Requête POST /api/scans :**

```json
{
  "local_id": 1,
  "user_id": 1,
  "pathology_id": 1,
  "commune_id": 1,
  "plant_name": "Maïs",
  "disease_name": "Rouille",
  "confidence": 95,
  "symptoms": "Tâches rouges sur feuilles",
  "treatment_local": "Cendre + savon",
  "treatment_chemical": "Fongicide au cuivre",
  "timestamp": 1693411200000,
  "hors_ligne": false,
  "latitude": 12.5,
  "longitude": -8.5
}
```

---

### 🔄 Diagnostics (En développement)

| Méthode | Endpoint              | Description          | Status       |
| ------- | --------------------- | -------------------- | ------------ |
| POST    | `/api/scans/diagnose` | Diagnostic par image | 🚧 Structure |

**Endpoint prêt à recevoir les images** mais la logique d'intégration Gemini n'est pas implémentée.

---

### ✅ Alertes territoriales

| Méthode | Endpoint  | Description                            | Status  |
| ------- | --------- | -------------------------------------- | ------- |
| GET     | `/alerts` | Récupérer alertes par commune (param)  | ✅ Fait |
| POST    | `/alerts` | Créer une nouvelle alerte territoriale | ✅ Fait |

**Requête POST /alerts :**

```json
{
  "pathology_id": 1,
  "commune_id": 1,
  "scan_count": 5,
  "alert_level": "Alerte"
}
```

**Requête GET /alerts?commune=SomeName :**

```
params: commune (String, requis)
```

---

### 🏥 Health Check

| Méthode | Endpoint  | Description                | Status  |
| ------- | --------- | -------------------------- | ------- |
| GET     | `/health` | Vérifier l'état du service | ✅ Fait |
| GET     | `/`       | Message de bienvenue       | ✅ Fait |

---

## ✅ État d'avancement

### Complété ✅

- [x] Mise en place architecture FastAPI
- [x] Configuration base de données PostgreSQL
- [x] Modèles SQLAlchemy (User, Scan, Pathology, Commune, TerritorialAlert)
- [x] CRUD utilisateurs avec authentification JWT
- [x] CRUD scans (créer, lire)
- [x] CRUD alertes territoriales
- [x] CRUD communes
- [x] Validation des données (Pydantic schemas)
- [x] Migrations Alembic
  - `ce7fe908f482_create_users_table.py`
  - `3d7996cf0e45_create_scans_table.py`
  - `f29d3ede95b3_enrich_scans_with_relations.py`
  - `daa7aafb4bf6_create_health_check_tabel.py`
  - `d90f213c1795_change_integer_to_biginteger_in_scan_.py`
- [x] Sécurité : Hachage de mot de passe (Argon2), JWT
- [x] Gestion d'erreurs et codes de statut HTTP

### À faire 🚧

**Haute Priorité**

- [ ] **Intégration Gemini AI** - Implémentation du diagnostic par image
  - Configurer Gemini API
  - Créer service `services/gemini.py`
  - Lier endpoint `/api/scans/diagnose` au service
  - Stockage des images (local ou cloud)
- [ ] **Authentification avancée**
  - Vérification du JWT dans les endpoints (protéger les routes)
  - Middleware d'authentification
  - Refresh tokens
- [ ] **Validation métier**
  - Contrôle des doublons (user_id + pathology_id + commune_id)
  - Règles de création d'alerts (seuils)

**Moyenne Priorité**

- [ ] **Endpoints manquants**
  - GET/PUT/DELETE scans (mise à jour, suppression)
  - Filtrage avancé des scans (par date, utilisateur, commune)
  - Statistiques des scans par commune/pathologie
- [ ] **Synchronisation offline**
  - Gestion complète du champ `hors_ligne`
  - Queue de synchronisation
- [ ] **Tests**
  - Tests unitaires (CRUD)
  - Tests d'intégration (endpoints)
  - Tests de sécurité (JWT)

**Basse Priorité**

- [ ] Documentation Swagger/OpenAPI (auto-générée par FastAPI)
- [ ] Logs structurés
- [ ] Monitoring et observabilité
- [ ] Performance : Indexation BD, pagination, caching

---

## 🚀 Démarrage du projet

### Prérequis

- Python 3.10+
- PostgreSQL 13+
- pip

### Installation

1. **Cloner et entrer dans le répertoire**

```bash
cd backend
```

2. **Créer un environnement virtuel (optionnel mais recommandé)**

```bash
python -m venv venv
# Windows:
venv\Scripts\activate
# macOS/Linux:
source venv/bin/activate
```

3. **Installer les dépendances**

```bash
pip install -r requirements.txt
```

4. **Configurer les variables d'environnement**

```bash
# Créer fichier .env dans backend/
DATABASE_URL=postgresql://user:password@localhost:5432/mboa_agri
GEMINI_API_KEY=your_gemini_key
JWT_SECRET_KEY=your_secret_key
```

5. **Exécuter les migrations**

```bash
alembic upgrade head
```

6. **Démarrer le serveur**

```bash
uvicorn app.main:app --reload
```

Le serveur sera disponible sur `http://localhost:8000`

**Documentation interactive** : `http://localhost:8000/docs` (Swagger UI)

---

## 📝 Notes pour les développeurs

### Créer une nouvelle migration

```bash
alembic revision --autogenerate -m "Description de la migration"
alembic upgrade head
```

### Passer un token JWT

```bash
# Après login, inclure dans les headers:
Authorization: Bearer <access_token>
```

### Structure des schemas Pydantic

Les schemas se trouvent dans `app/schemas/` :

- **Requêtes** : Classes suffixées par `Request`, ex: `ScanCreateRequest`
- **Réponses** : Classes suffixées par `Response`, ex: `ScanResponse`

### Ajouter un nouvel endpoint

1. Créer la fonction dans `routers/mon_router.py`
2. Créer les schemas de requête/réponse dans `schemas/mon_schema.py`
3. Créer/mettre à jour les CRUD dans `crud/mon_crud.py`
4. Importer le router dans `main.py`
5. Créer les migrations si changement BD

### Gestion des erreurs

Utiliser `HTTPException` de FastAPI avec les codes de statut appropriés :

```python
from fastapi import HTTPException, status
raise HTTPException(
    status_code=status.HTTP_404_NOT_FOUND,
    detail="Ressource non trouvée"
)
```

### Sécurité

- Tous les mots de passe sont hachés avec Argon2
- Les tokens JWT ont une durée d'expiration de 60 minutes (configurable)
- Vérifier l'authentification sur les endpoints sensibles

---

## 🔗 Ressources

- [FastAPI Docs](https://fastapi.tiangolo.com/)
- [SQLAlchemy Docs](https://docs.sqlalchemy.org/)
- [Alembic Docs](https://alembic.sqlalchemy.org/)
- [Pydantic Docs](https://docs.pydantic.dev/)

---

**Dernière mise à jour** : Septembre 2026  
**Responsable** : [Votre nom]
