package com.pamplona.authcore.testsupport;

import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.TokenProviderPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dobles en memoria de los puertos de salida: permiten probar sin Spring ni base de datos. */
public final class Dobles {

    private Dobles() {
    }

    /** Repositorio en memoria que asigna ids como lo haría la base de datos. */
    public static class RepositorioEnMemoria implements UserRepositoryPort {
        private final List<User> usuarios = new ArrayList<>();

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

    /** Hash de mentiras: suficiente para probar que nunca se guarda la contraseña en claro. */
    public static class HasherFalso implements PasswordHasherPort {
        public String hash(String raw) { return "hash:" + raw; }
        public boolean matches(String raw, String hashed) { return hashed.equals("hash:" + raw); }
    }

    public static class TokensFalsos implements TokenProviderPort {
        public String generateToken(User user) { return "token-de-" + user.getUsername(); }
    }
}
