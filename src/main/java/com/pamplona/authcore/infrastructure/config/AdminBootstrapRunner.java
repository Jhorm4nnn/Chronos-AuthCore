package com.pamplona.authcore.infrastructure.config;

import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.PasswordPolicy;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Crea el administrador inicial al arrancar, SOLO si no existe todavia.
 * Resuelve el problema de "quien asigna el primer rol ADMIN" sin acceso directo a la base de datos.
 * Usuario y contrasena llegan por ADMIN_USERNAME y ADMIN_PASSWORD; si faltan, no se crea ninguno.
 * La contrasena nunca se escribe en el log.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final String username;
    private final String password;

    public AdminBootstrapRunner(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher,
                                @Value("${authcore.admin.username:}") String username,
                                @Value("${authcore.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() || password.isBlank()) {
            log.info("No se crea administrador inicial: ADMIN_USERNAME y ADMIN_PASSWORD no estan definidas");
            return;
        }
        if (!PasswordPolicy.cumple(password)) {
            throw new IllegalStateException("ADMIN_PASSWORD " + PasswordPolicy.DESCRIPCION);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        Set<Role> roles = new HashSet<>(Set.of(Role.ADMIN, Role.USER));
        userRepository.save(new User(null, username, passwordHasher.hash(password), roles));
        log.info("Administrador inicial '{}' creado", username);
    }
}
