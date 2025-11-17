package de.mhus.nimbus.generated.network;

/**
 * Generated from MessageTypes.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum MessageType {
    LOGIN("login"),
    LOGIN_RESPONSE("loginResponse"),
    LOGOUT("logout"),
    PING("p"),
    WORLD_STATUS_UPDATE("w.su"),
    CHUNK_REGISTER("c.r"),
    CHUNK_QUERY("c.q"),
    CHUNK_UPDATE("c.u"),
    BLOCK_UPDATE("b.u"),
    BLOCK_STATUS_UPDATE("b.s.u"),
    ITEM_BLOCK_UPDATE("b.iu"),
    BLOCK_INTERACTION("b.int"),
    ENTITY_UPDATE("e.u"),
    ENTITY_CHUNK_PATHWAY("e.p"),
    ENTITY_POSITION_UPDATE("e.p.u"),
    ENTITY_INTERACTION("e.int.r"),
    ANIMATION_START("a.s"),
    USER_MOVEMENT("u.m"),
    PLAYER_TELEPORT("p.t"),
    INTERACTION_REQUEST("int.r"),
    INTERACTION_RESPONSE("int.rs"),
    CMD("cmd"),
    CMD_MESSAGE("cmd.msg"),
    CMD_RESULT("cmd.rs"),
    SCMD("scmd"),
    SCMD_RESULT("scmd.rs");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
