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

> **État actuel :** l’authentification, la synchronisation des scans, la gestion des communes et les routes de consultation et de création des alertes sont en place. L’intégration Gemini et l’automatisation métier des alertes restent à finaliser.

## Références techniques

[1]: https://fastapi.tiangolo.com/ "FastAPI Documentation"
[2]: https://docs.sqlalchemy.org/en/20/ "SQLAlchemy 2.0 Documentation"
[3]: https://alembic.sqlalchemy.org/en/latest/ "Alembic Documentation"
[4]: https://postgis.net/documentation/ "PostGIS Documentation"
