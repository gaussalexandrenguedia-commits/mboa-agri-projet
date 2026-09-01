# Rapport hebdomadaire — Développement du backend MBOA AGRI

**Projet :** MBOA AGRI / AgroScanEdu AI  
**Domaine :** Diagnostic et alerte des maladies agricoles  
**Rôle :** Développement backend FastAPI  
**Semaine :** Semaine 8 — du 31 août au 5 septembre 2026  
**Branche Git :** `feature/backend-fastapi`  
**Statut :** En cours

```markdown
## 2026-09-01 — Configuration de base du backend
```

## 1. Objet du rapport

Ce rapport présente les travaux réalisés dans le cadre de la préparation du backend de l’application MBOA AGRI. L’objectif est de transformer le prototype Android actuel, qui conserve principalement les diagnostics en local, en une architecture partagée capable de synchroniser les observations, de produire des alertes territoriales et d’alimenter un futur tableau de bord.

## 2. Travaux réalisés

### 2.1 Analyse des documents de référence

Les règles importantes identifiées sont les suivantes : les alertes doivent être basées sur plusieurs observations concordantes, la fenêtre d’analyse initiale est de sept jours, une alerte ne doit pas être dupliquée pour une même zone et une même pathologie, et les diagnostics de faible confiance doivent pouvoir être révisés par un administrateur ou un agronome.

### 2.2 Analyse de l’application Android existante

Les fichiers `AppDatabase.kt`, `Entities.kt`, `Daos.kt`, `GeminiApiClient.kt`, `Cropdiagnostic.kt` et `mainViewModel.kt` ont été examinés.

L’application utilise actuellement Room pour stocker localement les utilisateurs, les résultats de scan, les données de sol et les données du forum. Le modèle `ScanResultEntity` contient déjà les informations principales d’un diagnostic : plante, maladie, confiance, symptômes, traitements, horodatage et coordonnées GPS.

L’appel Gemini est actuellement effectué directement depuis Android à l’aide de `BuildConfig.GEMINI_API_KEY`. Cette organisation devra être remplacée par un appel vers le backend FastAPI. Le backend pourra ensuite appeler Gemini de manière sécurisée, valider la réponse et renvoyer à Android un format normalisé.

L’analyse du ViewModel a également mis en évidence plusieurs responsabilités actuellement exécutées sur le téléphone : appel Gemini, interprétation d’un JSON libre, sélection d’un diagnostic de secours, enregistrement local du scan et calcul local des alertes. Ces responsabilités seront progressivement déplacées ou contrôlées par le backend.

### 2.3 Mise en place de Git et de la branche de travail

La branche utilisée est :

feature/backend-fastapi

Cette organisation permet de développer indépendamment de la branche `main` et de soumettre les changements par Pull Request après vérification.

### 3.4 Organisation du dossier backend

Un dossier `backend` a été créé à la racine du dépôt Android afin de séparer le code Python du code Kotlin. L’environnement virtuel Python `.venv` a été créé et activé dans ce dossier.

La structure prévue suit l’organisation déjà maîtrisée par le développeur :

```text
backend/
├── .venv/
├── alembic/
├── app/
│   ├── core/
│   ├── crud/
│   ├── models/
│   ├── routers/
│   ├── schemas/
│   ├── services/
│   ├── config.py
│   ├── database.py
│   ├── main.py
│   └── __init__.py
├── .env
├── .env.example
├── .gitignore
├── alembic.ini
├── requirements.txt
└── README.md
```

Cette structure sépare la configuration, la sécurité, les modèles de données, les opérations CRUD, les schémas Pydantic, les routes HTTP et la logique métier.

### 3.5 Installation de l’environnement technique

L’environnement virtuel a été activé avec succès. Les dépendances principales ont été installées : FastAPI, Uvicorn, SQLAlchemy, Psycopg, Pydantic Settings, Alembic, `python-jose`, `pwdlib` et `python-multipart`.

Ces dépendances permettront de construire une API HTTP, de communiquer avec PostgreSQL, de versionner les migrations, de sécuriser l’authentification et de gérer les futurs envois de fichiers.

## 4. État d’avancement

| Élément                            | État     | Commentaire                                                       |
| ---------------------------------- | -------- | ----------------------------------------------------------------- |
| Analyse des documents fonctionnels | Réalisé  | Vision, MVP et feuille de route étudiés                           |
| Analyse du modèle Room             | Réalisé  | Entités et DAO examinés                                           |
| Analyse de l’appel Gemini Android  | Réalisé  | Clé actuellement exposée dans l’application, migration nécessaire |
| Branche Git dédiée                 | Réalisé  | `feature/backend-fastapi` publiée                                 |
| Dossier `backend`                  | Réalisé  | Séparé du code Android                                            |
| Environnement virtuel Python       | Réalisé  | `.venv` actif                                                     |
| Dépendances Python                 | Réalisé  | Installation terminée avec succès                                 |
| Configuration FastAPI              | En cours | `config.py`, `main.py` et endpoint de santé à finaliser et tester |
| Connexion PostgreSQL               | À faire  | Dépend des paramètres de la base et de la configuration locale    |
| Authentification JWT               | À faire  | Première fonctionnalité métier prévue                             |
| Endpoint `/scans`                  | À faire  | Contrat à aligner avec Android                                    |
| Moteur d’alertes                   | À faire  | À implémenter après persistance des scans                         |

## 5. Prochaines étapes

La prochaine étape consiste à finaliser la configuration minimale de FastAPI et à vérifier le démarrage du serveur avec un endpoint `/health`.

Ensuite, le projet sera configuré avec `config.py`, `database.py`, Alembic et les variables d’environnement. Les premiers modèles SQLAlchemy porteront sur les utilisateurs, les profils agricoles, les communes, les pathologies, les scans, les diagnostics et les alertes.

L’implémentation fonctionnelle commencera par l’inscription et la connexion, suivies de la création et de la consultation des scans. Le moteur de diagnostic Gemini sera ensuite placé côté serveur, avec validation stricte de la réponse et repli contrôlé en cas d’indisponibilité.
