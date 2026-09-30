# Plan de la Fase 0 — Fundaciones (italarm-api)

> Estado: **pendiente de aprobación de ITALARM** (AG-02). No se implementa nada hasta que el plan se apruebe.
> Base: `docs/requerimientos.md`, secciones 9, 10, 12.1, 12.2 y 13, más las respuestas de ITALARM en `docs/preguntas.md` (P-01 a P-07).
> Alcance de este plan: el repositorio **italarm-api** (backend). La parte de frontend de la Fase 0 se planea en `italarm-web/docs/plan-fase-0.md`; aquí solo figura lo que el backend debe entregarle.

## 1. Objetivo y entregable

Dejar lista la base técnica del backend, con el ingreso al sistema funcionando.

Entregable (12.2): Jose y Victor pueden ingresar al sistema en el ambiente de pruebas desde celular y computador y ver el menú vacío. Para eso el backend debe exponer ingreso, cierre de sesión, sesión actual y cambio de contraseña, desplegado en pruebas con su PostgreSQL. El despliegue queda en espera hasta que ITALARM elija proveedor de hosting (P-04).

## 2. Cambios respecto al documento de requerimientos

ITALARM decidió lo siguiente en `docs/preguntas.md`. Estas decisiones prevalecen sobre las secciones 9.1 y 10 y quedarán anotadas en CLAUDE.md como decisiones técnicas:

| Tema | Documento v0.7 | Decisión de ITALARM | Consecuencia técnica |
|---|---|---|---|
| Sesión (9.1, BP-20) | Cookie `HttpOnly` + `Secure` + `SameSite` y protección CSRF | Token guardado en `localStorage` (P-04/P-05) | Token opaco enviado en `Authorization: Bearer`. Sin cookies y sin CSRF. Funciona con cualquier dominio de hosting. |
| Límite de intentos (BP-20) | Obligatorio | Sin límite (P-02) | No se bloquea el usuario. |
| Duración de la sesión | No definida | Sin límite (P-03) | El token no vence; solo se invalida al cerrar sesión o al cambiar la contraseña. |
| Identificador de ingreso | "Usuario" (RU-01) | Correo electrónico (P-01) | Se ingresa con correo y contraseña. |

Riesgos que ITALARM acepta con estas decisiones (se dejan por escrito):
- Un token en `localStorage` lo puede robar cualquier script que logre ejecutarse en la página (XSS). La mitigación es una política CSP estricta en el frontend y no usar HTML sin sanear.
- Sin límite de intentos y con una contraseña inicial compartida y débil, cualquiera que conozca el correo puede probar contraseñas sin freno. **Recomendación: que Jose y Victor cambien la contraseña en su primer ingreso.**
- Un token que no vence sigue sirviendo si se pierde el celular. Para cortarlo basta cambiar la contraseña, porque eso cierra todas las demás sesiones.

## 3. Tareas

El orden sigue AG-03: migración → dominio con pruebas → casos de uso → endpoints → OpenAPI. Cada bloque es una rama y un Pull Request con Conventional Commits (AG-06).

### T0. Orden del repositorio — `docs:`
- [ ] Renombrar `docs/requerimientos.md` a `docs/requerimientos.md`, como pide la nota del propio documento (sección 9.3).
- [ ] Eliminar `docs/test.txt` (archivo vacío).
- [ ] `.gitignore` (Maven, IDE, `.env`), `.editorconfig` y `.env.example`.

### T1. Proyecto base — `feat:`
- [ ] Proyecto Spring Boot 3.5.x / Java 21 con Maven Wrapper (`./mvnw`), `groupId co.italarm`, paquete raíz `co.italarm.api`.
- [ ] Dependencias: web, validation, data-jpa, security, actuator, flyway (+ `flyway-database-postgresql`), postgresql y springdoc-openapi-starter-webmvc-ui. Pruebas: spring-boot-starter-test (JUnit 5, AssertJ, Mockito), spring-security-test, testcontainers (postgresql, junit-jupiter) y ArchUnit.
- [ ] Estructura de módulos de la sección 10.1. En esta fase solo tienen código `shared/`, `usuarios/` y `configuracion/`; los demás módulos se crean en la fase que los necesita.
- [ ] Perfiles `local`, `test`, `staging`, `prod` (BP-17). Los secretos de conexión salen de variables de entorno.
- [ ] `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`, zona JDBC en UTC (BP-13, BP-18).

### T2. Herramientas de calidad — `build:`
- [ ] Spotless con Google Java Format; `spotless:check` en `verify` (BP-22).
- [ ] Checkstyle (se elige Checkstyle en lugar de SpotBugs; se deja constancia en CLAUDE.md) con reglas basadas en Google, adaptadas al formato de Spotless.
- [ ] JaCoCo con regla de cobertura mínima de 80 % sobre los paquetes `..dominio..` y `..aplicacion..`; el build falla si no se cumple (BP-28).
- [ ] Pruebas ArchUnit que hacen cumplir la sección 10 (BP-01, BP-03, BP-05, BP-08):
  - los controladores no acceden a repositorios;
  - `dominio` no depende de Spring;
  - `@Transactional` solo en `aplicacion`;
  - sin `@Autowired` en campos;
  - los módulos solo se usan entre sí a través de `api`/`aplicacion`.

### T3. Base de datos local y migraciones — `feat:`
- [ ] `docker-compose.yml` con PostgreSQL 16 (volumen persistente, puerto 5432, credenciales de desarrollo tomadas de `.env`).
- [ ] Migraciones Flyway (detalle en la sección 5).

### T4. Módulo `shared` — dominio puro con pruebas primero (AG-04) — `feat:`
- [ ] `Moneda` (enum USD, COP, VES) con su símbolo y los decimales para mostrar (COP 0, USD 2, VES 2).
- [ ] Objeto de valor `Dinero` (record: `BigDecimal monto` + `Moneda`): suma, resta, multiplicación por cantidad, porcentaje y comparación. Opera solo entre la misma moneda (en otro caso, excepción). Sin `double`/`float` (BP-06).
- [ ] `Redondeo`: un único lugar con `HALF_UP` y las escalas: 6 decimales para calcular costos y tasas, 4 para guardar, y las de visualización por moneda (BP-06).
- [ ] `FormatoDinero` (es-CO) para los PDF y Excel futuros: `$ 1.250.000` · `US$ 1.939,04` · `Bs 1.234,56` (RNF-04).
- [ ] Fechas: zona `America/Bogota` definida en una sola constante, bean `Clock` inyectable y `FechaNegocio` para obtener el `LocalDate` de hoy en Colombia (BP-13).
- [ ] Serialización JSON: `BigDecimal` como texto (`"19.5000"`), nunca como número (RT-06). El dinero viaja como `{ "monto": "19.5000", "moneda": "USD" }`.

### T5. Errores, auditoría y logs — `feat:`
- [ ] Excepción base `NegocioException` (código de negocio estable, estado HTTP, mensaje en español), con subclases por caso.
- [ ] `@RestControllerAdvice` global que responde Problem Details (RFC 9457) con las propiedades `codigo`, `correlationId` y, en las validaciones, `errores: [{campo, mensaje}]` (BP-16, RT-05). También cubre 401/403 de Spring Security, 404, 405, JSON mal formado y error interno (sin filtrar detalles técnicos).
- [ ] Códigos de negocio de esta fase: `VALIDACION`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `CREDENCIALES_INVALIDAS`, `CONTRASENA_ACTUAL_INCORRECTA`, `CONTRASENA_NO_COINCIDE`, `CONTRASENA_DEBIL`, `RECURSO_NO_ENCONTRADO` y `ERROR_INTERNO`.
- [ ] Auditoría: `@MappedSuperclass` `EntidadAuditable` con `created_at`, `created_by`, `updated_at`, `updated_by` y `@Version` (datos maestros), más un `AuditorAware` que toma el usuario del token (BP-12). Sin Lombok en entidades.
- [ ] Filtro de correlación: lee `X-Correlation-Id` o genera un UUID, lo pone en el MDC y en la respuesta. El patrón de log incluye el id. Nunca se registran contraseñas ni tokens, y la cabecera `Authorization` se enmascara (BP-21).

### T6. Seguridad y usuarios — `feat:`
- [ ] Entidad `Usuario` (nombre, correo único en minúsculas, contraseña cifrada, activo) y su repositorio.
- [ ] Contraseñas con BCrypt (BP-20, RNF-02).
- [ ] **Sesión por token opaco** (decisión P-04/P-05):
  - Al ingresar se genera un token aleatorio de 256 bits (`SecureRandom`, Base64URL).
  - Se devuelve una sola vez en la respuesta del ingreso; el frontend lo guarda en `localStorage`.
  - En la base de datos solo se guarda su hash SHA-256, así que una copia filtrada de la base no permite suplantar a nadie.
  - Se valida en cada petición con un filtro de Spring Security que lee `Authorization: Bearer <token>`.
  - Spring Security en modo sin estado (`STATELESS`), sin `HttpSession`.
  - Se eligió un token opaco en lugar de JWT porque, al no tener vencimiento (P-03), la única forma segura de invalidarlo es poder revocarlo en la base de datos.
- [ ] Sin CSRF: no aplica, porque la API no usa cookies.
- [ ] CORS limitado al origen del frontend (variable `ITALARM_CORS_ORIGENES`), sin credenciales, permitiendo la cabecera `Authorization`.
- [ ] Cabeceras de seguridad: HSTS (fuera de `local`), `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy` y CSP restrictiva para la API.
- [ ] Sin token válido, la API responde 401 en Problem Details, nunca con una redirección ni una página HTML.
- [ ] Sin límite de intentos ni vencimiento de sesión (P-02, P-03).
- [ ] Usuarios iniciales (P-01):

  | Nombre | Correo |
  |---|---|
  | Jose | `joseochoa227@gmail.com` |
  | Victor | `victor8amanuelvd@gmail.com` |

  - La migración `V1` crea las filas sin contraseña.
  - Al arrancar, un inicializador asigna la contraseña inicial a los usuarios que no tienen ninguna, tomándola de la variable `ITALARM_CLAVE_INICIAL`. El valor acordado con ITALARM se configura en el `.env` local (no versionado) y en las variables del hosting; `.env.example` solo trae el nombre de la variable, sin valor.
  - Así el resultado es el que pidió ITALARM, pero la contraseña no queda en el código que se despliega (AG-09).
  - El inicializador nunca sobrescribe una contraseña ya asignada.
- [ ] Política de contraseñas (P-07), aplicada al cambiar la contraseña:
  - al menos una mayúscula, una minúscula, un número y un signo;
  - mínimo 8 caracteres (P-08);
  - distinta de la actual.

  La contraseña inicial acordada no cumple esta política (no tiene signo): se acepta solo como contraseña inicial.
- [ ] Casos de uso: `IniciarSesion`, `CerrarSesion` (revoca el token actual), `ConsultarUsuarioActual` y `CambiarContrasena` (verifica la actual, doble digitación y política; revoca los demás tokens del usuario y conserva el actual).

### T7. Configuración — `feat:`
- [ ] Solo la tabla y la entidad `Configuracion` (fila única) con los valores por defecto del documento. Las pantallas y los endpoints de edición son de la Fase 1 (12.3). Aquí no se expone ningún endpoint.

### T8. OpenAPI y Actuator — `feat:`
- [ ] springdoc: `/v3/api-docs` y `/swagger-ui.html`, **públicos en todos los ambientes, incluida producción (P-06)**. Incluye título, versión, esquema de seguridad `bearerAuth` (con botón "Authorize" en Swagger) y el esquema `ProblemDetail` documentado en las respuestas de error.
- [ ] El OpenAPI generado se publica como artefacto de la CI (`openapi.json`) para que italarm-web genere su cliente (RT-08).
- [ ] Actuator: solo `health` expuesto públicamente (con `liveness`/`readiness`); el resto deshabilitado.

### T9. Contenedor, CI y ambiente de pruebas — `ci:`
- [ ] `Dockerfile` multi-etapa: compila con JDK 21 y ejecuta con JRE 21, con usuario no root, `HEALTHCHECK` y opciones de memoria para ≥ 1 GB. La imagen no depende de ningún proveedor.
- [ ] GitHub Actions `ci.yml` en cada Pull Request y en `main`: `./mvnw verify` (Spotless, Checkstyle, pruebas unitarias y de integración con Testcontainers, JaCoCo), construcción de la imagen Docker y publicación del reporte de cobertura y de `openapi.json`.
- [ ] **En espera de P-04:** `deploy-staging.yml` y la creación del ambiente de pruebas (PostgreSQL propio y variables de entorno). Se hará cuando ITALARM elija proveedor; no bloquea el resto de la fase.

### T10. Documentación — `docs:`
- [ ] `README.md`: requisitos, cómo levantar PostgreSQL con Docker Compose, cómo ejecutar, probar y abrir Swagger, y variables de entorno.
- [ ] `CLAUDE.md`: comandos (`./mvnw verify`, `./mvnw spotless:apply`, `docker compose up -d`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`), convenciones (módulos, capas, dinero, fechas, errores, migraciones) y decisiones técnicas, incluidas las de la sección 2 (AG-07).
- [ ] `CHANGELOG.md` con la entrada de la Fase 0 (AG-11).

## 4. Endpoints de la Fase 0

Todos bajo `/api/v1` (RT-01). Los endpoints autenticados exigen `Authorization: Bearer <token>`.

| Método | Ruta | Acceso | Descripción | Respuestas |
|---|---|---|---|---|
| POST | `/api/v1/sesion` | Público | Ingreso. Cuerpo `{ "correo", "contrasena" }`. Devuelve el token y el usuario. | 200 `{ "token", "usuario": UsuarioActual }` · 400 `VALIDACION` · 401 `CREDENCIALES_INVALIDAS` |
| GET | `/api/v1/sesion` | Autenticado | Usuario del token actual; el frontend lo usa al cargar la app para proteger rutas. | 200 `UsuarioActual` · 401 `NO_AUTENTICADO` |
| DELETE | `/api/v1/sesion` | Autenticado | Cierre de sesión: revoca el token actual. | 204 |
| PUT | `/api/v1/usuarios/actual/contrasena` | Autenticado | Cambio de contraseña (RU-07). Cuerpo `{ "contrasenaActual", "contrasenaNueva", "confirmacion" }`. Conserva el token actual y revoca los demás del usuario. | 204 · 400 `VALIDACION` / `CONTRASENA_NO_COINCIDE` / `CONTRASENA_DEBIL` · 409 `CONTRASENA_ACTUAL_INCORRECTA` |
| GET | `/actuator/health` | Público | Salud (liveness/readiness) para el proveedor de hosting. | 200 / 503 |
| GET | `/v3/api-docs`, `/swagger-ui.html` | Público (P-06) | Contrato OpenAPI. | 200 |

`UsuarioActual` = `{ "id", "nombre", "correo" }`.

Se usa el recurso `/sesion` en lugar de `/auth/login` para mantener los recursos en español (RT-02). El cambio de contraseña cuelga de `/usuarios`, que ya figura en RT-02.

## 5. Migraciones Flyway

| Script | Contenido |
|---|---|
| `V1__usuarios.sql` | Tabla `usuario`: `id BIGINT GENERATED ALWAYS AS IDENTITY`, `nombre`, `correo` (UNIQUE, `CHECK (correo = lower(correo))`), `contrasena_hash` (admite nulo solo hasta que el inicializador la asigna), `activo`, `version`, `created_at TIMESTAMPTZ`, `created_by` (FK a `usuario`, admite nulo para lo que crea el sistema), `updated_at` y `updated_by` (FK). Inserta a Jose y Victor con sus correos, sin contraseña. |
| `V2__configuracion.sql` | Tabla `configuracion` (fila única, `CHECK (id = 1)`) con: <ul><li>nombre de la empresa `ITALARM` y lema `Instalación de cámaras de seguridad`;</li><li>NIT, ciudad, teléfono, correo y logo (clave S3) en nulo hasta que ITALARM los entregue (pendiente 6 de la sección 15);</li><li>`limite_variacion_tasa NUMERIC(7,4)` = 5;</li><li>`validez_cotizacion_dias` = 15 con `CHECK IN (8,15,30)`;</li><li>`garantia_mano_obra_meses` = 3 con `CHECK (BETWEEN 1 AND 3)`;</li><li>`garantia_equipos_meses` = 3;</li><li>`condiciones_garantia` con el texto de RF-115;</li><li>`pie_pdf`.</li></ul>Incluye las columnas de auditoría y `version`, e inserta la fila por defecto. |
| `V3__sesiones.sql` | Tabla `sesion`: `id`, `usuario_id` (FK con índice), `token_hash CHAR(64)` UNIQUE, `creada_en TIMESTAMPTZ`, `ultimo_uso TIMESTAMPTZ`, `revocada_en TIMESTAMPTZ` (nulo = activa), `agente_usuario` (navegador, para identificar el dispositivo). Índice parcial sobre los tokens activos. |

Las secuencias de consecutivos (BP-11) y demás tablas llegan en las fases que las usan.

## 6. Pruebas de la fase

Criterio de 12.2: prueba de ingreso correcto e incorrecto, prueba de formato de dinero (COP, USD, VES) y CI en verde.

**Unitarias (dominio `shared` y `usuarios`, sin Spring)**
- `Dinero`: suma/resta/multiplicación con la misma moneda; error al mezclar monedas; conserva `BigDecimal` exacto (0,1 + 0,2 = 0,3).
- `Redondeo`: `HALF_UP` y escalas de cálculo (6), almacenamiento (4) y visualización.
- `FormatoDinero`: `1250000 COP → "$ 1.250.000"`, `1939.04 USD → "US$ 1.939,04"`, `1234.56 VES → "Bs 1.234,56"`, más negativos, cero y el redondeo de COP sin decimales.
- `FechaNegocio` con `Clock` fijo: a las 23:30 del 1-oct en Bogotá (04:30 UTC del 2-oct) la fecha de negocio sigue siendo el 1-oct.
- `PoliticaContrasena`:
  - rechaza la falta de mayúscula, de minúscula, de número, de signo y una longitud menor a 8;
  - rechaza que sea igual a la actual;
  - acepta `Italarm#2026`.
- Generación de token: longitud, aleatoriedad y que el hash sea distinto del token.

**Integración (Testcontainers con PostgreSQL 16 real)**
- Las migraciones aplican desde cero y Hibernate valida el esquema.
- Ingreso correcto con `joseochoa227@gmail.com` y la contraseña inicial de prueba → 200 con token; en la base de datos solo queda el hash.
- El correo no distingue mayúsculas (`JoseOchoa227@Gmail.com` también ingresa).
- Ingreso incorrecto (clave errada, correo inexistente, usuario inactivo) → 401 `CREDENCIALES_INVALIDAS`, con el mismo mensaje en los tres casos.
- `GET /sesion` con token válido → 200; con token revocado, inventado o ausente → 401.
- Cierre de sesión → ese token deja de servir y los de otros dispositivos siguen funcionando.
- Cambio de contraseña:
  - actual incorrecta, confirmación distinta y contraseña débil se rechazan;
  - al cambiarla, el token actual sigue sirviendo y los demás quedan revocados;
  - después se ingresa con la nueva.
- Inicializador: asigna la contraseña inicial a Jose y Victor en el primer arranque y no la sobrescribe en el siguiente, ni después de un cambio de contraseña.
- Auditoría: `created_at`/`created_by`/`updated_*` se llenan solos.

**API (MockMvc, BP-27)**
- Endpoint protegido sin token → 401 `NO_AUTENTICADO` (JSON, sin redirección).
- Error de validación → 400 con `errores[]` por campo, mensajes en español y `correlationId`.
- CORS: el origen permitido recibe las cabeceras; otro origen no.
- La cabecera `X-Correlation-Id` se devuelve siempre.
- Las cabeceras de seguridad están presentes.
- Los logs no contienen la contraseña ni el token.

**Arquitectura (ArchUnit)**: las reglas de T2.

Ninguna prueba se desactiva para pasar la CI (AG-08, BP-29).

## 7. Qué necesita el frontend (italarm-web) de esta fase

- `openapi.json` publicado por la CI para generar el cliente (orval u openapi-typescript).
- Flujo de sesión:
  1. `POST /sesion` y guardar el token en `localStorage`.
  2. Enviar `Authorization: Bearer <token>` en cada petición.
  3. Al cargar la app, llamar a `GET /sesion`; si responde 401, borrar el token y mostrar el ingreso.
  4. Al cerrar sesión, llamar a `DELETE /sesion` y borrar el token.
- Errores traducidos por el campo `codigo` del Problem Details.
- CSP estricta en el frontend para reducir el riesgo de robo del token (sección 2).

## 8. Definición de terminado (12.1) aplicada a la Fase 0

- [ ] Pull Requests revisados con la CI en verde (compilación, Spotless, Checkstyle, pruebas, JaCoCo ≥ 80 %).
- [ ] Pruebas de la sección 6 escritas y pasando.
- [ ] Migraciones V1–V3 aplicadas en pruebas sin errores. *(Requiere P-04.)*
- [ ] OpenAPI publicado y cliente de italarm-web regenerado.
- [ ] Desplegado en pruebas y probado el ingreso desde celular y computador, junto con italarm-web. *(Requiere P-04.)*
- [ ] CHANGELOG.md actualizado y lista de verificación para ITALARM:
  1. Abrir la URL de pruebas en el celular y en el computador.
  2. Ingresar como Jose y como Victor con la contraseña inicial acordada.
  3. Intentar con una contraseña errada y ver el mensaje.
  4. Cambiar la contraseña (con mayúscula, minúscula, número y signo), cerrar sesión y volver a ingresar con la nueva.
  5. Verificar que la sesión del otro dispositivo se cerró al cambiar la contraseña.

## 9. Riesgos y dependencias

- **Proveedor de hosting (P-04)**: bloquea solo el despliegue a pruebas (T9) y, por tanto, el cierre formal de la fase. El resto avanza en paralelo.
- **Seguridad**: los riesgos aceptados de la sección 2.
- **Maven** no viene instalado en el entorno del agente: se usa el Maven Wrapper incluido en el repositorio.
