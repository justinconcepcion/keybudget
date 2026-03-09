package com.keybudget.auth.dto;

public record AuthResponse(String accessToken, long expiresIn) {}
