# CLAUDE.md — italarm-api

Guía para el agente de desarrollo (sección 13 de `docs/requerimientos.md`). Mantener actualizada (AG-07).

## Antes de trabajar

- Lee `docs/requerimientos.md` completo. Prevalece sobre el prototipo. Las secciones 10 y 11 prevalecen sobre las preferencias propias.
- Las decisiones de ITALARM en `docs/preguntas.md` prevalecen sobre el documento de requerimientos.
- Una fase a la vez (sección 12). Al iniciar una fase, escribe `docs/plan-fase-N.md` y espera la aprobación.
- No inventes reglas de negocio. Lo que no esté definido va a `docs/preguntas.md` y se pregunta a ITALARM (AG-05).

## Comandos

```bash
docker compose up -d                 # PostgreSQL local (lee .env)
./mvnw spring-boot:run               # API con perfil local (lee .env)
./mvnw verify                        # todo lo que corre la CI: formato, Checkstyle, pruebas, ArchUnit, JaCoCo
./mvnw test -Dtest=NombreDeLaPrueba  # una prueba
./mvnw spotless:apply                # aplicar formato antes de hacer commit
```

- Las pruebas de integración necesitan Docker (Testcontainers con `postgres:16-alpine`).
- En el entorno web de Claude Code, Maven Central puede responder 429. En ese caso:
  - usa el espejo de Google con `MVNW_REPOURL=https://maven-central.storage-download.googleapis.com/maven2`;
  - configura un `<mirror>` hacia esa URL en `~/.m2/settings.xml`;
  - si `dockerd` no está corriendo, arráncalo.

## Estructura

Monolito modular por módulo de negocio (sección 10.1), paquete raíz `co.italarm.api`:

```
shared/          dinero, moneda, fechas, errores, auditoría, seguridad, OpenAPI
  dominio/         Dinero, Moneda, Redondeo, FormatoDinero, FechaNegocio, NegocioException, EntidadAuditable
  api/             ManejadorGlobalErrores, FabricaProblemas, FiltroCorrelacion, ConfiguracionJson
  seguridad/       ConfiguracionSeguridad, FiltroToken, ValidadorToken, UsuarioAutenticado
  infraestructura/ reloj, auditoría JPA, OpenAPI, PropiedadesItalarm
usuarios/        sesión (token), usuarios, cambio de contraseña
configuracion/   datos de la empresa y valores por defecto (edición en Fase 1)
```

Capas dentro de cada módulo:
- `api`: controladores y DTO (records).
- `aplicacion`: servicios de caso de uso con `@Transactional`.
- `dominio`: entidades y reglas puras.
- `infraestructura`: repositorios y clientes externos.

Los módulos que falten se crean en la fase que los necesita.

`ArquitecturaTest` (ArchUnit) hace cumplir en cada compilación:
- los controladores no usan infraestructura;
- el dominio no depende de Spring (salvo las anotaciones de auditoría de Spring Data);
- `@Transactional` solo en `aplicacion`;
- sin `@Autowired` en campos;
- un módulo no usa el `dominio` ni la `infraestructura` de otro (salvo `shared`).

## Convenciones

- **Idioma**: dominio, clases, métodos, rutas y mensajes en español (BP-23). Commits con Conventional Commits en español (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `build:`, `ci:`).
- **Dinero**:
  - Siempre `BigDecimal`, con el objeto de valor `Dinero` (monto + moneda). Checkstyle prohíbe `double` y `float`.
  - Redondeo `HALF_UP` y escalas solo en `Redondeo`: 6 decimales para calcular, 4 para guardar; al mostrar, COP 0 y USD/VES 2.
  - En la base de datos, `NUMERIC(19,4)` o `NUMERIC(19,6)`.
  - En JSON, texto decimal: `{"monto":"19.5000","moneda":"USD"}`.
- **Fechas**:
  - Instantes en `TIMESTAMPTZ` UTC (`Instant`).
  - Fechas de negocio en `America/Bogota` (`LocalDate`) con `FechaNegocio`.
  - Nunca `LocalDate.now()` ni `Instant.now()` directos: se inyecta `Clock` o `FechaNegocio`.
- **Errores**:
  - Se lanza una subclase de `NegocioException` con un código estable (`STOCK_INSUFICIENTE`), un `TipoError` y un mensaje en español.
  - `ManejadorGlobalErrores` responde en Problem Details con `codigo` y `correlationId`.
  - `TipoError` → HTTP: VALIDACION 400, NO_AUTENTICADO 401, ACCESO_DENEGADO 403, NO_ENCONTRADO 404, CONFLICTO 409, REGLA_NEGOCIO 422.
- **Validación**: Bean Validation en los DTO, siempre con `message` en español. Las reglas de negocio van en el dominio.
- **Entidades**:
  - Heredan de `EntidadAuditable` (`created_*`, `updated_*`) o, si son datos maestros, de `EntidadMaestra` (además, `@Version`).
  - Sin Lombok. Constructor protegido para JPA.
  - Nunca se exponen en la API: siempre DTO.
- **Migraciones**:
  - `src/main/resources/db/migration/V{n}__descripcion.sql`.
  - Nunca modificar una ya aplicada: siempre una nueva (AG-10).
  - Hibernate solo valida (`ddl-auto=validate`).
  - Restricciones en la base de datos como segunda línea de defensa (BP-09); índices en llaves foráneas.
- **Pruebas**:
  - TDD en reglas de negocio (AG-04).
  - Unitarias del dominio sin Spring.
  - Integración: heredan de `soporte.PruebaIntegracion` (aplicación completa + PostgreSQL real). Esa clase restablece usuarios y sesiones antes de cada prueba.
  - Los casos de aceptación se nombran `cpNN_...` (BP-26).
  - Cobertura mínima de 80 % por paquete en `dominio` y `aplicacion` (JaCoCo). Nunca desactivar pruebas (AG-08).
- **Secretos**: nunca en el repositorio ni en `application*.yml`, y tampoco las contraseñas de usuarios (AG-09). Todo va por variables de entorno. El `.env` local está en `.gitignore`.

## Decisiones técnicas

| Decisión | Motivo |
|---|---|
| Sesión con **token opaco** en `Authorization: Bearer`, guardado por el frontend en `localStorage`. Sin cookies y sin CSRF. | Decisión de ITALARM (P-05), en lugar de la cookie + CSRF de la sección 9.1. Funciona con cualquier dominio de hosting. |
| El token es aleatorio (256 bits) y en la base de datos solo se guarda su SHA-256. No es un JWT. | Las sesiones no vencen (P-03), así que deben poder revocarse en la base de datos: al cerrar sesión o al cambiar la contraseña. |
| Sin límite de intentos de ingreso ni vencimiento de sesión. | Decisión de ITALARM (P-02, P-03), en lugar de BP-20. Riesgo aceptado (plan de la Fase 0, sección 2). |
| Se ingresa con el **correo**. Jose y Victor se crean en `V1__usuarios.sql` sin contraseña; `InicializadorUsuarios` asigna `ITALARM_CLAVE_INICIAL` al arrancar a quien no tenga. | P-01 sin poner la contraseña en el repositorio (AG-09). |
| Política de contraseñas: 8 a 64 caracteres, mayúscula, minúscula, número y signo. | P-07 y P-08. 64 caracteres por el límite de 72 bytes de BCrypt. |
| Swagger UI y `/v3/api-docs` públicos en todos los ambientes. | P-06. |
| Checkstyle (no SpotBugs) con reglas propias en `config/checkstyle/checkstyle.xml`; el formato lo aplica Spotless (Google Java Format). | BP-22. |
| ArchUnit para las reglas de arquitectura de la sección 10. | Verificación automática en cada compilación. |
| `ultimo_uso` de la sesión se actualiza como máximo cada 5 minutos. | Evitar una escritura en cada petición. |
| El contrato OpenAPI se exporta a `target/openapi.json` en las pruebas y la CI lo publica como artefacto. | RT-08: italarm-web genera su cliente desde ese archivo. |
