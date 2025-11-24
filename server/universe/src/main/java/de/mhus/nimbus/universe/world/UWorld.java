package de.mhus.nimbus.universe.world;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * UWorld Entity für das Universe-Modul.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "uWorlds")
public class UWorld {

    @Id
    private String id;

    private String name;
    // Externe/technische ID der Welt (vom aufzeichnenden System), optional
    private String worldId;
    private String description;

    private String apiUrl;

    @CreatedDate
    private Date createdAt;

    private String regionId;
    private String planetId;
    private String solarSystemId;
    private String galaxyId;

    // Freies Koordinatenfeld (z. B. "x,y,z" oder ein anderer Notationsstil)
    private String coordinates;
}
