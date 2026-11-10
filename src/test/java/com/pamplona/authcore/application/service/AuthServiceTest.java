package com.pamplona.authcore.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pamplona.authcore.application.exception.InvalidCredentialsException;
import com.pamplona.authcore.application.exception.UserAlreadyExistsException;
import com.pamplona.authcore.application.exception.UserNotFoundException;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import com.pamplona.authcore.testsupport.Dobles.HasherFalso;
import com.pamplona.authcore.testsupport.Dobles.RepositorioEnMemoria;
import com.pamplona.authcore.testsupport.Dobles.TokensFalsos;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Casos de uso de AuthService con dobles en memoria: sin Spring, sin base de datos. */
class AuthServiceTest {

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
