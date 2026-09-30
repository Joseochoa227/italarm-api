# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [Sin publicar] — Fase 0: Fundaciones

### Agregado
- Proyecto Spring Boot 3.5 / Java 21 con Maven Wrapper, organizado en módulos de negocio (sección 10.1).
- PostgreSQL local con Docker Compose y migraciones Flyway:
  - `V1` usuarios Jose y Victor;
  - `V2` configuración con los valores por defecto;
  - `V3` sesiones.
- Ingreso con correo y contraseña, sesión con token `Bearer` sin vencimiento, cierre de sesión y consulta del usuario actual (`/api/v1/sesion`).
- Cambio de contraseña con doble digitación y política de mayúscula, minúscula, número y signo; cierra las demás sesiones del usuario (`PUT /api/v1/usuarios/actual/contrasena`).
- Asignación de la contraseña inicial desde la variable `ITALARM_CLAVE_INICIAL`.
- Objeto de valor `Dinero`, redondeo centralizado, formato de dinero es-CO y fechas de negocio en hora de Colombia con reloj inyectable.
- Errores en Problem Details (RFC 9457) con código de negocio y mensajes en español, más un identificador de correlación en cada petición y en los logs.
- Auditoría automática (`created_*`, `updated_*`) y control de versión optimista.
- Seguridad:
  - contraseñas con BCrypt;
  - CORS limitado al frontend;
  - cabeceras de seguridad (CSP, X-Frame-Options, Referrer-Policy, nosniff).
- OpenAPI y Swagger UI públicos; salud con Actuator (`/actuator/health`, liveness y readiness).
- Calidad:
  - Spotless (Google Java Format);
  - Checkstyle;
  - ArchUnit;
  - JaCoCo con 80 % mínimo en dominio y aplicación.
- Dockerfile multi-etapa con usuario no root y CI en GitHub Actions (verificación completa, contrato OpenAPI, cobertura e imagen Docker).
