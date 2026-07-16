package com.hsummerhays.cloudnotes.user.api;

public record AuthResponse(
    String email,
    String displayName
) {}
