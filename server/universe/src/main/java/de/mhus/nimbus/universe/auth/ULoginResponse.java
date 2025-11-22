package de.mhus.nimbus.universe.auth;

public record ULoginResponse(String token, String refreshToken, String userId, String username) {}
