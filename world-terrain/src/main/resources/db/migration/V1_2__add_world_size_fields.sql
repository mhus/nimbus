-- Add sizeX and sizeY fields to worlds table
ALTER TABLE worlds
ADD COLUMN size_x INTEGER,
ADD COLUMN size_y INTEGER;

-- Add comments for documentation
COMMENT ON COLUMN worlds.size_x IS 'Size of the world in X-direction';
COMMENT ON COLUMN worlds.size_y IS 'Size of the world in Y-direction';
