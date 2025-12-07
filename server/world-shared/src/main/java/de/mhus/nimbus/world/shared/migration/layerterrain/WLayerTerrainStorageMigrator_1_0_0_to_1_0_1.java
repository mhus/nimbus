package de.mhus.nimbus.world.shared.migration.layerterrain;

import de.mhus.nimbus.shared.engine.EngineMapper;
import de.mhus.nimbus.shared.persistence.SchemaMigrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WLayerTerrainStorageMigrator_1_0_0_to_1_0_1 implements SchemaMigrator {

    private final EngineMapper mapper;

    @Override
    public String getEntityType() {
        return "WLayerTerrainStorage";
    }

    @Override
    public String getFromVersion() {
        return "1.0.0";
    }

    @Override
    public String getToVersion() {
        return "1.0.1";
    }

    @Override
    public String migrate(String entityJson) throws Exception {
        var json = mapper.readTree(entityJson);

        var blocks = json.get("blocks"); // array of blocks
        if (blocks != null && blocks.isArray()) {
            for (var blockNode : blocks) {
                var blockBlock = blockNode.get("block"); // obeject block
                var statusMap = blockNode.get("status"); // object status
                // every value is modifier
                if (statusMap != null && statusMap.isObject()) {
                    var fields = statusMap.fields();
                    while (fields.hasNext()) {
                        var entry = fields.next();
                        var modifierName = entry.getKey();
                        var modifierValue = entry.getValue();
                        var visibility = modifierValue.get("visibility");
                        if (visibility != null && visibility.isObject()) {
                            var rotationX = visibility.get("rotationX").asDouble(0);
                            var rotationY = visibility.get("rotationY").asDouble(0);
                            if (rotationX != 0 || rotationY != 0) {
                                // Apply rotation to block
                                var rotationNode = mapper.createObjectNode();
                                rotationNode.put("x", rotationX);
                                rotationNode.put("y", rotationY);
                                ((com.fasterxml.jackson.databind.node.ObjectNode) blockBlock).set("rotation", rotationNode);
                            }
                            ((com.fasterxml.jackson.databind.node.ObjectNode) visibility).remove("rotationX");
                            ((com.fasterxml.jackson.databind.node.ObjectNode) visibility).remove("rotationY");
                        }
                    }
                    // Remove status map
                    ((com.fasterxml.jackson.databind.node.ObjectNode) blockNode).remove("status");
                }
            }
        }
        return mapper.writeValueAsString(json);
    }
}
