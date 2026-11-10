package com.pamplona.authcore.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import com.pamplona.authcore.testsupport.Dobles.HasherFalso;
import com.pamplona.authcore.testsupport.Dobles.RepositorioEnMemoria;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class AdminBootstrapRunnerTest {

    private final RepositorioEnMemoria repo = new RepositorioEnMemoria();

    private AdminBootstrapRunner runner(String usuario, String clave) {
        return new AdminBootstrapRunner(repo, new HasherFalso(), usuario, clave);
    }

    @Test
    void sinVariablesNoCreaNingunAdministrador() {
        runner("", "").run();
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void creaElAdministradorConRolesAdminYUserSinEscribirLaContrasenaEnElLog(CapturedOutput salida) {
        runner("jefe", "Inicial2026").run();
        User admin = repo.findByUsername("jefe").orElseThrow();
        assertEquals(Set.of(Role.ADMIN, Role.USER), admin.getRoles());
        assertEquals("hash:Inicial2026", admin.getPasswordHash());
        assertTrue(salida.getAll().contains("jefe"));
        assertFalse(salida.getAll().contains("Inicial2026"));
    }

    @Test
    void siYaExisteNoLoDuplica() {
        runner("jefe", "Inicial2026").run();
        runner("jefe", "Inicial2026").run();
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void unaContrasenaDebilImpideArrancar() {
        assertThrows(IllegalStateException.class, () -> runner("jefe", "soloLetrasLargas").run());
        assertThrows(IllegalStateException.class, () -> runner("jefe", "corta1").run());
        assertTrue(repo.findAll().isEmpty());
    }
}
