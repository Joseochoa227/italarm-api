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
usuarios/        sesión (token), cambio de contraseña, gestión de usuarios
configuracion/   datos de la empresa, logo y valores por defecto
documentos/      almacenamiento de archivos (S3 o disco), validación de imágenes, enlaces firmados,
                 generador de PDF y enlaces públicos de comprobantes
catalogo/        categorías, unidades de medida y productos
terceros/        clientes y proveedores
tasas/           TRM automática, tasa del bolívar manual, correcciones e historial
inventario/      kárdex, motor de costo, seriales, ajustes, consultas e inventario inicial
compras/         compras a proveedores, vista previa, anulación y factura
comercial/       (solo aplicacion) preparación del material y vista previa comunes a ventas, instalaciones y cotizaciones
ventas/          ventas de material, utilidad, anulación, comprobante en PDF y enlace para WhatsApp
instalaciones/   instalaciones, técnicos, fotos, garantías de mano de obra, anulación y comprobante en PDF
cotizaciones/    cotizaciones, estados, versiones, vencimiento diario, PDF y datos para convertirlas
garantias/       consulta de garantías (vista SQL `garantia`) y reclamos
cargainicial/    plantilla y lectura del Excel de la carga inicial (usa catalogo, terceros e inventario)
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
- **Vistas**: los servicios de aplicación devuelven records `*Vista` (datos listos para el frontend, con enlaces firmados). Los controladores las exponen tal cual; las entidades nunca salen de la capa de aplicación.
- **Solicitudes**: records `Solicitud*` en `api` con Bean Validation; el `PUT` valida además el grupo `Edicion` (exige `version`).
- **Listados**: `Pagina<T>` con `@ParameterObject Pageable` y `@PageableDefault`. Filtros con `Specification` en `infraestructura`; los `%` y `_` que escribe el usuario se escapan.
- **Restricciones de la base de datos**: cada módulo declara las suyas en un `RestriccionesModulo` (índice o constraint → código de negocio); el manejador global las traduce.
- **Integraciones externas** (BP-14): interfaz en `aplicacion` (`AlmacenamientoArchivos`, `FuenteTrm`) e implementación en `infraestructura`. En las pruebas se usan `FuenteTrmSimulada`, el almacenamiento en disco y `RelojPrueba` (en `soporte/`).
- **Contrato OpenAPI**: `contrato/openapi.json` está versionado y `ApiComunIntegracionTest` falla si no corresponde al código. Tras cambiar un endpoint: `./mvnw test -Dtest=ApiComunIntegracionTest -Dcontrato.actualizar=true`. Los `BigDecimal` se declaran como texto decimal. Los registros anidados se publican con el nombre de su contenedor (`CompraVista.Linea` → `CompraVistaLinea`, `NombresDeEsquema`), para que no choquen los que se llaman igual.
- **Movimientos de inventario**: siempre por `inventario.aplicacion` (`ServicioMovimientos` para compras, ventas e instalaciones, `RegistroAjustes`, `CargaInventario`), que bloquean productos y seriales en orden de id (`OperacionesInventario`) y escriben kárdex, historial de costo y seriales en la misma transacción. Nunca se actualiza `producto.stock` o `costo_actual_usd` desde otro lugar.
- **Creaciones idempotentes**: el servicio público no es transaccional y llama a `ServicioIdempotencia.ejecutar`; la transacción la abre un bean `Registro*` que reserva la clave al empezar y la asocia al documento al final.
- **Pruebas de inventario**: heredan de `soporte.PruebaInventario` (reloj fijo el 01/10/2026, TRM 4.000, bolívar 50 y utilidades para productos, proveedores, clientes y compras).
- **PDF**: los módulos arman un `DocumentoPdf` con los textos ya formateados (`FormatoDinero`) y lo pasan a `GeneradorPdf` (OpenPDF, paquete `org.openpdf`). Para servir un tipo de documento por enlace público, el módulo implementa `FuenteComprobantes`.
- **Movimientos del cliente**: cada módulo con documentos de clientes implementa `terceros.aplicacion.MovimientosCliente`; así `terceros` no depende de `ventas` ni de `instalaciones`. Igual, `garantias` implementa `inventario.aplicacion.ReclamosSerial` y `cotizaciones` implementa `comercial.aplicacion.OrigenCotizacion` (convertir y revertir dentro de la transacción de la venta o instalación).
- **Documentos con material** (ventas, instalaciones y cotizaciones): reglas en `shared.dominio` (`PrecioSugerido`, `Descuento`, `CalculoDocumento`, `Garantia`) y preparación en `comercial.aplicacion` (`PreparacionMaterial`, `VistaPreviaMaterial`). No se duplican en cada módulo.
- **Proxies de Spring**: un método que otro bean llama sobre un servicio con `@Transactional` debe ser `public`; un método de paquete se ejecuta sobre el proxy, con los campos vacíos.
- **Limpieza en pruebas**: `soporte/LimpiezaDatos` deja la base como la dejan las migraciones antes de cada prueba de integración; al agregar tablas, agrégalas ahí.
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
| El contrato OpenAPI está versionado en `contrato/openapi.json`, verificado por prueba, y la CI publica además `target/openapi.json`. | RT-08: italarm-web genera su cliente desde ese archivo (ver `docs/guia-frontend.md`). |
| Almacenamiento `disco` en local (enlaces firmados HMAC servidos por `GET /api/v1/archivos`) y `s3` en el hosting (AWS SDK v2, checksums `WHEN_REQUIRED` por compatibilidad con R2). | Probar sin Docker ni cuenta S3; P-04 sin definir. |
| Imágenes validadas por los primeros bytes (JPEG, PNG, WebP), máximo 5 MB; claves de archivo generadas por el sistema. | BP-20, P-16. |
| Cantidades: Metro hasta 2 decimales, Unidad y Par enteros (`ReglaCantidad`). | P-09. |
| Teléfonos con indicativo internacional, +57 si no se escribe (`Telefono`). Documento de cliente único por tipo y número. | P-10, P-12. |
| Clientes y proveedores no se eliminan. | P-11. |
| TRM desde datos.gov.co (conjunto `32sa-8pi3`), tarea a las 6:00 con reintentos cada 30 min hasta las 12:00 y al arrancar. La oficial reemplaza a la manual del día y queda como corrección automática. | RF-28, P-13, P-18. |
| Cualquier tasa se puede corregir, con doble digitación y alerta de variación. | P-14. |
| Gestión mínima de usuarios: crear, desactivar/activar (cierra sesiones), restablecer contraseña de otro. | P-15. |
| `MovimientosProducto` lo implementa el inventario con el kárdex (`MovimientosProductoKardex`). | P-17 y RF-14. |
| La tabla `producto` tiene dos entidades: `catalogo.Producto` (stock y costo de solo lectura) e `inventario.ProductoInventario` (stock y costo, sin `@Version`). | El inventario mueve el stock sin depender del dominio del catálogo y sin generar conflictos de versión a quien edita el producto. |
| La regla de costo se aplica en el orden en que se registran las compras; la fecha de la factura solo elige las tasas (la del día o la última anterior). | P-19. |
| Una compra se anula solo si es el último movimiento de cada producto y sus seriales siguen en bodega; el costo vuelve al anterior según el historial. | P-23, RF-71. |
| Los ajustes no se anulan; entran al costo vigente salvo que el producto no tenga costo. | P-24, P-25. |
| Seriales en mayúsculas y sin espacios, únicos por producto salvo los anulados (índice parcial). | P-22. |
| Carga inicial: módulo `cargainicial` con la lectura de Excel en infraestructura; cada módulo valida y crea sus filas (`CargaProductos`, `CargaTerceros`, `CargaInventario`). Seriales separados por coma en la fila del producto. | Sección 3.18, P-26. |
| `Idempotency-Key` guardada en la tabla `idempotencia` (usuario, operación, clave → documento). | RT-07, BF-10. |
| La venta bloquea los productos antes de calcular (`bloquearParaSalida`) para guardar el costo vigente en cada línea y la utilidad en la misma transacción. | RF-68, BP-08. |
| La fecha de la venta es siempre hoy; el precio sugerido se convierte con las tasas del día (COP a pesos enteros) y puede cambiarse, incluso por debajo del costo (con aviso). | P-27 a P-29. |
| La venta guarda una copia de los datos del cliente para el comprobante. | P-35. |
| Comprobantes en PDF generados al pedirlos (no se guardan); los enlaces públicos usan un token aleatorio cuyo SHA-256 se guarda en `enlace_comprobante` y vencen a los 30 días. | RF-133, RF-134, P-33. |
| La instalación puede tener fecha anterior a hoy: sus tasas y garantías se toman de esa fecha. Garantía de mano de obra de 1 a 3 meses; la de equipos, la de Configuración, guardada en cada serial. | P-38, P-39, RF-114. |
| Técnicos = usuarios activos, guardados en `tecnico_instalacion`. | P-37. |
| Fotos en el almacenamiento (`instalaciones/{id}/{grupo}-…`), máximo 30 por grupo; el frontend las comprime. | P-42, BF-14. |
| La consulta de garantías lee la vista SQL `garantia` (mano de obra de instalaciones y seriales vendidos o instalados de documentos no anulados) con `NamedParameterJdbcTemplate`. Estado: Por vencer con 30 días o menos; Vencida desde el día siguiente. | RF-123, P-43. |
| Reclamos sobre una instalación o un serial; no se borran y quedan marcados si se registran fuera de garantía. | RF-125, P-45. |
| Máquina de estados de la cotización en `Cotizacion` (dominio); Rechazada y Vencida no se reabren, se duplican. Vencen Borrador y En evaluación desde el día siguiente a `fecha + validez`, con una tarea a las 00:05 y al arrancar. | P-47, P-48, RN-13. |
| Editar una cotización en evaluación sube su número de versión (COT-0001 v2) y guarda la anterior en `version_cotizacion` (JSONB). | RF-88, P-46. |
| La conversión no es un `POST /cotizaciones/{id}/convertir`: `GET /conversion` precarga el formulario y la venta o instalación se guarda con `cotizacionId`, que bloquea y convierte la cotización en la misma transacción. Un índice único parcial impide dos documentos activos por cotización. | RF-93 a RF-96, RN-14; se aparta de RT-03 para que la cotización solo quede Convertida si el documento se guarda. |
