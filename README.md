# italarm-api

API del sistema de inventario, ventas, cotizaciones e instalaciones de ITALARM.

- Requerimientos: [`docs/requerimientos.md`](docs/requerimientos.md)
- Planes por fase: [`docs/plan-fase-0.md`](docs/plan-fase-0.md), [`docs/plan-fase-1.md`](docs/plan-fase-1.md)
- Contrato de la API: [`contrato/openapi.json`](contrato/openapi.json) y [`docs/guia-frontend.md`](docs/guia-frontend.md)
- Preguntas y decisiones de ITALARM: [`docs/preguntas.md`](docs/preguntas.md)
- Cambios por versión: [`CHANGELOG.md`](CHANGELOG.md)

## Tecnología

Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Flyway · Spring Security · springdoc-openapi · Maven.

## Requisitos

- JDK 21
- Docker (para PostgreSQL local y para las pruebas de integración con Testcontainers)

No hace falta instalar Maven: se usa el wrapper `./mvnw` incluido en el repositorio.

## Ejecutar localmente

1. Copia `.env.example` como `.env` y completa los valores. El archivo `.env` nunca se sube al repositorio.

   | Variable | Para qué sirve |
   |---|---|
   | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Base de datos que crea Docker Compose. |
   | `ITALARM_DB_URL`, `ITALARM_DB_USUARIO`, `ITALARM_DB_CLAVE` | Conexión de la API a PostgreSQL. |
   | `ITALARM_CLAVE_INICIAL` | Contraseña inicial de Jose y Victor. Solo se asigna a usuarios que aún no tienen contraseña. |
   | `ITALARM_CORS_ORIGENES` | Origen del frontend, por ejemplo `http://localhost:5173`. Si son varios, se separan con comas. |
   | `ITALARM_ALMACENAMIENTO` | Dónde se guardan fotos y logo: `disco` (local, por defecto) o `s3` (hosting). |
   | `ITALARM_ALMACENAMIENTO_CARPETA` | Carpeta de archivos en modo disco (por defecto `./almacenamiento`). |
   | `ITALARM_URL_PUBLICA` | Dirección con la que el navegador llega a la API, para los enlaces de archivos (por defecto `http://localhost:8080`). |
   | `ITALARM_ENLACES_CLAVE` | Clave para firmar los enlaces en modo disco. Si falta, se genera al arrancar. |
   | `ITALARM_S3_ENDPOINT`, `ITALARM_S3_REGION`, `ITALARM_S3_BUCKET`, `ITALARM_S3_ACCESS_KEY`, `ITALARM_S3_SECRET_KEY` | Solo en modo `s3`. |
   | `ITALARM_TRM_URL` | Fuente de la TRM. Por defecto, la oficial de datos.gov.co. |

2. Levanta PostgreSQL:

   ```bash
   docker compose up -d
   ```

3. Arranca la API. El perfil `local` es el predeterminado y lee el archivo `.env`:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Abre:
   - Documentación de la API (Swagger): http://localhost:8080/swagger-ui.html
   - Contrato OpenAPI: http://localhost:8080/v3/api-docs
   - Salud: http://localhost:8080/actuator/health

Las migraciones de Flyway se aplican solas al arrancar. La TRM del día se consulta al arrancar y cada mañana; si no hay internet, queda el aviso en `GET /api/v1/tasas/vigentes`.

## Ingresar

```bash
curl -X POST http://localhost:8080/api/v1/sesion \
  -H 'Content-Type: application/json' \
  -d '{"correo":"joseochoa227@gmail.com","contrasena":"<ITALARM_CLAVE_INICIAL>"}'
```

La respuesta trae un `token`. Se envía en cada petición con la cabecera `Authorization: Bearer <token>`. En Swagger se usa el botón **Authorize**.

## Pruebas y calidad

```bash
./mvnw verify           # formato, Checkstyle, pruebas, ArchUnit y cobertura (lo mismo que la CI)
./mvnw test             # solo las pruebas
./mvnw spotless:apply   # corrige el formato
```

- Las pruebas de integración levantan un PostgreSQL real con Testcontainers, así que Docker debe estar corriendo.
- El reporte de cobertura queda en `target/site/jacoco/index.html`.
- El contrato OpenAPI queda en `target/openapi.json`.

## Perfiles

| Perfil | Uso |
|---|---|
| `local` | Desarrollo. Lee `.env`. Es el perfil predeterminado. |
| `test` | Pruebas automáticas (Testcontainers). |
| `staging` | Ambiente de pruebas. |
| `prod` | Producción. Es el perfil de la imagen Docker. |

En `staging` y `prod`, todas las variables se configuran en el proveedor de hosting.

## Imagen Docker

```bash
docker build -t italarm-api .
```

La imagen arranca con el perfil `prod`. Al desplegarla hay que definir `SPRING_PROFILES_ACTIVE` y las variables `ITALARM_*`.
