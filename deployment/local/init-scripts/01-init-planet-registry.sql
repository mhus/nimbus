-- Initialisierungsskript für Planet-Registry-Testdaten
-- Wird automatisch von PostgreSQL beim ersten Start ausgeführt

-- Erstelle Testplaneten
INSERT INTO planets (name, environment, description, galaxy, sector, system_name, population, climate, terrain, surface_water, gravity, active, created_at, updated_at) VALUES
('Tatooine', 'DEV', 'Desert planet with twin suns', 'Far Far Away', 'Arkanis', 'Tatoo', 200000, 'Arid', 'Desert', 1, '1 standard', true, NOW(), NOW()),
('Coruscant', 'DEV', 'Galactic capital planet covered in cityscape', 'Far Far Away', 'Core Worlds', 'Coruscant', 1000000000000, 'Temperate', 'Cityscape', 0, '1 standard', true, NOW(), NOW()),
('Hoth', 'DEV', 'Ice planet in outer rim', 'Far Far Away', 'Outer Rim', 'Hoth', 0, 'Frozen', 'Tundra', 100, '1.1 standard', true, NOW(), NOW()),
('Dagobah', 'DEV', 'Swamp planet strong with the Force', 'Far Far Away', 'Outer Rim', 'Dagobah', 0, 'Murky', 'Swamp', 8, '0.9 standard', true, NOW(), NOW()),
('Naboo', 'DEV', 'Peaceful planet with diverse biomes', 'Far Far Away', 'Mid Rim', 'Naboo', 4500000000, 'Temperate', 'Grassy hills', 12, '1 standard', true, NOW(), NOW());

-- Erstelle Testwelten für Tatooine
INSERT INTO worlds (world_id, name, planet_id, management_url, api_url, web_url, status, description, world_type, access_level, last_health_check, created_at, updated_at) VALUES
('tatooine-main', 'Main World', (SELECT id FROM planets WHERE name = 'Tatooine' AND environment = 'DEV'), 'https://tatooine-mgmt.nimbus.local:8443', 'https://tatooine-api.nimbus.local:8080', 'https://tatooine.nimbus.local', 'ACTIVE', 'Main settlement on Tatooine', 'settlement', 'public', NOW(), NOW(), NOW()),
('tatooine-cantina', 'Mos Eisley Cantina', (SELECT id FROM planets WHERE name = 'Tatooine' AND environment = 'DEV'), 'https://cantina-mgmt.nimbus.local:8443', 'https://cantina-api.nimbus.local:8080', 'https://cantina.tatooine.nimbus.local', 'ACTIVE', 'Famous cantina in Mos Eisley spaceport', 'cantina', 'public', NOW(), NOW(), NOW()),
('tatooine-lars', 'Lars Homestead', (SELECT id FROM planets WHERE name = 'Tatooine' AND environment = 'DEV'), 'https://lars-mgmt.nimbus.local:8443', 'https://lars-api.nimbus.local:8080', 'https://lars.tatooine.nimbus.local', 'ACTIVE', 'Moisture farm in the desert', 'homestead', 'private', NOW(), NOW(), NOW());

-- Erstelle Testwelten für Coruscant
INSERT INTO worlds (world_id, name, planet_id, management_url, api_url, web_url, status, description, world_type, access_level, last_health_check, created_at, updated_at) VALUES
('coruscant-temple', 'Jedi Temple', (SELECT id FROM planets WHERE name = 'Coruscant' AND environment = 'DEV'), 'https://temple-mgmt.nimbus.local:8443', 'https://temple-api.nimbus.local:8080', 'https://temple.coruscant.nimbus.local', 'MAINTENANCE', 'Sacred Jedi Temple on Coruscant', 'temple', 'restricted', NOW(), NOW(), NOW()),
('coruscant-senate', 'Galactic Senate', (SELECT id FROM planets WHERE name = 'Coruscant' AND environment = 'DEV'), 'https://senate-mgmt.nimbus.local:8443', 'https://senate-api.nimbus.local:8080', 'https://senate.coruscant.nimbus.local', 'ACTIVE', 'Seat of galactic government', 'government', 'official', NOW(), NOW(), NOW()),
('coruscant-underworld', 'Coruscant Underworld', (SELECT id FROM planets WHERE name = 'Coruscant' AND environment = 'DEV'), 'https://underworld-mgmt.nimbus.local:8443', 'https://underworld-api.nimbus.local:8080', 'https://underworld.coruscant.nimbus.local', 'ACTIVE', 'Lower levels of Coruscant', 'underworld', 'dangerous', NOW(), NOW(), NOW());

-- Erstelle Testwelten für Hoth
INSERT INTO worlds (world_id, name, planet_id, management_url, api_url, web_url, status, description, world_type, access_level, last_health_check, created_at, updated_at) VALUES
('hoth-echo-base', 'Echo Base', (SELECT id FROM planets WHERE name = 'Hoth' AND environment = 'DEV'), 'https://echo-mgmt.nimbus.local:8443', 'https://echo-api.nimbus.local:8080', 'https://echo.hoth.nimbus.local', 'INACTIVE', 'Abandoned Rebel Alliance base', 'military_base', 'abandoned', NOW(), NOW(), NOW());

-- Erstelle Testwelten für Dagobah
INSERT INTO worlds (world_id, name, planet_id, management_url, api_url, web_url, status, description, world_type, access_level, last_health_check, created_at, updated_at) VALUES
('dagobah-yoda-hut', 'Yoda''s Hut', (SELECT id FROM planets WHERE name = 'Dagobah' AND environment = 'DEV'), 'https://yoda-mgmt.nimbus.local:8443', 'https://yoda-api.nimbus.local:8080', 'https://yoda.dagobah.nimbus.local', 'ACTIVE', 'Jedi Master Yoda''s dwelling', 'hermitage', 'force_sensitive', NOW(), NOW(), NOW());

-- Erstelle Testwelten für Naboo
INSERT INTO worlds (world_id, name, planet_id, management_url, api_url, web_url, status, description, world_type, access_level, last_health_check, created_at, updated_at) VALUES
('naboo-theed', 'Theed Palace', (SELECT id FROM planets WHERE name = 'Naboo' AND environment = 'DEV'), 'https://theed-mgmt.nimbus.local:8443', 'https://theed-api.nimbus.local:8080', 'https://theed.naboo.nimbus.local', 'ACTIVE', 'Royal palace and capital of Naboo', 'palace', 'royal', NOW(), NOW(), NOW()),
('naboo-gungan', 'Gungan City', (SELECT id FROM planets WHERE name = 'Naboo' AND environment = 'DEV'), 'https://gungan-mgmt.nimbus.local:8443', 'https://gungan-api.nimbus.local:8080', 'https://gungan.naboo.nimbus.local', 'ACTIVE', 'Underwater Gungan settlement', 'underwater_city', 'species_specific', NOW(), NOW(), NOW());

-- Erstelle Metadaten für die Welten
INSERT INTO world_metadata (world_id, metadata_key, metadata_value) VALUES
((SELECT id FROM worlds WHERE world_id = 'tatooine-main'), 'biome', 'desert'),
((SELECT id FROM worlds WHERE world_id = 'tatooine-main'), 'moons', '2'),
((SELECT id FROM worlds WHERE world_id = 'tatooine-main'), 'suns', '2'),
((SELECT id FROM worlds WHERE world_id = 'tatooine-cantina'), 'species_allowed', 'all'),
((SELECT id FROM worlds WHERE world_id = 'tatooine-cantina'), 'music', 'jizz'),
((SELECT id FROM worlds WHERE world_id = 'tatooine-cantina'), 'no_droids', 'true'),
((SELECT id FROM worlds WHERE world_id = 'coruscant-temple'), 'order', 'jedi'),
((SELECT id FROM worlds WHERE world_id = 'coruscant-temple'), 'force_sensitive_only', 'true'),
((SELECT id FROM worlds WHERE world_id = 'coruscant-senate'), 'government', 'republic'),
((SELECT id FROM worlds WHERE world_id = 'coruscant-senate'), 'senators', '1024'),
((SELECT id FROM worlds WHERE world_id = 'hoth-echo-base'), 'faction', 'rebel_alliance'),
((SELECT id FROM worlds WHERE world_id = 'hoth-echo-base'), 'temperature', '-60C'),
((SELECT id FROM worlds WHERE world_id = 'hoth-echo-base'), 'abandoned', 'true'),
((SELECT id FROM worlds WHERE world_id = 'dagobah-yoda-hut'), 'force_nexus', 'true'),
((SELECT id FROM worlds WHERE world_id = 'dagobah-yoda-hut'), 'master', 'yoda'),
((SELECT id FROM worlds WHERE world_id = 'naboo-theed'), 'government_type', 'monarchy'),
((SELECT id FROM worlds WHERE world_id = 'naboo-gungan'), 'habitat', 'aquatic'),
((SELECT id FROM worlds WHERE world_id = 'naboo-gungan'), 'species', 'gungan');
