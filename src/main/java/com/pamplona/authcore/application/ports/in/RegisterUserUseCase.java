package com.pamplona.authcore.application.ports.in;

/**
 * Puerto de entrada: registrar un nuevo usuario.
 */
public interface RegisterUserUseCase {
    Long register(String username, String rawPassword);
}
