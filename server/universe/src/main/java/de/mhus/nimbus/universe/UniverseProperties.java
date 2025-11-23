package de.mhus.nimbus.universe;

import de.mhus.nimbus.shared.security.KeyIntent;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "universe")
@Data
public class UniverseProperties {

    private static final String JWT_TOKEN_KEY_OWNER = "theUniverse";
    public static final KeyIntent MAIN_JWT_TOKEN_INTENT = new KeyIntent(JWT_TOKEN_KEY_OWNER, KeyIntent.MAIN_JWT_TOKEN);

    private int securityAuthExpiresMinutes = 60;
    private int securityRefreshExpiresDays = 30;

    private int securityRefreshMaxTotalDays = 365;


}

