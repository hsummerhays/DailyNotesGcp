package com.hsummerhays.cloudnotes.user.api;

public record AuthResponse(
    String token,
    String email,
    String displayName
) {}
