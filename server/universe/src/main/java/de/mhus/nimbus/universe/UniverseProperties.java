package de.mhus.nimbus.universe;

import de.mhus.nimbus.shared.security.KeyIntent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UniverseProperties {

    private static final String JWT_TOKEN_KEY_OWNER = "theUniverse";
    public static final KeyIntent MAIN_JWT_TOKEN_INTENT = new KeyIntent(JWT_TOKEN_KEY_OWNER, KeyIntent.MAIN_JWT_TOKEN);

    @Value("${universe.security.auth-expires-minutes:60}")
    private int securityAuthExpiresMinutes = 60;
    @Value("${universe.security.refresh-expires-days:30}")
    private int securityRefreshExpiresDays = 30;
    @Value("${universe.security.refresh-max-total-days:365}")
    private int securityRefreshMaxTotalDays = 365;


}

