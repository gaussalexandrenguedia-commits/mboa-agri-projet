# Documentation technique — Lot 1 : préparation Android pour le backend

**Projet :** Mboa Agri  
**Version documentée :** commit `de36bf9`  
**Auteur :** Manus AI

## 1. Objectif

Le Lot 1 prépare l’application Android à communiquer avec le backend de Martial tout en conservant une expérience utilisable hors connexion. Le principe retenu est local-first : l’application enregistre d’abord les données dans Room, puis les transmet au serveur lorsqu’une connexion réseau est disponible.

Cette approche sépare les responsabilités. Android gère l’interface, la capture et la persistance locale; le backend conserve les secrets, expose l’API et prend en charge les appels d’intelligence artificielle.

## 2. Résumé des changements

| Zone | Fichier(s) | Changement |
|---|---|---|
| Sécurité | `GeminiApiClient.kt`, `app/build.gradle.kts` | Suppression de la lecture de `BuildConfig.GEMINI_API_KEY`; l’IA passe par le backend. |
| Réseau | `api/ApiClient.kt` | Création d’un client Retrofit avec conversion Moshi et timeouts réseau. |
| Données | `data/Entities.kt`, `data/Converters.kt` | Ajout de `SyncStatus` et des champs de profil. |
| Base locale | `data/AppDatabase.kt` | Passage du schéma Room de la version 3 à la version 4 avec migration. |
| Synchronisation | `sync/ScanSyncWorker.kt` | Envoi automatique des scans PENDING/FAILED sous contrainte de réseau connecté. |
| Intégration UI | `ui/MainViewModel.kt` | Chaque nouveau scan est enregistré localement puis placé dans la file de synchronisation. |
| Permissions | `AndroidManifest.xml` | Ajout de `ACCESS_NETWORK_STATE`. |
| Tests | `SyncStatusTest.kt` | Vérification des transitions PENDING, FAILED et SYNCED. |

## 3. Suppression de la clé Gemini de l’APK

L’ancien client lisait directement `BuildConfig.GEMINI_API_KEY`, ce qui pouvait exposer une clé dans l’application compilée. Cette référence a été supprimée. `GeminiApiClient.kt` conserve les modèles de requête et de réponse, mais transmet maintenant la requête à `ApiClient.service.generate()`.

Le backend doit donc recevoir la requête sur `POST /api/ai/generate`, conserver ses propres secrets côté serveur et appeler le fournisseur d’IA depuis son environnement sécurisé.

> Règle de sécurité : aucune clé Gemini ne doit être placée dans le code Android, dans `BuildConfig`, dans `.env.example` ou dans un commit Git.

## 4. Client Retrofit

Le fichier [`ApiClient.kt`](../app/src/main/java/com/example/api/ApiClient.kt) définit trois opérations :

| Méthode Kotlin | Route | Données envoyées |
|---|---|---|
| `uploadScan` | `POST /api/scans` | Résultat de scan, coordonnées, date et identifiant local. |
| `uploadProfile` | `POST /api/profiles` | Commune, cultures, langue et consentement aux alertes. |
| `generate` | `POST /api/ai/generate` | Contenu destiné à l’assistant agronomique. |

L’URL racine est fournie par `BuildConfig.BACKEND_BASE_URL`. Cette valeur n’est pas un secret. Elle est définie par la variable d’environnement `BACKEND_BASE_URL`, avec `http://10.0.2.2:8000/` comme valeur de développement par défaut.

Pour utiliser le backend de production :

```bash
export BACKEND_BASE_URL="https://backend-de-martial.example/"
```

L’URL doit se terminer par `/`, conformément aux exigences de construction d’URL de Retrofit.

## 5. Modèle de synchronisation Room

L’énumération suivante décrit le cycle de vie d’un scan :

```kotlin
enum class SyncStatus { PENDING, SYNCED, FAILED }
```

Un scan nouvellement créé reçoit `PENDING`. Le Worker tente ensuite de l’envoyer. Le serveur répondant correctement, le statut devient `SYNCED`. Une erreur réseau ou serveur produit `FAILED`; le scan reste conservé localement et pourra être rejoué.

Le convertisseur Room de [`Converters.kt`](../app/src/main/java/com/example/data/Converters.kt) stocke le statut sous forme de texte. Cette représentation rend les valeurs lisibles dans SQLite et garantit la compatibilité avec la migration.

## 6. Migration de la base de données

La version Room passe de 3 à 4. La migration ajoute les colonnes suivantes :

| Table | Colonne | Valeur par défaut |
|---|---|---|
| `scan_results` | `syncStatus` | `PENDING` |
| `users` | `commune` | chaîne vide |
| `users` | `cultures` | chaîne vide |
| `users` | `langue` | `fr` |
| `users` | `consentementAlertes` | `false` |

La migration est non destructive pour les installations déjà existantes. Les anciennes lignes de scans deviennent automatiquement `PENDING`, ce qui permet leur envoi au backend après la mise à jour.

## 7. Fonctionnement de WorkManager

La fonction `enqueueScanSync()` crée un travail unique nommé `mboa-agri-scan-sync`. Le travail possède une contrainte `NetworkType.CONNECTED`; il n’est donc pas exécuté tant qu’Android ne détecte pas de connexion.

Le flux est le suivant :

```text
Création du scan
       |
       v
Room : PENDING
       |
       v
WorkManager attend le réseau
       |
       v
POST /api/scans
   |             |
 succès       erreur
   |             |
   v             v
SYNCED        FAILED
                 |
                 v
        nouvelle tentative avec backoff
```

`ExistingWorkPolicy.KEEP` évite de créer plusieurs travaux identiques lorsque plusieurs scans sont enregistrés rapidement. Le backoff exponentiel limite les requêtes répétées en cas d’indisponibilité persistante du serveur.

## 8. Profil utilisateur et alertes territoriales

`UserEntity` contient désormais les informations nécessaires au ciblage territorial :

```text
commune                  Commune de résidence ou d’exploitation
cultures                 Cultures suivies par l’utilisateur
langue                   Langue préférée, par défaut fr
consentementAlertes      Autorisation de recevoir des alertes territoriales
```

`MainViewModel.updateUserProfile()` sauvegarde ces informations localement et tente de les transmettre à `POST /api/profiles`. En cas d’échec, le profil local reste disponible. Une prochaine itération pourra lui ajouter son propre statut de synchronisation si le backend exige une garantie de livraison équivalente à celle des scans.

## 9. Validation du cycle hors connexion

Le test [`SyncStatusTest.kt`](../app/src/test/java/com/example/SyncStatusTest.kt) vérifie les transitions métier essentielles :

1. Un nouvel enregistrement commence en `PENDING`.
2. Un échec peut être représenté par `FAILED`.
3. Une synchronisation réussie aboutit à `SYNCED`.

La validation instrumentée complète doit être réalisée dans Android Studio ou dans une machine disposant du SDK Android et de Gradle. Le dépôt ne contient pas encore le fichier `gradlew`, et l’environnement de préparation ne disposait pas de Gradle ni du SDK Android; l’assemblage réel de l’APK n’a donc pas été exécuté dans cette session.

Scénario manuel recommandé :

| Étape | Action | Résultat attendu |
|---:|---|---|
| 1 | Désactiver le réseau de l’émulateur. | Aucun appel backend ne passe. |
| 2 | Créer un diagnostic. | Le scan est visible localement avec `PENDING`. |
| 3 | Réactiver le réseau. | WorkManager débloque le travail. |
| 4 | Vérifier le backend et Room. | Le scan est reçu puis marqué `SYNCED`. |
| 5 | Arrêter temporairement le backend. | Le scan passe `FAILED`, puis est rejoué ultérieurement. |

## 10. Mise en route pour l’équipe

Après clonage, ouvrir le projet dans Android Studio et vérifier que le SDK correspondant au `compileSdk` est installé. Configurer l’URL du backend, synchroniser Gradle, puis lancer les tests et l’assemblage debug :

```bash
export BACKEND_BASE_URL="https://backend-de-martial.example/"
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Pour un backend local sur la machine hôte de l’émulateur, conserver `http://10.0.2.2:8000/`. Sur un appareil physique, remplacer cette adresse par l’adresse IP accessible de la machine ou par une URL HTTPS.

## 11. Points d’attention backend

Le backend doit accepter les noms JSON indiqués par les annotations Moshi, notamment `local_id`, `plant_name`, `disease_name`, `treatment_local`, `treatment_chemical` et `consentement_alertes`. Il doit retourner un code HTTP 2xx après persistance réussie. Tout code d’erreur ou toute exception réseau doit être considéré comme un échec temporaire par le Worker.

Le contrat exact devra être aligné avec l’implémentation FastAPI de Martial avant la mise en production. Si les routes ou les noms de champs diffèrent, il suffira d’adapter les annotations et les chemins dans [`ApiClient.kt`](../app/src/main/java/com/example/api/ApiClient.kt).

## Références du dépôt

- [`README.md`](../README.md) — présentation et démarrage rapide.
- [`ApiClient.kt`](../app/src/main/java/com/example/api/ApiClient.kt) — service Retrofit et payloads.
- [`GeminiApiClient.kt`](../app/src/main/java/com/example/api/GeminiApiClient.kt) — modèles IA et appel backend.
- [`Entities.kt`](../app/src/main/java/com/example/data/Entities.kt) — entités Room et statuts.
- [`AppDatabase.kt`](../app/src/main/java/com/example/data/AppDatabase.kt) — version et migration Room.
- [`ScanSyncWorker.kt`](../app/src/main/java/com/example/sync/ScanSyncWorker.kt) — traitement de synchronisation.
- [`SyncStatusTest.kt`](../app/src/test/java/com/example/SyncStatusTest.kt) — tests du cycle de statut.
