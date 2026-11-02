package com.pamplona.authcore.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username es obligatorio") String username,
        @NotBlank(message = "password es obligatorio")
        @Size(min = 6, message = "password debe tener al menos 6 caracteres") String password
) {}
