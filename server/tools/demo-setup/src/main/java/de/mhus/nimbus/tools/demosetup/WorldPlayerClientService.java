package de.mhus.nimbus.tools.demosetup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorldPlayerClientService extends BaseClientService {
    public WorldPlayerClientService(@Value("${world.player.base-url:}") String baseUrl) {
        super(baseUrl);
    }
    @Override
    public String getName() { return "world-player"; }
}

