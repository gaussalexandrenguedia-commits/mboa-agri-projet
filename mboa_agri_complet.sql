-- 1. PRÉPARATION DU SYSTÈME
-- On crée la base et on active PostGIS pour les cartes (Exigence Lot 3)
CREATE DATABASE mboa_agri;
\c mboa_agri;
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. TABLE DES COMMUNES
-- Pour cibler les alertes par zone géographique
CREATE TABLE communes (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    code_postal VARCHAR(20),
    geom GEOMETRY(MultiPolygon, 4326) -- Format carte standard
);

-- 3. CATALOGUE DES PATHOLOGIES (Exigence Lot 4)
-- Pour que l'IA et le mode hors-ligne utilisent les mêmes noms
CREATE TABLE pathologies (
    id SERIAL PRIMARY KEY,
    nom_technique VARCHAR(100),
    nom_commun VARCHAR(100) NOT NULL,
    culture_concernee VARCHAR(50) NOT NULL,
    symptomes_cles TEXT,
    traitement_bio TEXT,
    traitement_chimique TEXT,
    gravite_defaut VARCHAR(20) DEFAULT 'Attention'
);

-- 4. TABLE DES UTILISATEURS (Exigence Lot 1)
-- Avec le village et la commune pour les alertes ciblées
CREATE TABLE utilisateurs (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20) UNIQUE NOT NULL,
    commune_id INTEGER REFERENCES communes(id),
    village VARCHAR(100),
    date_inscription TIMESTAMP DEFAULT NOW()
);

-- 5. TABLE DES SCANS (Le cœur du projet)
-- On enregistre la position GPS précise et le lien vers la maladie
CREATE TABLE scans_geo (
    id SERIAL PRIMARY KEY,
    utilisateur_id INTEGER REFERENCES utilisateurs(id),
    pathologie_id INTEGER REFERENCES pathologies(id),
    culture VARCHAR(50) NOT NULL,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    position_gps GEOGRAPHY(Point, 4326), -- Pour PostGIS
    hors_ligne BOOLEAN DEFAULT FALSE, -- Savoir si c'est un scan fait sans réseau
    date_scan TIMESTAMP DEFAULT NOW()
);

-- 6. TABLE DES PHOTOS (Exigence Lot 6)
CREATE TABLE scan_photos (
    id SERIAL PRIMARY KEY,
    scan_id INTEGER REFERENCES scans_geo(id) ON DELETE CASCADE,
    url_photo TEXT NOT NULL,
    date_upload TIMESTAMP DEFAULT NOW()
);

-- 7. TABLE DES ALERTES (Exigence Lot 5)
-- Pour stocker les alertes quand 3 cas sont détectés
CREATE TABLE alertes (
    id SERIAL PRIMARY KEY,
    pathologie_id INTEGER REFERENCES pathologies(id),
    commune_id INTEGER REFERENCES communes(id),
    nombre_scans INTEGER NOT NULL,
    niveau_alerte VARCHAR(20) NOT NULL,
    date_alerte TIMESTAMP DEFAULT NOW()
);

-- 8. TABLE FEEDBACK (Exigence Lot 6)
-- Pour savoir si le conseil a sauvé la récolte
CREATE TABLE feedback_traitements (
    id SERIAL PRIMARY KEY,
    scan_id INTEGER REFERENCES scans_geo(id) ON DELETE CASCADE,
    traitement_applique BOOLEAN DEFAULT FALSE,
    amelioration_observée BOOLEAN DEFAULT FALSE,
    commentaire_agriculteur TEXT,
    date_feedback TIMESTAMP DEFAULT NOW()
);

-- ==========================================
-- INSERTION DE TES DONNÉES DE TEST (Tes 10 agriculteurs)
-- ==========================================
INSERT INTO communes (nom) VALUES ('Foumbot'), ('Koutaba'), ('Dschang');

INSERT INTO pathologies (nom_commun, culture_concernee, gravite_defaut) VALUES 
('Mildiou', 'Tomate', 'Urgent'),
('Mosaïque virale', 'Manioc', 'Urgent'),
('Anthracnose', 'Tomate', 'Attention');

INSERT INTO utilisateurs (nom, telephone, commune_id, village) VALUES
('Jean-Pierre Kamga', '+237690001111', 1, 'Foumbot Centre'),
('Marie Tchinda', '+237690002222', 1, 'Foumbot Nord'),
('Paul Ngoumou', '+237690003333', 2, 'Koutaba'),
('Célestine Fomekong', '+237690004444', 1, 'Foumbot Centre'),
('André Wabo', '+237690005555', 1, 'Foumbot Sud'),
('Jeanne Ngassa', '+237690006666', 2, 'Koutaba'),
('Pierre Feudjio', '+237690007777', 1, 'Foumbot Nord'),
('Esther Mbianda', '+237690008888', 1, 'Foumbot Centre'),
('Simon Talla', '+237690009999', 1, 'Foumbot Sud'),
('Bernadette Kouam', '+237690010101', 2, 'Koutaba');
