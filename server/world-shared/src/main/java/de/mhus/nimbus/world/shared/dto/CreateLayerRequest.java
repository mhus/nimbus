package de.mhus.nimbus.world.shared.dto;

import de.mhus.nimbus.shared.annotations.GenerateTypeScript;
import de.mhus.nimbus.shared.annotations.TypeScript;
import de.mhus.nimbus.world.shared.layer.LayerType;

import java.util.List;
import java.util.Map;

/**
 * DTO for creating a new WLayer.
 * Used for API requests and TypeScript generation.
 *
 * Note: mountX/Y/Z and groups are deprecated - these fields are now in WLayerModel.
 * They are kept here for backwards compatibility but will be ignored.
 */
@GenerateTypeScript("dto")
public record CreateLayerRequest(
        String name,

        @TypeScript(follow = true, optional = true)
        LayerType layerType,

        @Deprecated
        @TypeScript(optional = true)
        Integer mountX,

        @Deprecated
        @TypeScript(optional = true)
        Integer mountY,

        @Deprecated
        @TypeScript(optional = true)
        Integer mountZ,

        @Deprecated
        @TypeScript(optional = true)
        Boolean ground,

        @TypeScript(optional = true)
        Boolean allChunks,

        @TypeScript(optional = true)
        List<String> affectedChunks,

        @TypeScript(optional = true)
        Integer order,

        @TypeScript(optional = true)
        Boolean enabled,

        @Deprecated
        @TypeScript(optional = true)
        Map<Integer, String> groups
) {
}
