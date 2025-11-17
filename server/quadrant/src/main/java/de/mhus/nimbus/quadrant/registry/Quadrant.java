package de.mhus.nimbus.quadrant.registry;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quadrants")
public class Quadrant {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String apiUrl;

    private String publicSignKey;

    public Quadrant() {}

    public Quadrant(String name, String apiUrl, String publicSignKey) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.publicSignKey = publicSignKey;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getPublicSignKey() { return publicSignKey; }
    public void setPublicSignKey(String publicSignKey) { this.publicSignKey = publicSignKey; }
}

