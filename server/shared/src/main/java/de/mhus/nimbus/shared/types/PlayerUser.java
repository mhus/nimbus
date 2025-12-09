package de.mhus.nimbus.shared.types;

import lombok.Data;

@Data
public class PlayerUser {
    private String userId;
    private String displayName;
    private String developmentPassword;
}
