package com.mindhub.user_service.dtos;

public record WelcomeMessage(
        String username,
        String email,
        String token
) {
}