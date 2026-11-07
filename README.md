# AuthCore — Chronos (Sistema de Turnos)

Servicio de autenticación del proyecto **Chronos** (Ingeniería del Software III, Universidad de Pamplona).
Registra usuarios, autentica con BCrypt y emite un **JWT firmado (HS384)** que el
[Chronos-Domain-Service](https://github.com/Jhorm4nnn/Chronos-Domain-Service) valida localmente con el mismo secreto.

Java 21 · Spring Boot · Spring Security · Gradle · MySQL · jjwt 0.12.6 · arquitectura hexagonal (paquete `com.pamplona.authcore`).

## Historias de usuario (HU-1 a HU-7)

| HU | Qué hace | Dónde |
|---|---|---|
| HU-1 | Registro de usuario (rol USER por defecto) | `RegisterUserUseCase`, `AuthService`, `POST /api/auth/register` |
| HU-2 | Login → JWT con `uid` y `roles` | `LoginUseCase`, `JwtTokenProviderAdapter`, `POST /api/auth/login` |
| HU-3 | Asignar rol (solo ADMIN) | `AssignRoleUseCase`, `POST /api/auth/users/{id}/roles` |
| HU-4 | Acceso por rol (401 / 403) | `SecurityConfig`, `JwtAuthenticationFilter` |
| HU-5 | Contraseñas con BCrypt | `BCryptPasswordHasherAdapter` |
| HU-6 | JWT con firma y expiración configurable | `authcore.jwt.expiration-ms` (`JWT_EXPIRATION_MS`) |
| HU-7 | Dominio separado de la infraestructura | `domain/`, `application/ports`, `infrastructure/adapters` |

El backlog está en [`docs/`](docs/). La plantilla CloudFormation del laboratorio de AWS (S3 e IAM) está en
[`docs/aws/`](docs/aws/infraestructura.yaml).

## Arquitectura

```
domain/          User, Role (Java puro)
application/     puertos in (casos de uso) y out (repositorio, hasher, token), AuthService, excepciones
infrastructure/  REST (in), JPA + BCrypt + JWT (out), configuración de seguridad
```

## Configuración (variables de entorno, nunca en el repositorio)

| Variable | Para qué |
|---|---|
| `JWT_SECRET_BASE64` | Clave compartida con el Domain Service, en Base64, mínimo 48 bytes. **Obligatoria** |
| `SPRING_DATASOURCE_URL` | Opcional; por defecto `jdbc:mysql://localhost:3306/authcore_db` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credenciales de MySQL. **Obligatorias** |
| `JWT_EXPIRATION_MS` | Vigencia del token, por defecto 3600000 (1 h) |

Generar un secreto (una sola vez, y usar el mismo en ambos servicios):

```bash
openssl rand -base64 48
```

## Ejecución

```bash
export JWT_SECRET_BASE64='<secreto Base64>'
export SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=...
gradle bootRun          # http://localhost:8081
```

Con Docker: crear un archivo `.env` (ignorado por git) con `JWT_SECRET_BASE64=...` y ejecutar `docker compose up --build`.

Al arrancar por primera vez se crea un usuario administrador inicial (ver `AdminBootstrapRunner`);
cambie su contraseña antes de exponer el servicio.

## Proyecto Chronos — repositorios relacionados

| Repositorio | Contenido |
|---|---|
| **Chronos-AuthCore** (este) | Autenticación y roles (HU-1 a HU-7), puerto 8081 |
| [Chronos-Domain-Service](https://github.com/Jhorm4nnn/Chronos-Domain-Service) | Agenda de turnos (HU-8 a HU-12), puerto 8082 |
| [Crhonos-Sistema_de_Turnos](https://github.com/Jhorm4nnn/Crhonos-Sistema_de_Turnos) | Repositorio original, reemplazado por estos dos |
