package de.mhus.nimbus.tools.demosetup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorldEditorClientService extends BaseClientService {
    public WorldEditorClientService(@Value("${world.editor.base-url:}") String baseUrl) {
        super(baseUrl);
    }
    @Override
    public String getName() { return "world-editor"; }
}

