package com.pamplona.authcore.infrastructure.config;

import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Crea un usuario admin por defecto al arrancar, SOLO si no existe todavia.
 * Resuelve el problema de "quien asigna el primer rol ADMIN" sin necesitar
 * acceso directo a la base de datos.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public AdminBootstrapRunner(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).isEmpty()) {
            Set<Role> roles = new HashSet<>(Set.of(Role.ADMIN, Role.USER));
            User admin = new User(null, DEFAULT_ADMIN_USERNAME,
                    passwordHasher.hash(DEFAULT_ADMIN_PASSWORD), roles);
            userRepository.save(admin);
            log.warn("=================================================================");
            log.warn(" Usuario admin creado automaticamente:");
            log.warn("   username: {}", DEFAULT_ADMIN_USERNAME);
            log.warn("   password: {}", DEFAULT_ADMIN_PASSWORD);
            log.warn(" CAMBIA esta contrasena si el servicio queda expuesto o compartido.");
            log.warn("=================================================================");
        }
    }
}
