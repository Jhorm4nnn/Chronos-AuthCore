package com.pamplona.authcore.infrastructure.adapters.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Prueba de punta a punta (HTTP -> casos de uso -> H2) con la seguridad JWT real. */
@SpringBootTest
@ActiveProfiles("test")
class AuthApiTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private WebApplicationContext contexto;
    @Value("${authcore.jwt.secret-base64}")
    private String secreto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
    }

    private String tokenCon(String... roles) {
        Instant ahora = Instant.now();
        return Jwts.builder().subject("tester").claim("uid", 99).claim("roles", List.of(roles))
                .issuedAt(Date.from(ahora)).expiration(Date.from(ahora.plusSeconds(600))).signWith(clave()).compact();
    }

    private ResultActions postJson(String ruta, String json) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String usuarioNuevo() {
        return "usuario" + SECUENCIA.incrementAndGet();
    }

    @Test
    void registrarDevuelve201SinLaContrasenaYConRolUser() throws Exception {
        String usuario = usuarioNuevo();
        postJson("/api/auth/register", "{\"username\":\"" + usuario + "\",\"password\":\"Clave1234\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(usuario))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registrarDosVecesElMismoUsuarioDa409() throws Exception {
        String json = "{\"username\":\"" + usuarioNuevo() + "\",\"password\":\"Clave1234\"}";
        postJson("/api/auth/register", json).andExpect(status().isCreated());
        postJson("/api/auth/register", json).andExpect(status().isConflict());
    }

    @Test
    void loginDevuelveUnJwtConUidYRoles() throws Exception {
        String usuario = usuarioNuevo();
        postJson("/api/auth/register", "{\"username\":\"" + usuario + "\",\"password\":\"Clave1234\"}");
        String cuerpo = postJson("/api/auth/login", "{\"username\":\"" + usuario + "\",\"password\":\"Clave1234\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Claims claims = Jwts.parser().verifyWith(clave()).build()
                .parseSignedClaims(JsonPath.read(cuerpo, "$.token")).getPayload();
        assertEquals(usuario, claims.getSubject());
        assertEquals(List.of("USER"), claims.get("roles", List.class));
        assertEquals(Integer.class, claims.get("uid").getClass());
    }

    @Test
    void loginInvalidoDa401() throws Exception {
        postJson("/api/auth/login", "{\"username\":\"nadie\",\"password\":\"Clave1234\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoSinTokenDa401() throws Exception {
        mvc.perform(get("/api/auth/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoConTokenInvalidoDa401() throws Exception {
        mvc.perform(get("/api/auth/users").header("Authorization", "Bearer no-es-un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointDeAdminConTokenUserDa403() throws Exception {
        mvc.perform(get("/api/auth/users").header("Authorization", "Bearer " + tokenCon("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpointDeAdminConTokenAdminDa200() throws Exception {
        mvc.perform(get("/api/auth/users").header("Authorization", "Bearer " + tokenCon("USER", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void asignarRolAUnUsuarioInexistenteDa404() throws Exception {
        mvc.perform(post("/api/auth/users/99999/roles").header("Authorization", "Bearer " + tokenCon("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
    }
}
