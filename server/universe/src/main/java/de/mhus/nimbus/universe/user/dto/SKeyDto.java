package de.mhus.nimbus.universe.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "SKey", description = "Metadaten eines Schlüssels (ohne Key-Material)")
public class SKeyDto {

    @Schema(description = "Primärschlüssel der Persistenz", example = "1")
    public Long id;

    @Schema(description = "Kontexttyp", example = "UNIVERSE")
    public String type;

    @Schema(description = "Art des Schlüssels", example = "public")
    public String kind;

    @Schema(description = "Algorithmus", example = "RSA")
    public String algorithm;

    @Schema(description = "Name/UUID des Schlüssels")
    public String name;

    @Schema(description = "Erstellzeitpunkt")
    public Instant createdAt;
}
