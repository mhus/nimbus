package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.universe.user.User;

public record CurrentUser(String userId, String username, User user) {}

