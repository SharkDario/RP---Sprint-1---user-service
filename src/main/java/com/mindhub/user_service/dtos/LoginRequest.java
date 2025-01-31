package com.mindhub.user_service.dtos;

public record LoginRequest(
        String email,

        String password
) {
}
