# Documentation UX/UI Design - MBOA AGRI (v2.1)

> **Projet :** MBOA AGRI (AgroScanEdu AI v2.1)  
> **Équipe :** UBUNTU TECH (Team 59) — ENSPD Douala  
> **Auteur :** JULTIENNE FERNANDE BIDJECK (UX/UI Designer)  
> **Période :** Semaine du 31 août au 5 septembre 2026  
> **Livrable :** Lot 1 partiel - Spécifications UX/UI, Kit de composants et Maquettes Figma  

---

## 1. Vision & Principes UX Directeurs

Conformément au rapport d'orientation technique et à la vision révisée d'UBUNTU TECH, la refonte UX/UI de **MBOA AGRI** repose sur trois piliers fondamentaux :

1. **Visuel d'abord (Visual-First) :** Réduction maximale de la densité textuelle au profit de pictogrammes universels et d'éléments visuels explicites pour être directement assimilable par les agriculteurs ruraux.
2. **Accessibilité Rurale Radicalement Simple :** Ergonomie conçue pour être utilisable en conditions réelles de terrain (forte luminosité, manipulation à une main, faible littératie numérique).
3. **Explicabilité & Système Tri-Couleur (Code Trafic) :**
   *  **VERT (Sain / Normal) :** Culture saine, absence de menace territoriale ou cas isolé non critique.
   *  **ORANGE (Attention / À confirmer) :** Symptômes modérés ou alerte épidémique locale en attente de confirmation.
   *  **ROUGE (Urgent / Danger) :** Maladie sévère identifiée ou épidémie territoriale confirmée nécessitant un traitement immédiat.

---

## 📱 2. Description des Maquettes & Écrans Réalisés

### 1. Écran "Alerte de Zone Territorial" (Mobile)
* **Rôle :** Avertir immédiatement l'agriculteur de tout risque d'infection active dans sa commune (ex: Foumbot) dès l'ouverture de l'application.
* **Éléments de l'interface :**
  * **Header dynamique :** Bannière d'alerte à fort contraste (Rouge `#C62828` / Orange `#EF6C00`).
  * **Indicateur de Confiance :** Puce explicite `FIABLE` (basée sur la concordance de multiples cas) ou `À CONFIRMER`.
  * **Plan d'Action Immédiat :** Cartes étapes des gestes préventifs et traitements autorisés avec dosages prudents.
  * **Boucle de Rétroaction (Feedback Loop) :** Bouton d'action *"J'ai appliqué le traitement"* pour suivre le taux de couverture sur le terrain.

### 2. Guide Visuel "Comment réussir son Scan" (Mobile - CameraX)
* **Rôle :** Prévenir les erreurs de cadrage et optimiser la qualité d'acquisition pour les analyses de l'IA (Online/Offline).
* **Éléments de l'interface :**
  * **Masque de Cadrage :** Overlay indicatif au centre de l'objectif de la caméra.
  * **Grille comparative "Bon / Mauvais Scan" :**
    *  **Photo correcte :** Feuille nette, bien éclairée, occupant ~70% du cadre (contour vert).
    *  **Photo incorrecte :** Image floue, vue trop éloignée, ombre forte ou contre-jour (croix rouge).
  * **Consignes :** Textes d'accompagnement très courts associés à des icônes explicites.

### 3. Tableau de Bord Territorial (Web / Desktop)
* **Rôle :** Fournir une vision synthétique et décisionnelle aux coopératives agricoles et aux délégués (MINADER, IRAD).
* **Éléments de l'interface :**
  * **Barre d'indicateurs (KPIs) :** Scans synchronisés, nombre d'alertes actives, zones couvertes et taux d'activité hors-ligne.
  * **Carte Cartographique (Leaflet) :** Cartographie en cluster des foyers d'infection géolocalisés avec filtres par gravité.
  * **Panneau Analytique :** Graphiques de répartition par spéculation agricole (Tomate, Manioc, Maïs) et tendances temporelles.

---

##  3. Simplifications d'Interface & Prise en Charge du Mode Hors-Ligne

* **Restructuration du Diagnostic :** Remplacement des pavés de texte bruts par une fiche visuelle synthétique incluant le badge de gravité et l'origine de la prédiction (*Analyse Serveur* ou *Analyse Hors-Ligne*).
* **Puces de Synchronisation (WorkManager & Room) :**
  *  **PENDING :** Scan enregistré localement sur l'appareil, en attente d'une connexion Internet.
  *  **SYNCED :** Données consolidées et partagées avec le réseau d'alerte territorial.

---

##  4. Liens & Ressources Figma

* **Lien de la maquette Figma interactive :** `[https://www.figma.com/proto/ls3qg4oPLrFDhHoI0z0Ilf/Untitled?node-id=0-1&t=TTwgqbWFcafnfuPM-1]`

---
*Document rédigé dans le cadre des livrables de la Semaine 8 pour le projet MBOA AGRI.*