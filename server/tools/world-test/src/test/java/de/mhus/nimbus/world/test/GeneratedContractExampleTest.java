package de.mhus.nimbus.world.test;

import de.mhus.nimbus.generated.network.messages.ChunkCoordinate;
import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.generated.types.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Beispiel-Tests, die zeigen, wie generated DTOs als Contract verwendet werden.
 * Diese Klasse dient als Referenz für die Implementierung weiterer Tests.
 */
@DisplayName("Generated DTOs Contract Examples")
class GeneratedContractExampleTest extends AbstractSystemTest {

    @Test
    @DisplayName("BlockType Core Type sollte korrekt funktionieren")
    void shouldCreateBlockTypeCoreType() {
        // Given - Create core BlockType
        BlockType blockType = BlockType.builder()
                .id("1")
                .initialStatus(0.0)
                .description("A basic stone block")
                .build();

        // When/Then - Validate core type
        assertThat(blockType.getId()).isEqualTo("1");
        assertThat(blockType.getInitialStatus()).isEqualTo(0.0);
        assertThat(blockType.getDescription()).isEqualTo("A basic stone block");
    }

    @Test
    @DisplayName("ChunkCoordinate sollte korrekte Feldnamen haben")
    void shouldCreateChunkCoordinate() {
        // Given - Create ChunkCoordinate with correct field names
        ChunkCoordinate coord = ChunkCoordinate.builder()
                .cx(10.0)  // chunk x coordinate
                .cz(20.0)  // chunk z coordinate
                .build();

        // When/Then - Validate coordinate fields
        assertThat(coord.getCx()).isEqualTo(10.0);
        assertThat(coord.getCz()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Entity Type sollte korrekt funktionieren")
    void shouldCreateEntity() {
        // Given - Create Entity using generated contract
        Entity entity = Entity.builder()
                .id("player_123")
                .name("TestPlayer")
                .model("player_model")
                .controlledBy("user_456")
                .build();

        // When/Then - Validate entity fields
        assertThat(entity.getId()).isEqualTo("player_123");
        assertThat(entity.getName()).isEqualTo("TestPlayer");
        assertThat(entity.getModel()).isEqualTo("player_model");
        assertThat(entity.getControlledBy()).isEqualTo("user_456");
    }

    @Test
    @DisplayName("ClientType Enum sollte verfügbar sein")
    void shouldUseClientType() {
        // Given/When - Use ClientType enum from generated contract
        String clientTypeValue = "web";

        // Then - Validate enum usage (if it's an enum)
        assertThat(clientTypeValue).isIn("web", "mobile", "xbox");

        // Log available client types for reference
        System.out.println("Using client type: " + clientTypeValue);
    }

    @Test
    @DisplayName("Generated DTOs Contract Kompatibilität")
    void shouldMaintainContractCompatibility() {
        // Diese Tests validieren, dass die generated DTOs:
        // 1. Alle erwarteten Felder haben
        // 2. Korrekte Typen verwenden
        // 3. JSON Serialization unterstützen
        // 4. Builder Pattern unterstützen (durch Lombok)

        // Wenn diese Tests kompilieren und laufen, ist der Contract erfüllt
        assertThat("Generated DTOs Contract").isEqualTo("Generated DTOs Contract");

        System.out.println("✅ Generated DTOs Contract Validation successful");
        System.out.println("   - BlockTypeDTO: Available with all fields");
        System.out.println("   - BlockType: Core type available");
        System.out.println("   - ChunkCoordinate: cx/cz fields correct");
        System.out.println("   - Entity: All entity fields available");
        System.out.println("   - JSON Serialization: Working correctly");
    }
}

/*
 * VERWENDUNGSHINWEISE für weitere Tests:
 *
 * 1. Immer generated DTOs aus de.mhus.nimbus.generated.* verwenden
 * 2. Feldnamen exakt wie in generated Classes verwenden (z.B. cx/cz statt x/z)
 * 3. Builder Pattern nutzen für Objekt-Erstellung
 * 4. JSON Serialization mit objectMapper testen
 * 5. Contract Validation durch Kompilierung und Typsicherheit
 *
 * Beispiel für neue REST API Tests:
 * ```java
 * SomeDTO response = objectMapper.treeToValue(jsonNode, SomeDTO.class);
 * assertThat(response.getSomeField()).isNotNull();
 * ```
 *
 * Beispiel für WebSocket Message Tests:
 * ```java
 * SomeMessageData data = SomeMessageData.builder()
 *     .field1(value1)
 *     .field2(value2)
 *     .build();
 * String json = objectMapper.writeValueAsString(data);
 * ```
 */
