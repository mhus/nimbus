package de.mhus.nimbus.world.shared.layer;

/**
 * Layer type enumeration.
 *
 * TERRAIN: Chunk-based layers with external storage (like WChunk)
 * MODEL: Entity-based layers with blocks stored in document
 */
public enum LayerType {
    /**
     * Terrain layer - chunk-oriented with external storage.
     * Data stored per chunk in w_layer_terrain collection.
     */
    TERRAIN,

    /**
     * Model layer - entity-oriented with blocks in document.
     * Data stored in w_layer_model collection with relative positions.
     */
    MODEL
}
