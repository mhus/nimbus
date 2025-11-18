package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.universe.user.UUser;

public record CurrentUser(String userId, String username, UUser user) {}

