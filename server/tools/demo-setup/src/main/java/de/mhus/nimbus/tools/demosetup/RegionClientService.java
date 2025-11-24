package de.mhus.nimbus.tools.demosetup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RegionClientService extends BaseClientService {
    public RegionClientService(@Value("${region.base-url:}") String baseUrl) {
        super(baseUrl);
    }
    @Override
    public String getName() { return "region"; }
}

