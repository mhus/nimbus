/*
 * Source TS: MessageTypes.ts
 * Original TS: 'enum MessageType'
 */
package de.mhus.nimbus.evaluate.generated.network;

public enum MessageType {
    LOGIN(1),
    LOGIN_RESPONSE(2),
    LOGOUT(3),
    PING(4),
    WORLD_STATUS_UPDATE(5),
    CHUNK_REGISTER(6),
    CHUNK_QUERY(7),
    CHUNK_UPDATE(8),
    BLOCK_UPDATE(9),
    BLOCK_STATUS_UPDATE(10),
    ITEM_BLOCK_UPDATE(11),
    BLOCK_INTERACTION(12),
    ENTITY_UPDATE(13),
    ENTITY_CHUNK_PATHWAY(14),
    ENTITY_POSITION_UPDATE(15),
    ENTITY_INTERACTION(16),
    ANIMATION_START(17),
    EFFECT_TRIGGER(18),
    EFFECT_PARAMETER_UPDATE(19),
    USER_MOVEMENT(20),
    PLAYER_TELEPORT(21),
    INTERACTION_REQUEST(22),
    INTERACTION_RESPONSE(23),
    CMD(24),
    CMD_MESSAGE(25),
    CMD_RESULT(26),
    SCMD(27),
    SCMD_RESULT(28);

    @lombok.Getter
    private final int tsIndex;
    MessageType(int tsIndex) { this.tsIndex = tsIndex; }
}
