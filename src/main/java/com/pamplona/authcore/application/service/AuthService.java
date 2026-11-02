package com.pamplona.authcore.application.service;

import com.pamplona.authcore.application.exception.InvalidCredentialsException;
import com.pamplona.authcore.application.exception.UserAlreadyExistsException;
import com.pamplona.authcore.application.exception.UserNotFoundException;
import com.pamplona.authcore.application.ports.in.AssignRoleUseCase;
import com.pamplona.authcore.application.ports.in.ListUsersUseCase;
import com.pamplona.authcore.application.ports.in.LoginUseCase;
import com.pamplona.authcore.application.ports.in.RegisterUserUseCase;
import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.TokenProviderPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de aplicacion: implementa los 4 casos de uso (puertos de entrada)
 * usando SOLO los puertos de salida. No importa nada de "infrastructure".
 */
@Service
public class AuthService implements RegisterUserUseCase, LoginUseCase, AssignRoleUseCase, ListUsersUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenProviderPort tokenProvider;

    public AuthService(UserRepositoryPort userRepository,
                        PasswordHasherPort passwordHasher,
                        TokenProviderPort tokenProvider) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Long register(String username, String rawPassword) {
        userRepository.findByUsername(username).ifPresent(u -> {
            throw new UserAlreadyExistsException(username);
        });
        Set<Role> defaultRoles = new HashSet<>(Set.of(Role.USER));
        User user = new User(null, username, passwordHasher.hash(rawPassword), defaultRoles);
        User saved = userRepository.save(user);
        return saved.getId();
    }

    @Override
    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenProvider.generateToken(user);
    }

    @Override
    public void assignRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.addRole(role);
        userRepository.save(user);
    }

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }
}
