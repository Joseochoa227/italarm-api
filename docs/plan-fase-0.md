# Plan de la Fase 0 — Fundaciones (italarm-api)

> Estado: **pendiente de aprobación de ITALARM** (AG-02). No se implementa nada hasta que el plan se apruebe.
> Base: `docs/Requerimientos_Sistema_Inventario_v0.7.md` — secciones 9, 10, 12.1, 12.2 y 13.
> Alcance de este plan: el repositorio **italarm-api** (backend). La parte de frontend de la Fase 0 se planea en `italarm-web/docs/plan-fase-0.md`; aquí solo figura lo que el backend debe entregarle.

## 1. Objetivo y entregable

Dejar lista la base técnica del backend, con el ingreso al sistema funcionando.

Entregable (12.2): Jose y Victor pueden ingresar al sistema en el ambiente de pruebas desde celular y computador y ver el menú vacío. Para eso el backend debe exponer ingreso, cierre de sesión, sesión actual y cambio de contraseña, desplegado en pruebas con su PostgreSQL.

## 2. Tareas

El orden sigue AG-03: migración → dominio con pruebas → casos de uso → endpoints → OpenAPI. Cada bloque es una rama y un Pull Request con Conventional Commits (AG-06).

### T0. Orden del repositorio — `docs:`
- [ ] Renombrar `docs/Requerimientos_Sistema_Inventario_v0.7.md` a `docs/requerimientos.md`, como pide la nota del propio documento (sección 9.3).
- [ ] Eliminar `docs/test.txt` (archivo vacío).
- [ ] Crear `docs/preguntas.md` (AG-05). Ya creado junto con este plan.
- [ ] `.gitignore` (Maven, IDE, `.env`), `.editorconfig`, `.env.example` sin valores reales.

### T1. Proyecto base — `feat:`
- [ ] Proyecto Spring Boot 3.5.x / Java 21 con Maven Wrapper (`./mvnw`), `groupId co.italarm`, paquete raíz `co.italarm.api`.
- [ ] Dependencias: web, validation, data-jpa, security, actuator, flyway (+ `flyway-database-postgresql`), postgresql, springdoc-openapi-starter-webmvc-ui, spring-session-jdbc. Pruebas: spring-boot-starter-test (JUnit 5, AssertJ, Mockito), spring-security-test, testcontainers (postgresql, junit-jupiter), ArchUnit.
- [ ] Estructura de módulos de la sección 10.1. En esta fase solo tienen código `shared/`, `usuarios/` y `configuracion/`; los demás módulos no se crean vacíos, se crean en la fase que los necesita.
- [ ] Perfiles `local`, `test`, `staging`, `prod` (BP-17). Todo secreto sale de variables de entorno.
- [ ] `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`, zona JDBC en UTC (BP-13, BP-18).

### T2. Herramientas de calidad — `build:`
- [ ] Spotless con Google Java Format; `spotless:check` en `verify` (BP-22).
- [ ] Checkstyle (se elige Checkstyle en lugar de SpotBugs; se deja constancia en CLAUDE.md) con reglas basadas en Google, adaptadas al formato de Spotless.
- [ ] JaCoCo con regla de cobertura mínima 80 % sobre los paquetes `..dominio..` y `..aplicacion..`; el build falla si no se cumple (BP-28).
- [ ] Pruebas ArchUnit que hacen cumplir la sección 10: los controladores no acceden a repositorios, `dominio` no depende de Spring, `@Transactional` solo en `aplicacion`, sin `@Autowired` en campos, los módulos solo se usan entre sí a través de `api`/`aplicacion` (BP-01, BP-03, BP-05, BP-08).

### T3. Base de datos local y migraciones — `feat:`
- [ ] `docker-compose.yml` con PostgreSQL 16 (volumen persistente, puerto 5432, credenciales de desarrollo tomadas de `.env`).
- [ ] Migraciones Flyway (detalle en la sección 4).

### T4. Módulo `shared` — dominio puro con pruebas primero (AG-04) — `feat:`
- [ ] `Moneda` (enum USD, COP, VES) con su símbolo y los decimales para mostrar (COP 0, USD 2, VES 2).
- [ ] Objeto de valor `Dinero` (record: `BigDecimal monto` + `Moneda`): suma, resta, multiplicación por cantidad, porcentaje, comparación. Opera solo entre la misma moneda (en otro caso, excepción). Sin `double`/`float` (BP-06).
- [ ] `Redondeo`: un único lugar con `HALF_UP` y las escalas: 6 decimales para calcular costos y tasas, 4 para guardar, y las de visualización por moneda (BP-06).
- [ ] `FormatoDinero` (es-CO) para PDF y Excel futuros: `$ 1.250.000` · `US$ 1.939,04` · `Bs 1.234,56` (RNF-04).
- [ ] Fechas: zona `America/Bogota` definida en una sola constante, bean `Clock` inyectable y `FechaNegocio` para obtener el `LocalDate` de hoy en Colombia (BP-13).
- [ ] Serialización JSON: `BigDecimal` como texto (`"19.5000"`), nunca como número (RT-06). El dinero viaja como `{ "monto": "19.5000", "moneda": "USD" }`.

### T5. Errores, auditoría y logs — `feat:`
- [ ] Excepción base `NegocioException` (código de negocio estable, estado HTTP, mensaje en español), con subclases por caso.
- [ ] `@RestControllerAdvice` global que responde Problem Details (RFC 9457) con las propiedades `codigo`, `correlationId` y, para validaciones, `errores: [{campo, mensaje}]` (BP-16, RT-05). También cubre 401/403 de Spring Security, CSRF inválido, 404, 405, JSON mal formado y error interno (sin filtrar detalles técnicos).
- [ ] Códigos de negocio de esta fase: `VALIDACION`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `CSRF_INVALIDO`, `CREDENCIALES_INVALIDAS`, `INGRESO_BLOQUEADO`, `CONTRASENA_ACTUAL_INCORRECTA`, `CONTRASENA_NO_COINCIDE`, `CONTRASENA_DEBIL`, `RECURSO_NO_ENCONTRADO`, `ERROR_INTERNO`.
- [ ] Auditoría: `@MappedSuperclass` `EntidadAuditable` con `created_at`, `created_by`, `updated_at`, `updated_by` y `@Version` (datos maestros), `AuditorAware` que toma el usuario de la sesión (BP-12). Sin Lombok en entidades.
- [ ] Filtro de correlación: lee `X-Correlation-Id` o genera un UUID, lo pone en el MDC y en la respuesta. Patrón de log con el id. Nunca se registran contraseñas ni cookies (BP-21).

### T6. Seguridad y usuarios — `feat:`
- [ ] Entidad `Usuario` (nombre, usuario, contraseña cifrada, activo), repositorio y `UserDetailsService`.
- [ ] Contraseñas con BCrypt (BP-20, RNF-02).
- [ ] Sesión con cookie `HttpOnly`, `Secure` (salvo en `local`), `SameSite=Lax`, guardada en PostgreSQL con Spring Session JDBC para que un redespliegue no cierre la sesión de los usuarios.
- [ ] CSRF activo con `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN` legible por el frontend, cabecera `X-XSRF-TOKEN`), con el dominio de la cookie configurable para que `app.` pueda leer el token emitido por `api.`.
- [ ] CORS limitado al origen del frontend (variable `ITALARM_CORS_ORIGENES`), con credenciales.
- [ ] Límite de intentos de ingreso por usuario (valores propuestos en `docs/preguntas.md`, P-02), guardado en base de datos.
- [ ] Cabeceras de seguridad: HSTS (fuera de `local`), `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, CSP restrictiva para la API.
- [ ] Sin sesión, la API responde 401 en Problem Details, nunca con una redirección ni una página HTML de ingreso.
- [ ] Usuarios iniciales Jose y Victor: los crea un inicializador al arrancar si no existen, con contraseñas tomadas de variables de entorno (`ITALARM_CLAVE_INICIAL_JOSE`, `ITALARM_CLAVE_INICIAL_VICTOR`). Nunca quedan en el repositorio (AG-09). Ver P-01.
- [ ] Casos de uso: `IniciarSesion` (lo resuelve Spring Security, más el registro de intentos), `CerrarSesion`, `ConsultarUsuarioActual` y `CambiarContrasena` (verifica la actual, doble digitación y política mínima).

### T7. Configuración — `feat:`
- [ ] Solo la tabla y la entidad `Configuracion` (fila única) con los valores por defecto del documento. Las pantallas y los endpoints de edición son de la Fase 1 (12.3). Aquí no se expone ningún endpoint.

### T8. OpenAPI y Actuator — `feat:`
- [ ] springdoc: `/v3/api-docs` y `/swagger-ui.html`, con título, versión, descripción del esquema de sesión y CSRF, y el esquema `ProblemDetail` documentado en las respuestas de error.
- [ ] El OpenAPI generado se publica como artefacto de la CI (`openapi.json`) para que italarm-web genere su cliente (RT-08).
- [ ] Actuator: solo `health` expuesto públicamente (con `liveness`/`readiness`); el resto deshabilitado o protegido.

### T9. Contenedor, CI y ambiente de pruebas — `ci:`
- [ ] `Dockerfile` multi-etapa: compila con JDK 21 y ejecuta con JRE 21, con usuario no root, `HEALTHCHECK` y opciones de memoria para ≥ 1 GB.
- [ ] GitHub Actions `ci.yml` en cada Pull Request y en `main`: `./mvnw verify` (Spotless, Checkstyle, pruebas unitarias y de integración con Testcontainers, JaCoCo), construcción de la imagen Docker y publicación del reporte de cobertura y de `openapi.json`.
- [ ] `deploy-staging.yml`: al integrar en `main`, publica la imagen y despliega en el ambiente de pruebas. **Depende del proveedor de hosting (P-04) y del dominio (P-05).**
- [ ] Ambiente de pruebas: base de datos PostgreSQL propia y variables de entorno configuradas en el proveedor.

### T10. Documentación — `docs:`
- [ ] `README.md`: requisitos, cómo levantar PostgreSQL con Docker Compose, cómo ejecutar, probar y abrir Swagger, y variables de entorno.
- [ ] `CLAUDE.md`: comandos (`./mvnw verify`, `./mvnw spotless:apply`, `docker compose up -d`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`), convenciones (módulos, capas, dinero, fechas, errores, migraciones) y decisiones técnicas tomadas (AG-07).
- [ ] `CHANGELOG.md` con la entrada de la Fase 0 (AG-11).

## 3. Endpoints de la Fase 0

Todos bajo `/api/v1` (RT-01). Todo `POST`/`PUT`/`DELETE` requiere el token CSRF.

| Método | Ruta | Acceso | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/v1/sesion/csrf` | Público | Emite la cookie `XSRF-TOKEN` para que el frontend pueda hacer el primer `POST`. | 204 |
| POST | `/api/v1/sesion` | Público | Ingreso. Cuerpo `{ "usuario", "contrasena" }`. Crea la sesión (cookie) y devuelve el usuario actual. | 200 `UsuarioActual` · 400 `VALIDACION` · 401 `CREDENCIALES_INVALIDAS` · 429 `INGRESO_BLOQUEADO` |
| GET | `/api/v1/sesion` | Autenticado | Usuario de la sesión actual; el frontend lo usa para proteger rutas. | 200 `UsuarioActual` · 401 `NO_AUTENTICADO` |
| DELETE | `/api/v1/sesion` | Autenticado | Cierre de sesión: invalida la sesión y borra la cookie. | 204 |
| PUT | `/api/v1/usuarios/actual/contrasena` | Autenticado | Cambio de contraseña (RU-07). Cuerpo `{ "contrasenaActual", "contrasenaNueva", "confirmacion" }`. Mantiene la sesión actual y cierra las demás sesiones del usuario. | 204 · 400 `VALIDACION` / `CONTRASENA_NO_COINCIDE` / `CONTRASENA_DEBIL` · 409 `CONTRASENA_ACTUAL_INCORRECTA` |
| GET | `/actuator/health` | Público | Salud (liveness/readiness) para el proveedor de hosting. | 200 / 503 |
| GET | `/v3/api-docs`, `/swagger-ui.html` | Público en `local`/`staging`; ver P-06 para `prod` | Contrato OpenAPI. | 200 |

`UsuarioActual` = `{ "id", "nombre", "usuario" }`.

Se usa el recurso `/sesion` en lugar de `/auth/login` para mantener los recursos en español y en plural/sustantivo (RT-02). El cambio de contraseña cuelga de `/usuarios`, que ya figura en RT-02.

## 4. Migraciones Flyway

| Script | Contenido |
|---|---|
| `V1__usuarios.sql` | Tabla `usuario`: `id BIGINT GENERATED ALWAYS AS IDENTITY`, `nombre`, `usuario` (UNIQUE, en minúsculas), `contrasena_hash`, `activo`, `intentos_fallidos`, `bloqueado_hasta TIMESTAMPTZ`, `version`, `created_at TIMESTAMPTZ`, `created_by` (FK a `usuario`, admite nulo para lo que crea el sistema), `updated_at`, `updated_by` (FK). No inserta usuarios ni contraseñas: de eso se encarga el inicializador (T6). |
| `V2__configuracion.sql` | Tabla `configuracion` (fila única, `CHECK (id = 1)`): nombre de la empresa `ITALARM`, lema `Instalación de cámaras de seguridad`, NIT, ciudad, teléfono, correo y logo (clave S3) en nulo hasta que ITALARM los entregue (pendiente 6 de la sección 15); `limite_variacion_tasa NUMERIC(7,4)` = 5; `validez_cotizacion_dias` = 15 con `CHECK IN (8,15,30)`; `garantia_mano_obra_meses` = 3 con `CHECK (BETWEEN 1 AND 3)`; `garantia_equipos_meses` = 3; `condiciones_garantia` con el texto de RF-115; `pie_pdf`. Incluye las columnas de auditoría y `version`. Inserta la fila por defecto. |
| `V3__spring_session.sql` | Tablas `spring_session` y `spring_session_attributes` (esquema oficial de Spring Session JDBC para PostgreSQL), versionadas con Flyway y no creadas automáticamente, para cumplir BP-18. |

Las secuencias de consecutivos (BP-11) y demás tablas llegan en las fases que las usan.

## 5. Pruebas de la fase

Criterio de 12.2: prueba de ingreso correcto e incorrecto, prueba de formato de dinero (COP, USD, VES) y CI en verde.

**Unitarias (dominio `shared`, sin Spring)**
- `Dinero`: suma/resta/multiplicación con la misma moneda; error al mezclar monedas; conserva `BigDecimal` exacto (0,1 + 0,2 = 0,3).
- `Redondeo`: `HALF_UP` y escalas de cálculo (6), almacenamiento (4) y visualización.
- `FormatoDinero`: `1250000 COP → "$ 1.250.000"`, `1939.04 USD → "US$ 1.939,04"`, `1234.56 VES → "Bs 1.234,56"`, negativos, cero, redondeo de COP sin decimales.
- `FechaNegocio` con `Clock` fijo: a las 23:30 del 1-oct en Bogotá (04:30 UTC del 2-oct) la fecha de negocio sigue siendo el 1-oct.
- Política de contraseñas y registro de intentos/bloqueo.

**Integración (Testcontainers con PostgreSQL 16 real)**
- Las migraciones aplican desde cero y Hibernate valida el esquema.
- Ingreso correcto → 200, cookie de sesión y `UsuarioActual`; la sesión queda guardada en la base de datos.
- Ingreso incorrecto (clave errada, usuario inexistente, usuario inactivo) → 401 `CREDENCIALES_INVALIDAS`, con el mismo mensaje en los tres casos.
- Bloqueo tras N intentos fallidos → 429 `INGRESO_BLOQUEADO`; desbloqueo al pasar el tiempo (con `Clock` controlado).
- Cierre de sesión → la cookie deja de servir.
- Cambio de contraseña: actual incorrecta, confirmación distinta, contraseña débil, éxito y posterior ingreso con la nueva.
- Inicializador de usuarios: crea a Jose y Victor una sola vez y no sobrescribe sus contraseñas en el siguiente arranque.
- Auditoría: `created_at`/`created_by`/`updated_*` se llenan solos.

**API (MockMvc, BP-27)**
- `POST` sin token CSRF → 403 `CSRF_INVALIDO` en Problem Details.
- Endpoint protegido sin sesión → 401 `NO_AUTENTICADO` (JSON, sin redirección).
- Error de validación → 400 con `errores[]` por campo, mensajes en español y `correlationId`.
- CORS: el origen permitido recibe las cabeceras; otro origen no.
- La cabecera `X-Correlation-Id` se devuelve siempre.
- Las cabeceras de seguridad están presentes.

**Arquitectura (ArchUnit)**: las reglas de T2.

Ninguna prueba se desactiva para pasar la CI (AG-08, BP-29).

## 6. Qué necesita el frontend (italarm-web) de esta fase

- `openapi.json` publicado por la CI para generar el cliente (orval u openapi-typescript).
- Flujo de sesión: `GET /sesion/csrf` → `POST /sesion` → `GET /sesion` al cargar la app → `DELETE /sesion`.
- Peticiones con `credentials: 'include'` y la cabecera `X-XSRF-TOKEN`.
- Errores traducidos por el campo `codigo` del Problem Details.

## 7. Definición de terminado (12.1) aplicada a la Fase 0

- [ ] Pull Requests revisados con la CI en verde (compilación, Spotless, Checkstyle, pruebas, JaCoCo ≥ 80 %).
- [ ] Pruebas de la sección 5 escritas y pasando.
- [ ] Migraciones V1–V3 aplicadas en pruebas sin errores.
- [ ] OpenAPI publicado y cliente de italarm-web regenerado.
- [ ] Desplegado en pruebas y probado el ingreso desde celular y computador (junto con italarm-web).
- [ ] CHANGELOG.md actualizado y lista de verificación para ITALARM:
  1. Abrir la URL de pruebas en el celular y en el computador.
  2. Ingresar como Jose y como Victor.
  3. Intentar con una contraseña errada y ver el mensaje.
  4. Cambiar la contraseña, cerrar sesión y volver a ingresar con la nueva.
  5. Verificar que al cerrar sesión no se puede volver a entrar con el botón Atrás.

## 8. Riesgos y dependencias

- **Hosting y dominio (P-04, P-05)** bloquean solo T9 (despliegue). El resto de la fase puede avanzar en paralelo.
- **Cookies entre `app.` y `api.`**: funcionan porque ambos subdominios son el mismo sitio. Si el ambiente de pruebas usa dominios del proveedor (por ejemplo `*.onrender.com` y `*.pages.dev`), el navegador bloqueará la cookie de sesión. Por eso se recomienda usar subdominios de `italarm.com` también en pruebas.
- **Maven** no viene instalado en el entorno del agente: se usa el Maven Wrapper incluido en el repositorio.
