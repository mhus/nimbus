package de.mhus.nimbus.shared.types;

import de.mhus.nimbus.generated.configs.Settings;

public record PlayerData(
        PlayerUser user,
        PlayerCharacter character,
        Settings settings
) {
}
