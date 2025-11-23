package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.security.KeyIntent;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "universe.security")
@Data
public class USecurityProperties {

    public static final String JWT_TOKEN_KEY_OWNER = "jwt-auth-token";
    public static final KeyIntent MAIN_JWT_TOKEN_INTENT = new KeyIntent(JWT_TOKEN_KEY_OWNER, KeyIntent.MAIN_JWT_TOKEN);

    private int authExpiresMinutes = 60;
    private int refreshExpiresDays = 30;

    private int refreshMaxTotalDays = 365;


}

