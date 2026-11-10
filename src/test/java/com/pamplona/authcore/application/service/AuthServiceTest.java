package com.pamplona.authcore.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pamplona.authcore.application.exception.InvalidCredentialsException;
import com.pamplona.authcore.application.exception.UserAlreadyExistsException;
import com.pamplona.authcore.application.exception.UserNotFoundException;
import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.TokenProviderPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Casos de uso de AuthService con dobles en memoria: sin Spring, sin base de datos. */
class AuthServiceTest {

    /** Repositorio en memoria que asigna ids como lo haría la base de datos. */
    static class RepositorioEnMemoria implements UserRepositoryPort {
        final List<User> usuarios = new ArrayList<>();

        public User save(User user) {
            if (user.getId() == null) {
                user.setId((long) usuarios.size() + 1);
                usuarios.add(user);
            } else {
                usuarios.replaceAll(u -> u.getId().equals(user.getId()) ? user : u);
            }
            return user;
        }

        public Optional<User> findByUsername(String username) {
            return usuarios.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }

        public Optional<User> findById(Long id) {
            return usuarios.stream().filter(u -> u.getId().equals(id)).findFirst();
        }

        public List<User> findAll() {
            return List.copyOf(usuarios);
        }
    }

    /** Hash de mentiras: suficiente para probar que el servicio nunca guarda la contraseña en claro. */
    static class HasherFalso implements PasswordHasherPort {
        public String hash(String raw) { return "hash:" + raw; }
        public boolean matches(String raw, String hashed) { return hashed.equals("hash:" + raw); }
    }

    static class TokensFalsos implements TokenProviderPort {
        public String generateToken(User user) { return "token-de-" + user.getUsername(); }
    }

    private final RepositorioEnMemoria repo = new RepositorioEnMemoria();
    private final AuthService servicio = new AuthService(repo, new HasherFalso(), new TokensFalsos());

    @Test
    void registrarAsignaElRolUserYGuardaSoloElHash() {
        Long id = servicio.register("ana", "Clave1234");
        User guardado = repo.findById(id).orElseThrow();
        assertEquals(Set.of(Role.USER), guardado.getRoles());
        assertEquals("hash:Clave1234", guardado.getPasswordHash());
    }

    @Test
    void registrarUnUsuarioRepetidoFalla() {
        servicio.register("ana", "Clave1234");
        assertThrows(UserAlreadyExistsException.class, () -> servicio.register("ana", "Otra5678"));
    }

    @Test
    void loginConCredencialesValidasDevuelveElToken() {
        servicio.register("ana", "Clave1234");
        assertEquals("token-de-ana", servicio.login("ana", "Clave1234"));
    }

    @Test
    void loginConClaveIncorrectaOUsuarioInexistenteDaElMismoError() {
        servicio.register("ana", "Clave1234");
        assertThrows(InvalidCredentialsException.class, () -> servicio.login("ana", "Mala1234"));
        assertThrows(InvalidCredentialsException.class, () -> servicio.login("nadie", "Clave1234"));
    }

    @Test
    void asignarRolSeReflejaEnElUsuario() {
        Long id = servicio.register("ana", "Clave1234");
        servicio.assignRole(id, Role.ADMIN);
        assertEquals(Set.of(Role.USER, Role.ADMIN), repo.findById(id).orElseThrow().getRoles());
    }

    @Test
    void asignarRolAUnUsuarioInexistenteFalla() {
        assertThrows(UserNotFoundException.class, () -> servicio.assignRole(99L, Role.ADMIN));
    }
}
