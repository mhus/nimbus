-- Database initialization script for Nimbus MMORPG System
-- This script creates separate databases for different components

-- Create databases for different services
CREATE DATABASE identity;
CREATE DATABASE registry;
CREATE DATABASE world_data;
CREATE DATABASE world_life;

-- Grant permissions to the nimbus user
GRANT ALL PRIVILEGES ON DATABASE identity TO nimbus;
GRANT ALL PRIVILEGES ON DATABASE registry TO nimbus;
GRANT ALL PRIVILEGES ON DATABASE world_data TO nimbus;
GRANT ALL PRIVILEGES ON DATABASE world_life TO nimbus;

-- Connect to each database and ensure the nimbus user can create schemas
\c identity;
GRANT CREATE ON SCHEMA public TO nimbus;
ALTER SCHEMA public OWNER TO nimbus;

\c registry;
GRANT CREATE ON SCHEMA public TO nimbus;
ALTER SCHEMA public OWNER TO nimbus;

\c world_data;
GRANT CREATE ON SCHEMA public TO nimbus;
ALTER SCHEMA public OWNER TO nimbus;

\c world_life;
GRANT CREATE ON SCHEMA public TO nimbus;
ALTER SCHEMA public OWNER TO nimbus;
