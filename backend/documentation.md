# MBOA AGRI — Suivi du développement du backend

**Date : 4 septembre 2026**  
**Branche Git : `feature/backend-fastapi`**  
**Technologie principale : FastAPI**

## 1. Objectif du backend

Le backend de MBOA AGRI sert d’intermédiaire sécurisé entre l’application Android et la base de données. Il reçoit les données produites par l’application mobile, les enregistre, prépare les échanges avec le service d’intelligence artificielle Gemini et permet de diffuser des alertes territoriales concernant les maladies agricoles.

Le fonctionnement retenu est le suivant : l’application mobile envoie les requêtes à l’API ; le backend traite ces requêtes et communique avec la base de données et, pour le diagnostic, avec Gemini. Les clés secrètes et les mots de passe ne doivent donc jamais être stockés dans l’application Android ni dans Git.

## 2. Structure du projet

Le projet suit une organisation modulaire inspirée de la structure utilisée dans les autres projets FastAPI :

| Dossier        | Responsabilité                                            |
| -------------- | --------------------------------------------------------- |
| `app/core`     | Sécurité, dépendances et configuration technique commune  |
| `app/crud`     | Opérations de lecture et d’écriture en base de données    |
| `app/models`   | Modèles SQLAlchemy représentant les tables PostgreSQL     |
| `app/routers`  | Routes HTTP de l’API                                      |
| `app/schemas`  | Validation des requêtes et format des réponses            |
| `app/services` | Logique métier ou intégrations externes, notamment Gemini |
| `alembic`      | Versions et exécution des migrations de base de données   |

La configuration est centralisée dans `app/config.py`. Les variables sensibles sont placées dans `.env`, tandis que `.env.example` peut être partagé et versionné sans contenir de secrets.

## 3. Base de données et migrations

La base utilisée est PostgreSQL 17, avec l’extension PostGIS activée dans la base `mboa_agri`. PostGIS sera utilisé pour les traitements géographiques avancés et les alertes territoriales. Les coordonnées simples `latitude` et `longitude` restent conservées pour assurer la compatibilité avec les données Android.

Les migrations sont gérées avec Alembic. Les principales migrations déjà réalisées sont :

| Migration      | Contenu                                                           |
| -------------- | ----------------------------------------------------------------- |
| `daa7aafb4bf6` | Création de la table `health_checks`                              |
| `ce7fe908f482` | Création de la table `users`                                      |
| `3d7996cf0e45` | Création initiale de la table `scans`                             |
| `f29d3ede95b3` | Enrichissement de `scans` avec les relations et les champs métier |

La table système PostGIS `spatial_ref_sys` ne doit pas être supprimée par les migrations Alembic. La configuration d’Alembic a été adaptée afin d’ignorer les tables système PostGIS lors de l’autogénération.

## 4. Authentification et sécurité

L’authentification repose sur le numéro de téléphone, qui est unique, et un mot de passe. Ce choix évite les ambiguïtés liées aux noms d’utilisateur identiques.

Les routes actuellement disponibles sont :

| Méthode | Route            | Fonction                                                                          |
| ------- | ---------------- | --------------------------------------------------------------------------------- |
| `POST`  | `/auth/register` | Inscrire un utilisateur avec son nom, son numéro de téléphone et son mot de passe |
| `POST`  | `/auth/login`    | Vérifier les identifiants et retourner un token JWT                               |

|
Les mots de passe sont hachés avant l’enregistrement et ne sont jamais retournés dans les réponses API. Le token JWT possède une durée d’expiration configurée dans les paramètres du backend, actuellement fixée à 60 minutes par défaut.

## 5. Synchronisation des scans Android

L’application Android fonctionne en mode offline-first avec Room. Un scan peut être enregistré localement puis synchronisé lorsque le réseau est disponible par l’intermédiaire de `ScanSyncWorker`.

Le backend accepte les données compatibles avec `ScanResultEntity`, notamment :

| Champ                   | Rôle                                            |
| ----------------------- | ----------------------------------------------- |
| `local_id`              | Identifiant du scan sur le téléphone            |
| `plant_name`            | Nom de la culture ou de la plante               |
| `disease_name`          | Maladie détectée ou enregistrée localement      |
| `confidence`            | Niveau de confiance du diagnostic               |
| `symptoms`              | Symptômes observés                              |
| `treatment_local`       | Traitement local recommandé                     |
| `treatment_chemical`    | Traitement chimique recommandé                  |
| `timestamp`             | Date Android au format millisecondes            |
| `latitude`, `longitude` | Position GPS facultative                        |
| `hors_ligne`            | Indique si le scan a été réalisé sans connexion |

Le champ `timestamp` est de type `BIGINT` afin de pouvoir recevoir les valeurs Android en millisecondes, qui dépassent la capacité d’un entier PostgreSQL standard.

Les routes fonctionnelles sont :

| Méthode | Route                  | Fonction                                                   |
| ------- | ---------------------- | ---------------------------------------------------------- |
| `POST`  | `/api/scans`           | Recevoir et enregistrer un scan synchronisé depuis Android |
| `GET`   | `/api/scans`           | Récupérer les scans                                        |
| `GET`   | `/api/scans/{scan_id}` | Récupérer un scan par son identifiant                      |

|

## 6. Communes et alertes territoriales

Les modèles `Commune` et `TerritorialAlert` ont été ajoutés pour préparer le ciblage géographique des alertes. Les opérations CRUD des communes et des alertes sont séparées des routes, conformément à l’architecture du projet.

Les routes d’alertes actuellement exposées sont :

| Méthode | Route               | Fonction                                                       |
| ------- | ------------------- | -------------------------------------------------------------- |
| `GET`   | `/alerts?commune=X` | Retourner les alertes actives liées à une commune              |
| `POST`  | `/alerts`           | Créer une alerte pour une pathologie et une commune existantes |

La route `GET /alerts?commune=X` commence par rechercher la commune à partir de son nom. Si elle n’existe pas, l’API retourne `404` avec le message `Commune introuvable.`. Si elle existe mais ne possède aucune alerte, la réponse est une liste vide.

La route `POST /alerts` reçoit actuellement un payload de la forme suivante :

```json
{
  "pathology_id": 1,
  "commune_id": 1,
  "scan_count": 3,
  "alert_level": "Attention"
}
```

Avant de créer l’alerte, le backend vérifie que la commune et la pathologie indiquées existent. Les coordonnées GPS précises des agriculteurs ne sont pas exposées dans la réponse d’une alerte ; l’alerte est regroupée au niveau de la commune.

Cette création manuelle sert pour le moment aux tests et à l’administration. Une étape ultérieure devra automatiser la génération d’une alerte lorsqu’un nombre défini de scans concordants est atteint dans une commune.

## 7. Diagnostic par image

La route de diagnostic par image est préparée :

| Méthode | Route                 | État                                                                                       |
| ------- | --------------------- | ------------------------------------------------------------------------------------------ |
| `POST`  | `/api/scans/diagnose` | Réception de l’image et des informations associées fonctionnelle ; appel Gemini à intégrer |

Le backend reçoit notamment le fichier image, le nom de la plante, les symptômes et, lorsque disponibles, la latitude et la longitude. L’appel à Gemini Vision devra être réalisé côté serveur à partir d’une clé placée dans `.env`. Le résultat devra ensuite être validé et renvoyé au mobile sous un format stable.

## 8. Tests déjà effectués

Le serveur FastAPI démarre correctement et la documentation Swagger est accessible. Les tests réalisés ont validé l’inscription, la connexion JWT, la protection du mot de passe dans les réponses, la synchronisation d’un scan Android, la récupération des scans, la réception d’une image de diagnostic et l’accès à la route des alertes.

Un test de la route `GET /alerts?commune=...` avec une commune inexistante a correctement retourné une réponse HTTP `404` :

```json
{
  "detail": "Commune introuvable."
}
```

## 9. Prochaines étapes

Les prochaines priorités sont l’intégration réelle de Gemini Vision, la création et le peuplement du catalogue des 22 pathologies, l’ajout éventuel d’une colonne géographique `Geography(Point, 4326)` dans `Scan`, puis l’automatisation de la génération des alertes à partir des scans concordants.

Il faudra également continuer à tester chaque nouvelle route dans Swagger ou avec Postman, créer les migrations correspondantes avec Alembic et mettre à jour le journal de développement ainsi que les rapports hebdomadaires.

> **État actuel :** l’authentification, la gestion des communes et les routes de consultation et de création des alertes sont en place. L’intégration Gemini et l’automatisation métier des alertes restent à finaliser.

## Références techniques

[1]: https://fastapi.tiangolo.com/ "FastAPI Documentation"
[2]: https://docs.sqlalchemy.org/en/20/ "SQLAlchemy 2.0 Documentation"
[3]: https://alembic.sqlalchemy.org/en/latest/ "Alembic Documentation"
[4]: https://postgis.net/documentation/ "PostGIS Documentation"

**Date : 14 septembre 2026**  
**Branche Git : `feature/backend-fastapi`**  
**Technologie principale : FastAPI**

# Rapport d’état du backend MBOA AGRI

## 1. Résumé exécutif

Le backend FastAPI dispose maintenant du socle nécessaire pour authentifier les utilisateurs, recevoir des scans mobiles, diagnostiquer une image avec Gemini, stocker les images sur Cloudinary, gérer un catalogue de pathologies et préparer les analyses territoriales avec PostgreSQL/PostGIS.

Le flux de diagnostic en ligne est fonctionnel dans son principe et a été testé avec une clé Gemini valide. Les routes du catalogue de pathologies fonctionnent également. La synchronisation offline est idempotente et garantit qu’une même paire `utilisateur serveur + local_id Room` ne crée pas plusieurs scans.

L’application n’est toutefois pas encore complète. Les principaux travaux restants concernent la synchronisation des scans offline vers la table analytique `diagnostics`, la génération automatique et historisée des alertes, le peuplement expert des 22 pathologies, la gestion avancée des communes et des géométries, la couverture de tests, puis le déploiement et les tests de production.

> **État global recommandé :** socle backend opérationnel, fonctionnalités métier principales encore à finaliser avant une mise en production complète.

## 2. Architecture actuellement retenue

Le backend utilise PostgreSQL comme base principale et PostGIS pour les données géographiques. L’application mobile utilise Room avec ses propres identifiants locaux. Les identifiants Room ne sont donc pas utilisés comme références PostgreSQL.

La commune est identifiée entre les deux applications par un code métier stable, par exemple `CM-BFS-02`. PostgreSQL conserve son propre `commune_id` interne. Les limites administratives sont stockées dans `communes.geom` sous forme de `MULTIPOLYGON` avec le système de référence spatial `4326`.

Un diagnostic est localisé par un point GPS. Le modèle conserve les champs `latitude` et `longitude` pour la compatibilité avec l’application mobile et construit également `position_gps` sous forme de `GEOGRAPHY(Point, 4326)` pour les requêtes PostGIS.

Les observations individuelles sont destinées à être conservées dans `diagnostics`. Les alertes territoriales sont conservées séparément dans `alerts` et représentent une agrégation de plusieurs diagnostics.

## 3. Fonctionnalités déjà réalisées

### 3.1 Authentification et identité serveur

L’authentification par JWT est disponible. Les routes protégées récupèrent l’utilisateur connecté à partir du token Bearer.

L’identité serveur ne dépend pas du `user_id` envoyé par Room. Le `user_id` mobile peut être différent de l’identifiant PostgreSQL. Pour les routes sécurisées, le propriétaire du scan est déterminé par l’utilisateur présent dans le JWT.

Cette séparation évite qu’un utilisateur puisse créer un scan au nom d’un autre utilisateur en modifiant simplement un identifiant local dans le payload.

### 3.2 Réception et synchronisation des scans mobiles

La route suivante existe :

```text
POST /api/scans
```

Elle reçoit les données d’un scan mobile et utilise l’identité du JWT. La synchronisation offline est idempotente grâce à la combinaison :

```text
user_id PostgreSQL + local_id Room
```

Si le mobile renvoie deux fois le même scan offline, le backend retourne le scan déjà enregistré au lieu de créer un doublon.

Le contrat prend également en compte `commune_code`. Le champ `commune_id` Room peut rester dans le payload pour compatibilité, mais il n’est pas utilisé comme référence PostgreSQL.

### 3.3 Consultation des scans

Les routes de consultation des scans limitent la lecture aux scans appartenant à l’utilisateur connecté. Cette règle correspond au périmètre actuel et pourra être étendue plus tard pour les consultations régionales ou administratives.

Les fonctionnalités régionales ne sont pas nécessaires pour la première version du produit et ne doivent pas être ajoutées avant la finalisation du flux principal.

### 3.4 Catalogue des pathologies

Le modèle `Pathology` possède maintenant un code métier unique :

```text
pathologies.code
```

Ce code permet de faire correspondre une réponse Gemini au catalogue expert sans dépendre du nom libre de la maladie.

Le catalogue contient notamment les informations suivantes :

| Champ                  | Rôle                                                       |
| ---------------------- | ---------------------------------------------------------- |
| `code`                 | Identifiant métier stable utilisé par Gemini et le backend |
| `technical_name`       | Nom scientifique ou technique facultatif                   |
| `common_name`          | Nom compréhensible par l’utilisateur                       |
| `crop_name`            | Culture concernée                                          |
| `key_symptoms`         | Symptômes validés par les experts                          |
| `biological_treatment` | Traitement biologique validé                               |
| `chemical_treatment`   | Traitement chimique validé                                 |
| `default_severity`     | Gravité de référence du catalogue                          |
| `is_active`            | Activation ou désactivation sans suppression physique      |

### 3.5 Routes du catalogue de pathologies

Les routes suivantes sont disponibles et nécessitent actuellement un JWT valide. Aucun rôle administrateur n’est encore imposé, conformément à la décision prise pour cette phase.

```text
GET    /api/pathologies
GET    /api/pathologies/code/{code}
GET    /api/pathologies/{pathology_id}
POST   /api/pathologies
PUT    /api/pathologies/{pathology_id}
PATCH  /api/pathologies/{pathology_id}/activate
PATCH  /api/pathologies/{pathology_id}/deactivate
```

Les filtres disponibles sont :

```text
GET /api/pathologies?crop_name=Maïs
GET /api/pathologies?is_active=true
GET /api/pathologies?crop_name=Maïs&is_active=true
```

La désactivation est privilégiée à la suppression physique afin de préserver l’historique des anciens diagnostics.

### 3.6 Diagnostic en ligne avec Gemini

La route suivante existe :

```text
POST /api/scans/diagnose
```

Elle reçoit une requête `multipart/form-data` contenant notamment :

```text
image
plant_name
symptoms
local_id
commune_code
latitude
longitude
```

Le flux actuel est le suivant :

```text
Réception de l’image
    ↓
Validation du type et de la taille
    ↓
Chargement du catalogue compatible avec la culture
    ↓
Envoi de l’image et du catalogue à Gemini
    ↓
Validation de la réponse JSON Gemini
    ↓
Recherche du code dans pathologies
    ↓
Utilisation du catalogue expert si le code existe
    ↓
Création d’une candidate si le code n’existe pas
    ↓
Upload de l’image sur Cloudinary
    ↓
Enregistrement du diagnostic
    ↓
Réponse au mobile
```

Gemini reçoit une liste contrôlée des pathologies de la culture concernée. Le prompt lui demande de retourner le code existant lorsqu’une correspondance est possible et `null` dans le cas contraire.

### 3.7 Priorité aux données expertes

Lorsqu’une correspondance est trouvée dans `pathologies`, le backend utilise les informations du catalogue :

```text
pathologies.key_symptoms
pathologies.biological_treatment
pathologies.chemical_treatment
pathologies.default_severity
```

Les textes générés par Gemini ne remplacent donc pas les recommandations validées du catalogue.

Deux niveaux de gravité sont conservés :

| Champ               | Origine                                        |
| ------------------- | ---------------------------------------------- |
| `severity_detected` | Gravité estimée par Gemini pour le cas observé |
| `severity_default`  | Gravité de référence du catalogue expert       |

### 3.8 Gestion des pathologies non reconnues

Lorsque Gemini propose une maladie absente du catalogue, le backend génère un code déterministe de type :

```text
CANDIDATE_MAIS_NOM_DE_LA_MALADIE
```

La proposition est enregistrée dans `pathology_candidates`. Les occurrences suivantes de la même proposition réutilisent la candidate et incrémentent `occurrence_count`.

Le diagnostic est marqué comme provisoire :

```text
information_source = AI_PROVISIONAL
validation_status = AI_ONLY
```

Les symptômes et traitements IA retournés dans ce cas doivent être affichés comme des informations non encore validées par un expert.

### 3.9 Stockage permanent des images avec Cloudinary

Les images de diagnostic en ligne sont envoyées à Cloudinary après l’appel Gemini. PostgreSQL ne conserve pas les octets de l’image. Il conserve seulement l’URL HTTPS retournée par Cloudinary dans :

```text
diagnostics.image_url
```

La configuration utilise les variables d’environnement suivantes :

```env
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=mboa_agri/diagnostics
```

Le dossier Cloudinary est créé automatiquement au premier upload si nécessaire.

Le service utilise l’API REST Cloudinary et conserve le secret uniquement côté backend.

### 3.10 PostgreSQL et PostGIS

La migration spatiale ajoute :

```text
communes.code
communes.geom
pathologies.code
```

Elle crée également :

```text
diagnostics
pathology_candidates
```

La géométrie de commune est de type :

```sql
geometry(MULTIPOLYGON, 4326)
```

La position d’un diagnostic est de type :

```sql
geography(POINT, 4326)
```

La migration active également l’extension PostGIS avec :

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 3.11 Alertes et filtres disponibles

Le modèle `TerritorialAlert` existe déjà et correspond à la table `alerts`. Il conserve les alertes agrégées par commune et pathologie.

La route de consultation est :

```text
GET /alerts
```

Les filtres disponibles sont :

| Filtre           | Exemple                                  |
| ---------------- | ---------------------------------------- |
| Commune par nom  | `/alerts?commune=Bafoussam%20II`         |
| Commune par code | `/alerts?commune_code=CM-BFS-02`         |
| Pathologie       | `/alerts?pathology_id=4`                 |
| Culture          | `/alerts?crop_name=Maïs`                 |
| Début de période | `/alerts?start_date=2026-09-01T00:00:00` |
| Fin de période   | `/alerts?end_date=2026-09-13T23:59:59`   |

Les filtres peuvent être combinés. Le modèle actuel conserve les informations essentielles suivantes :

```text
pathology_id
commune_id
scan_count
alert_level
created_at
```

La logique automatique de génération des alertes n’est pas encore branchée sur les diagnostics.

### 3.12 Tests et validations effectués

La compilation Python des fichiers applicatifs et des migrations a été validée. Les imports des modèles et des routeurs ont été vérifiés.

Le test de synchronisation offline couvre notamment :

- le refus d’utiliser le `user_id` Room comme identité serveur ;
- l’utilisation de l’utilisateur provenant du JWT ;
- le retour du même scan lors d’une nouvelle synchronisation avec le même `local_id`.

Les tests actuels passent avec le résultat :

```text
Ran 2 tests
OK
```

Le test SQLite doit utiliser un schéma minimal pour la logique de synchronisation, car SQLite ne fournit pas les fonctions PostGIS natives. La base cible du projet reste PostgreSQL/PostGIS.

## 4. Structure actuelle importante

| Emplacement                         | Responsabilité                                    |
| ----------------------------------- | ------------------------------------------------- |
| `app/models/commune.py`             | Commune, code partagé et géométrie PostGIS        |
| `app/models/pathology.py`           | Catalogue expert des maladies                     |
| `app/models/diagnostic.py`          | Résultats analytiques online et offline           |
| `app/models/pathology_candidate.py` | Maladies proposées par l’IA mais non validées     |
| `app/models/territorial_alert.py`   | Alertes agrégées conservées                       |
| `app/routers/scan.py`               | Synchronisation et consultation des scans mobiles |
| `app/routers/diagnostic.py`         | Diagnostic image en ligne                         |
| `app/routers/pathology.py`          | CRUD du catalogue de pathologies                  |
| `app/routers/alerts.py`             | Consultation et création des alertes              |
| `app/services/gemini.py`            | Appel REST structuré vers Gemini                  |
| `app/services/cloudinary.py`        | Stockage permanent des images                     |
| `app/services/diagnostic.py`        | Correspondance IA/catalogue et candidates         |
| `app/crud/diagnostic.py`            | Persistance des diagnostics et du point GPS       |
| `alembic/versions/2b3c4d5e6f7a...`  | Migration spatiale et tables analytiques          |

## 5. Ce qui reste à faire

### 5.1 Peupler le catalogue expert

Le catalogue doit être rempli avec les 22 pathologies couvrant les 6 cultures du projet :

```text
tomate
maïs
manioc
plantain
pomme de terre
haricot
```

Les données doivent être validées par les experts avant utilisation comme conseils agricoles. Le script de peuplement devra utiliser des codes stables et normalisés, par exemple :

```text
MAIS_ROUILLE
TOMATE_MILDIOU
MANIOC_MOSAIQUE
```

Cette étape est prioritaire, car Gemini ne peut exploiter correctement le mécanisme de correspondance que si le catalogue contient les pathologies officielles.

### 5.2 Brancher les scans offline sur `diagnostics`

La route `POST /api/scans` enregistre actuellement le scan dans la table historique des scans mobiles. Elle doit encore créer le diagnostic analytique correspondant dans `diagnostics`.

Le flux cible est :

```text
Réception du scan offline
    ↓
Vérification idempotente
    ↓
Résolution de commune_code
    ↓
Création du scan mobile
    ↓
Création du diagnostic analytique
    ↓
Création de position_gps si les coordonnées existent
    ↓
Analyse pour les alertes
```

La création du scan et du diagnostic doit idéalement s’effectuer dans une même transaction. Un échec doit empêcher une situation où le scan existe mais où le diagnostic analytique manque.

Le champ `image_url` doit rester nullable pour les scans offline qui ne transfèrent pas leur image.

### 5.3 Ajouter l’idempotence sur `diagnostics`

La table `diagnostics` doit recevoir une contrainte permettant d’éviter les doublons lors de la synchronisation offline.

La contrainte recommandée est :

```text
user_id + local_id + hors_ligne
```

Une autre solution consiste à conserver un lien vers l’identifiant du scan mobile déjà synchronisé. Cette décision doit être prise avant de connecter définitivement la route offline à `diagnostics`.

### 5.4 Résoudre et valider les communes par PostGIS

La résolution actuelle se fait par `commune_code`. Il reste à ajouter une politique géographique complète :

1. si `commune_code` et GPS sont cohérents, accepter le diagnostic ;
2. si le point GPS est hors de la géométrie de la commune déclarée, retourner une incohérence ou appliquer une politique définie ;
3. si seul le GPS est disponible, déterminer la commune avec `ST_Contains` ou `ST_Within` ;
4. si aucun code ni GPS n’est disponible, conserver le diagnostic avec `commune_id` nul ou le refuser selon la règle métier.

La requête centrale devra respecter l’ordre PostGIS :

```sql
ST_MakePoint(longitude, latitude)
```

et non l’inverse.

### 5.5 Finaliser l’enregistrement des images

Le flux online stocke déjà l’URL Cloudinary. Il reste à décider si les images offline doivent être uploadées :

- lors de la synchronisation si le mobile envoie effectivement une photo ;
- ou uniquement dans le flux online où l’image est obligatoire.

Si l’application mobile transmet une image offline, il faudra ajouter un champ fichier au payload ou un endpoint séparé de synchronisation multipart.

### 5.6 Automatiser la création des alertes

La génération automatique doit respecter la règle métier annoncée :

```text
au moins 3 diagnostics concordants
même pathologie
même commune
sur une période de 7 jours
```

La tâche devra :

1. rechercher les diagnostics récents ;
2. regrouper par `commune_id` et `pathology_id` ;
3. compter les cas ;
4. ignorer les diagnostics invalides ou mal localisés ;
5. décider si les diagnostics `AI_ONLY` sont exclus des alertes officielles ;
6. créer ou mettre à jour l’alerte ;
7. éviter de créer plusieurs alertes actives pour le même couple commune/pathologie.

La recommandation actuelle est de ne produire des alertes officielles qu’à partir des diagnostics `CATALOG_MATCH`. Les résultats `AI_ONLY` peuvent être conservés comme signaux provisoires pour un futur dashboard.

### 5.7 Enrichir le cycle de vie des alertes

Le modèle `alerts` doit être enrichi avant l’automatisation complète. Les champs recommandés sont :

| Champ          | Utilité                                |
| -------------- | -------------------------------------- |
| `period_start` | Début de la fenêtre d’analyse          |
| `period_end`   | Fin de la fenêtre d’analyse            |
| `last_scan_at` | Date du dernier diagnostic contributif |
| `status`       | `ACTIVE`, `RESOLVED` ou `EXPIRED`      |
| `resolved_at`  | Date de résolution                     |

Une contrainte logique ou un mécanisme applicatif devra garantir une seule alerte active pour une combinaison donnée de commune et pathologie.

### 5.8 Ajouter les endpoints de consultation des diagnostics

La table `diagnostics` existe, mais il manque encore des routes dédiées pour l’exploiter.

Les routes recommandées sont :

```text
GET /api/diagnostics/{diagnostic_id}
GET /api/diagnostics/me
GET /api/diagnostics?commune_code=...
GET /api/diagnostics?pathology_code=...
```

Les consultations régionales ne sont pas prioritaires aujourd’hui. Elles devront être protégées par des règles d’accès appropriées lorsqu’elles seront ajoutées.

### 5.9 Ajouter la gestion des candidates

La table `pathology_candidates` existe et reçoit les propositions inconnues. Il reste à ajouter les routes de revue :

```text
GET  /api/pathology-candidates
GET  /api/pathology-candidates/{id}
PATCH /api/pathology-candidates/{id}/approve
PATCH /api/pathology-candidates/{id}/reject
```

L’approbation ne doit pas créer automatiquement une pathologie experte sans validation des champs et des traitements. Le workflow recommandé est :

```text
Candidate IA
    ↓
Revue humaine
    ↓
Correction des informations
    ↓
Création manuelle dans pathologies
    ↓
Candidate marquée APPROVED ou MERGED
```

### 5.10 Ajouter le rôle administrateur

Pour la phase actuelle, tout utilisateur authentifié peut gérer le catalogue comme demandé. Cette politique n’est pas acceptable pour la production, car un utilisateur ordinaire peut actuellement ajouter ou modifier des traitements experts.

Il faudra ensuite :

1. ajouter un champ de rôle ou une permission dans `users` ;
2. inclure le rôle dans le JWT ;
3. créer une dépendance `require_admin` ;
4. protéger les opérations d’écriture du catalogue ;
5. protéger la revue des candidates ;
6. protéger la création manuelle des alertes.

Les routes de lecture peuvent rester accessibles aux utilisateurs authentifiés ou être rendues publiques selon les besoins de l’application mobile.

### 5.11 Compléter les tests automatisés

La couverture actuelle est insuffisante pour une mise en production. Il faut ajouter des tests pour :

- création d’une pathologie ;
- refus d’un code pathologie dupliqué ;
- modification et désactivation d’une pathologie ;
- filtrage par culture et statut actif ;
- correspondance Gemini avec le catalogue ;
- priorité des traitements experts ;
- création et regroupement d’une candidate IA ;
- enregistrement d’un diagnostic avec et sans image ;
- génération du point PostGIS ;
- résolution d’une commune par code ;
- détection d’un point GPS hors commune ;
- idempotence des diagnostics offline ;
- création et mise à jour d’une alerte ;
- exclusion ou traitement séparé des diagnostics `AI_ONLY`.

Les tests nécessitant des fonctions spatiales réelles doivent utiliser une base PostgreSQL avec PostGIS, et non SQLite.

### 5.12 Configurer les environnements

Les variables suivantes doivent être présentes dans l’environnement de déploiement :

```env
DATABASE_URL=postgresql+psycopg://...
JWT_SECRET_KEY=...
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=60
GEMINI_API_KEY=...
GEMINI_MODEL=gemini-3.6-flash
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=mboa_agri/diagnostics
```

Les secrets ne doivent jamais être commités dans Git ou inclus dans les captures Postman.

### 5.13 Déployer le backend

Le backend doit encore être déployé sur Railway, Render ou une plateforme équivalente avec :

- une URL HTTPS stable ;
- une base PostgreSQL avec PostGIS ;
- les variables d’environnement configurées ;
- l’exécution des migrations Alembic ;
- un endpoint `/health` accessible ;
- des limites adaptées aux uploads d’images ;
- des logs consultables ;
- une politique de sauvegarde de la base.

### 5.14 Tester l’environnement de production

Avant de transmettre l’URL à l’équipe mobile, il faut tester au minimum :

```text
POST /auth/register
POST /auth/login
GET /health
POST /api/pathologies
GET /api/pathologies
POST /api/scans/diagnose
POST /api/scans
GET /api/scans
GET /alerts
```

Le test du diagnostic online doit vérifier simultanément :

- la réponse Gemini ;
- l’URL Cloudinary ;
- l’enregistrement PostgreSQL ;
- la commune ;
- la gravité ;
- la source des traitements ;
- le statut de validation.

## 6. Ordre de réalisation recommandé

| Priorité | Étape                                             | Résultat attendu                             |
| -------: | ------------------------------------------------- | -------------------------------------------- |
|        1 | Insérer les 22 pathologies expertes               | Catalogue utilisable par Gemini              |
|        2 | Connecter `POST /api/scans` à `diagnostics`       | Même table analytique pour online et offline |
|        3 | Ajouter l’idempotence de `diagnostics`            | Absence de doublons offline                  |
|        4 | Ajouter la validation géographique PostGIS        | Commune et GPS cohérents                     |
|        5 | Enrichir `alerts`                                 | Cycle de vie et historique fiables           |
|        6 | Automatiser le regroupement sur 7 jours           | Création des alertes à partir de 3 cas       |
|        7 | Ajouter la revue des candidates                   | Validation humaine des maladies inconnues    |
|        8 | Ajouter les endpoints de consultation diagnostics | Exploitation et dashboard possibles          |
|        9 | Ajouter le rôle administrateur                    | Sécurisation des écritures expertes          |
|       10 | Étendre les tests d’intégration                   | Réduction des régressions                    |
|       11 | Déployer sur HTTPS                                | Backend accessible par Android               |
|       12 | Tester la production avec Postman                 | Validation avant remise à l’équipe mobile    |

## 7. Critères de complétude de l’application

L’application backend pourra être considérée comme complète pour le périmètre initial lorsque les conditions suivantes seront réunies :

1. Les 22 pathologies expertes sont insérées avec des codes stables.
2. Les communes utilisées par Android disposent de codes partagés et de géométries valides.
3. Un diagnostic online avec image est analysé par Gemini, stocké sur Cloudinary et enregistré dans `diagnostics`.
4. Un diagnostic offline est synchronisé sans doublon et enregistré dans `diagnostics`.
5. Les traitements expert sont prioritaires sur les textes IA lorsqu’un code catalogue existe.
6. Les pathologies inconnues sont marquées comme provisoires et regroupées dans `pathology_candidates`.
7. Les diagnostics concordants déclenchent une alerte selon la règle des 3 cas sur 7 jours.
8. Les alertes ont un statut et une période d’analyse exploitables.
9. Les règles d’accès distinguent les utilisateurs ordinaires et les administrateurs.
10. Les tests d’intégration PostgreSQL/PostGIS, Gemini et Cloudinary passent.
11. Le backend est déployé avec HTTPS et ses secrets sont configurés hors du code.
12. L’équipe mobile dispose de l’URL de production, des contrats JSON et des règles de synchronisation documentées.

## 8. Conclusion

Le projet dispose maintenant d’une base cohérente pour relier l’application mobile, Gemini, Cloudinary, PostgreSQL et PostGIS. Les décisions structurantes sont prises : les identifiants Room restent locaux, `commune_code` assure la cohérence inter-systèmes, `position_gps` représente le point d’un diagnostic, `communes.geom` représente les limites administratives et `diagnostics` sert de table analytique commune.

La prochaine étape la plus importante est le **peuplement fiable du catalogue des 22 pathologies**, suivi immédiatement par la connexion des scans offline à `diagnostics`. Ces deux étapes permettront ensuite d’implémenter les alertes automatiques sur une base de données complète et cohérente.

## Références

[1]: https://fastapi.tiangolo.com/ "FastAPI Documentation"
[2]: https://alembic.sqlalchemy.org/ "Alembic Documentation"
[3]: https://postgis.net/docs/ "PostGIS Documentation"
[4]: https://cloudinary.com/documentation/upload_images "Cloudinary Image Upload Documentation"
[5]: https://ai.google.dev/gemini-api/docs "Google Gemini API Documentation"
