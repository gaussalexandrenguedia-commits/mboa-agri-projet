# Déploiement gratuit Mboa Agri (bêta)

Le dépôt contient `render.yaml` pour créer un Web Service FastAPI sur le plan gratuit Render. Il configure le dossier `backend`, l'installation des dépendances, la migration Alembic au démarrage et Uvicorn sur le port fourni par Render.

## Mise en ligne

1. Dans Render, créer un Blueprint depuis le dépôt GitHub et sélectionner la branche `arena/8dd95ad5-mboa-agri-projet`.
2. Vérifier que le service `mboa-agri-api` est bien sur `Free` et ne pas ajouter de service ou ressource payante.
3. Saisir les variables secrètes demandées au premier déploiement :
   - `DATABASE_URL`: URL Postgres de **MBOA AGRI** au format `postgresql+psycopg://...`.
   - `SUPABASE_SERVICE_ROLE_KEY`: clé serveur Supabase uniquement. Ne jamais la mettre dans l'APK ni dans Git.
   - `GEMINI_API_KEY`: clé Gemini côté serveur.
4. Laisser `SUPABASE_URL` sur l'URL du projet MBOA AGRI et le bucket privé `scan-images`.
5. Vérifier `/health` après le déploiement. L'URL Render est le `BACKEND_BASE_URL` à mettre dans la configuration de build Android, avec un slash final.

## Limites du zéro-budget

Le plan gratuit Render met le service en veille après une période sans requêtes ; le premier appel suivant peut attendre environ une minute. C'est convenable pour une bêta/démonstration, pas pour une disponibilité garantie. Le service gratuit peut redémarrer et ne doit pas stocker de fichiers sur son disque local. Les images restent dans le bucket Supabase privé.

Le compte Supabase gratuit a ses propres limites d'usage et de mise en veille. Surveille les quotas dans les tableaux de bord. Ne crée pas de ressources payantes et ne saisis pas de carte bancaire pour cette configuration zéro-budget.

Les migrations se lancent dans la commande de démarrage car les commandes pre-deploy Render nécessitent un plan payant. Si les migrations échouent, FastAPI ne démarre pas et l'erreur doit être corrigée avant de republier.
