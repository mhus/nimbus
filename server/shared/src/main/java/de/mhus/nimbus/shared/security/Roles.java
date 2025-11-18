package de.mhus.nimbus.shared.security;

/**
 * Rollen im System. Diese Enum kann bei Bedarf erweitert werden.
 * Die Speicherung in der User-Entität erfolgt als kommaseparierte Liste der Enum-Namen.
 */
public enum Roles {
    USER,
    ADMIN;
}

