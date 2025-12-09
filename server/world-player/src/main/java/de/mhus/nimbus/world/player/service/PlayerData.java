package de.mhus.nimbus.world.player.service;

import de.mhus.nimbus.generated.configs.Settings;

public record PlayerData(
        PlayerUser user,
        PlayerCharacter character,
        Settings settings
) {
}
