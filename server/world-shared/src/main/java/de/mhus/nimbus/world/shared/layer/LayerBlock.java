package de.mhus.nimbus.world.shared.layer;

import de.mhus.nimbus.generated.types.Block;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Layer block wrapper.
 * Contains a block with additional layer-specific properties.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LayerBlock {

    /**
     * The actual block data.
     */
    private Block block;

    /**
     * Layer-specific weight or priority.
     * Can be used for advanced overlay algorithms.
     */
    private Integer weight;

    /**
     * Layer-specific metadata.
     * Can be used for layer-specific behavior.
     */
    private String metadata;

    /**
     * Flag to indicate if this block should override previous layers.
     * If false, this block might be skipped if another block exists at this position.
     */
    @Builder.Default
    private boolean override = true;
}
