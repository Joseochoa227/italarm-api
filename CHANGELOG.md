# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [Sin publicar] — Fase 1: Catálogo, terceros, tasas y configuración

### Agregado
- Catálogo (migración V4):
  - categorías y unidades de medida con sus valores iniciales;
  - productos con código único, precios en USD u otra moneda, stock mínimo según la unidad, foto, activar/desactivar y búsqueda paginada.
- Clientes y proveedores (migración V5):
  - teléfono con indicativo internacional;
  - documento único;
  - precio aplicado según el tipo de cliente.
- Tasas de cambio (migración V6):
  - TRM automática diaria desde datos.gov.co, con reintentos e idempotencia;
  - tasa del bolívar manual con doble digitación y alerta de variación;
  - TRM manual si falla la automática;
  - correcciones con historial, tasas vigentes con avisos e historial por fechas;
  - casos CP-10, CP-11 y CP-12.
- Configuración: edición de los datos de la empresa, el logo y los valores por defecto.
- Gestión de usuarios: crear, desactivar/activar y restablecer contraseña.
- Almacenamiento de archivos:
  - S3 (Cloudflare R2, AWS S3, DigitalOcean Spaces) o disco local, con enlaces firmados de 15 minutos;
  - validación de imágenes por contenido.
- Listados paginados, control de versión en las ediciones y traducción de restricciones de la base de datos a códigos de negocio.
- Contrato `contrato/openapi.json` versionado y `docs/guia-frontend.md` para italarm-web.

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
