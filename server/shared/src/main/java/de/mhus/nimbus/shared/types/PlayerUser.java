package de.mhus.nimbus.shared.types;

import de.mhus.nimbus.shared.annotations.GenerateTypeScript;
import lombok.Data;

@Data
@GenerateTypeScript("entities")
public class PlayerUser {
    private String userId;
    private String displayName;
}
