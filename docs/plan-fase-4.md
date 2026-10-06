# Plan de la Fase 4 — Instalaciones, fotos y garantías (italarm-api)

> Estado: **aprobado por ITALARM el 06/10/2026**, implementado; pendiente de la prueba de ITALARM (lista de la sección 7). Preguntas P-37 a P-45 respondidas: de acuerdo con las propuestas (ver `docs/preguntas.md`).
> Base: `docs/requerimientos.md`, secciones 3.4, 3.8, 3.9, 3.13 a 3.15, 4, 9.4 y 12.6, más las decisiones de `docs/preguntas.md`.
> Alcance: solo el backend (**italarm-api**). Las pantallas se hacen en italarm-web a partir del contrato OpenAPI que deja esta fase.

## 1. Objetivo y entregable

Registrar el trabajo completo de cada instalación: cliente, técnicos, material con seriales, fotos, cobro con mano de obra, utilidad y garantías.

Entregable (12.6): ITALARM registra instalaciones reales con fotos y consulta garantías.

## 2. Qué queda fuera de esta fase

- **Cotizaciones y su conversión en instalación** (Fase 5). La instalación queda preparada para guardar la cotización de origen (RF-74), pero el campo se llena desde la Fase 5.
- **Reportes y exportación a Excel de instalaciones y garantías**: Fase 6.
- **Bloque de garantías por vencer en Inicio** (sección 3.2): Fase 6. En esta fase queda el endpoint de consulta de garantías que usará Inicio.
- **Fotos en el PDF**: el comprobante no incluye fotos (RF-132 no las pide); se ven en el detalle de la instalación.

## 3. Tareas

Orden según AG-03: migración → dominio con pruebas → casos de uso → endpoints → OpenAPI. Las reglas de negocio se escriben primero como prueba del caso CP-xx (AG-04).

### T1. Reglas comunes de ventas e instalaciones — `refactor:`
- [x] El precio sugerido (RN-01, P-28), el descuento (P-30) y el cálculo de totales, costo y utilidad (RN-03, RF-68) pasan del dominio de `ventas` a `shared` (y la preparación del material y la vista previa, al paquete `comercial`), para que las instalaciones apliquen exactamente las mismas reglas sin depender del módulo de ventas (sección 10.1). Las pruebas de ventas siguen pasando sin cambios.
- [x] El cálculo agrega la **mano de obra** (RF-118): total = material + mano de obra − descuento (RN-03). La mano de obra no tiene costo de material, así que suma completa a la utilidad.

### T2. Dominio de la instalación (puro, sin Spring, con TDD) — `feat:`
- [x] **Garantía de mano de obra** (RF-113, P-39): de 1 a 3 meses desde la fecha de la instalación; por defecto, el valor de Configuración (3 meses). CP-20: instalación del 1 de octubre → vigente hasta el 1 de enero.
- [x] **Garantía de equipos** (RF-114): siempre los meses de Configuración desde la fecha de la instalación, guardada en cada serial instalado.
- [x] **Estado de una garantía** (RF-123, P-43): Vigente, Por vencer (30 días o menos antes del vencimiento) o Vencida (después del día de vencimiento).
- [x] **Datos de la instalación** (RF-107, P-37, P-38, P-41): cliente, dirección (obligatoria; se propone la del cliente), fecha (puede ser anterior a hoy, nunca futura), al menos un técnico, descripción del trabajo; material, mano de obra o ambos.
- [x] **Anulación** (RF-72, RF-73): exige motivo; una instalación anulada no se vuelve a anular.

### T3. Persistencia (migración V10) — `feat:`
- [x] Secuencia `seq_instalacion` (I-0001).
- [x] Tablas `instalacion`, `linea_instalacion`, `tecnico_instalacion`, `foto_instalacion` y `reclamo_garantia` (detalle en la sección 5).
- [x] Nuevos tipos de kárdex `INSTALACION` y `ANULACION_INSTALACION`, y de historial de serial `INSTALACION` y `ANULACION_INSTALACION` (nueva migración que amplía los `CHECK`; V7 y V9 no se modifican, AG-10).

### T4. Salidas de inventario por instalación (módulo `inventario`) — `feat:`
- [x] La salida por venta de la Fase 3 se generaliza para instalaciones: bloqueo de productos y seriales en orden de id (BP-08), stock suficiente (`STOCK_INSUFICIENTE`, CP-15), seriales en bodega (`SERIAL_NO_DISPONIBLE`, CP-14), kárdex con el costo vigente y seriales `INSTALADO` con la instalación como documento de salida y su garantía.
- [x] Reingreso por anulación (RF-72): el material vuelve al costo vigente sin cambiar el costo y los seriales vuelven a `EN_BODEGA` sin garantía.

### T5. Instalaciones (módulo `instalaciones`) — `feat:`
- [x] **Vista previa** (RF-108, RF-119), sin guardar nada: por línea, precio sugerido, disponibilidad con el aviso de stock, costo con las tasas de hoy y de la última compra (RF-69) y subtotal en las tres monedas; resumen con material cobrado, mano de obra, descuento, total, costo y utilidad en las tres monedas; vencimiento de las garantías; avisos de tasa; y si se puede guardar.
- [x] **Registrar** (RF-107 a RF-119):
  - cliente, dirección, fecha, técnicos, descripción, moneda, líneas de material (producto, cantidad o seriales, precio), mano de obra, descuento, meses de garantía de mano de obra, condiciones de garantía (por defecto, las de Configuración; RF-115) y observaciones;
  - tasas de la fecha de la instalación (RN-04), costo de cada línea y utilidad (RF-68);
  - descuenta el material del inventario con sus seriales y garantía (RF-109);
  - `Idempotency-Key` (RT-07).
- [x] **Listado** (RF-121): filtros por cliente, técnico, rango de fechas (por defecto, el mes en curso), estado de la garantía de mano de obra e incluir o no las anuladas. Totales del período sin las anuladas: material, mano de obra, total, costo y utilidad por moneda y su suma en USD.
- [x] **Detalle** con material, seriales, fotos por grupo (enlaces firmados), garantías con su estado, reclamos, cobro, tasas, usuario y anulación.
- [x] **Editar datos descriptivos** (RF-122, P-44): descripción, dirección, técnicos, condiciones de garantía y observaciones, con control de versión.
- [x] **Anular** (RF-72, RF-73): `{ motivo }`; devuelve el material y los seriales; la instalación queda visible como anulada. Sus fotos y reclamos se conservan.
- [x] Concurrencia: una venta y una instalación simultáneas de la última unidad; una se guarda y la otra responde `STOCK_INSUFICIENTE`.

### T6. Fotos (RF-110 a RF-112, P-42) — `feat:`
- [x] Subir fotos a un grupo (`ANTES`, `DURANTE`, `DESPUES`) al registrar o después: JPEG, PNG o WebP, máximo 5 MB, validadas por su contenido; el frontend las comprime antes de subirlas (BF-14).
- [x] Máximo 30 fotos por grupo. Cada grupo muestra cuántas tiene.
- [x] Quitar una foto (es un dato descriptivo). El archivo se borra del almacenamiento al confirmar.
- [x] Las fotos se guardan en el almacenamiento (S3 en el hosting, disco en local), nunca en la base de datos.

### T7. Garantías y reclamos (sección 3.14) — `feat:`
- [x] **Consulta de garantías** (RF-123, RF-124): una fila por garantía:
  - mano de obra de cada instalación;
  - cada equipo con serial vendido o instalado.

  Cada fila trae el estado (Vigente, Por vencer, Vencida), el vencimiento, los días que faltan, el cliente y el documento. Filtros: estado, cliente, tipo (`INSTALACION` o `VENTA`) y búsqueda por serial. Las garantías de documentos anulados no aparecen.
- [x] **Reclamos de garantía** (RF-125, P-45): sobre una instalación o un serial, con fecha, descripción del problema y solución. La solución se puede escribir después. Los reclamos no se borran. Un reclamo fuera de garantía se puede registrar y queda marcado como tal.
- [x] El historial del serial (RF-24) muestra la instalación en que se usó, su garantía y sus reclamos.

### T8. Comprobante de instalación en PDF y WhatsApp — `feat:`
- [x] **PDF** con el mismo formato del comprobante de venta (RF-126 a RF-132):
  - "Comprobante de instalación", consecutivo, fecha, cliente y dirección de la instalación;
  - descripción del trabajo y técnicos;
  - ítems de material con seriales y la línea "Mano de obra · instalación y configuración" (RF-129);
  - totales en la moneda del cobro y en las monedas adicionales elegidas (P-34);
  - **garantías**: vencimiento de la mano de obra y de los equipos, y condiciones de la instalación (RF-132);
  - marca "ANULADA" si se anuló.
- [x] **Descargar** y **enlace público** para WhatsApp, iguales a los de la venta (RF-133, RF-134, P-33).

### T9. Clientes y técnicos — `feat:`
- [x] El listado e historial del cliente suman sus instalaciones no anuladas (RF-76, RF-77, P-36).
- [x] Lista de técnicos para el formulario: los usuarios activos (P-37).

### T10. Contrato, documentación y verificación — `docs:`
- [x] Actualizar `contrato/openapi.json` y `docs/guia-frontend.md` (formulario en 4 pasos, fotos, garantías y reclamos).
- [x] CHANGELOG.md, CLAUDE.md (módulo `instalaciones`, decisiones) y README.
- [x] Verificación completa en verde (447 pruebas); falta la prueba de ITALARM en Swagger (sección 7).

## 4. Endpoints

Todos bajo `/api/v1`, con `Authorization: Bearer`. Las creaciones aceptan la cabecera `Idempotency-Key`.

### Instalaciones
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| POST | `/instalaciones/vista-previa` | Precios, disponibilidad, costos, cobro con utilidad y vencimientos de garantía. No guarda. | `TASA_NO_DISPONIBLE`, `VALIDACION` |
| POST | `/instalaciones` | Registrar y descontar el material del inventario. | `STOCK_INSUFICIENTE`, `SERIAL_NO_DISPONIBLE`, `SERIALES_NO_COINCIDEN`, `CANTIDAD_INVALIDA`, `PRODUCTO_INACTIVO`, `INSTALACION_INVALIDA`, `DESCUENTO_INVALIDO`, `CLIENTE_NO_EXISTE`, `TECNICO_NO_EXISTE`, `TASA_NO_DISPONIBLE` |
| GET | `/instalaciones?clienteId=&tecnicoId=&desde=&hasta=&estadoGarantia=&incluirAnuladas=` | Listado paginado con los totales del período. | — |
| GET | `/instalaciones/{id}` | Detalle con material, seriales, fotos, garantías, reclamos y cobro. | `RECURSO_NO_ENCONTRADO` |
| PUT | `/instalaciones/{id}` | Editar descripción, dirección, técnicos, condiciones y observaciones (con `version`). | `MODIFICADO_POR_OTRO_USUARIO` |
| POST | `/instalaciones/{id}/anular` | `{ motivo }`. Devuelve material y seriales. | `INSTALACION_YA_ANULADA` |
| POST | `/instalaciones/{id}/fotos` | `multipart`: `grupo` y `archivo`. | `ARCHIVO_TIPO_NO_PERMITIDO`, `ARCHIVO_DEMASIADO_GRANDE`, `FOTOS_MAXIMAS` |
| DELETE | `/instalaciones/{id}/fotos/{fotoId}` | Quitar una foto. | — |
| GET | `/instalaciones/{id}/comprobante` | Descargar el PDF. | — |
| POST | `/instalaciones/{id}/enlace` | Enlace público del PDF con el mensaje y el enlace `wa.me`. | — |

### Garantías y reclamos
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/garantias?estado=&clienteId=&tipo=&serial=` | Consulta paginada de garantías, ordenada por vencimiento. |
| POST | `/garantias/reclamos` | `{ instalacionId o serialId, fecha, problema, solucion }`. |
| PUT | `/garantias/reclamos/{id}` | Escribir o cambiar la solución (con `version`). |
| GET | `/garantias/reclamos?instalacionId=&serialId=&clienteId=` | Reclamos registrados. |

### Técnicos
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/usuarios/tecnicos` | Usuarios activos que se pueden elegir como técnicos. |

## 5. Migraciones Flyway

| Script | Contenido |
|---|---|
| `V10__instalaciones.sql` | <ul><li>Secuencia `seq_instalacion`.</li><li>`instalacion`: consecutivo único, fecha, cliente y copia de sus datos (P-35), dirección, descripción, moneda, tasas con sus fechas, material, mano de obra, subtotal, descuento (tipo, valor y monto), total, costo, utilidad y sus valores en USD, meses y vencimiento de la garantía de mano de obra, vencimiento de la garantía de equipos, condiciones de garantía, observaciones, monedas adicionales del comprobante, cotización de origen (vacía hasta la Fase 5), estado, motivo, usuario y fecha de anulación, `version`.</li><li>`linea_instalacion`: igual que `linea_venta`.</li><li>`tecnico_instalacion`: instalación y usuario (al menos uno por instalación).</li><li>`foto_instalacion`: instalación, grupo (`ANTES`/`DURANTE`/`DESPUES`), clave del archivo, fecha y usuario.</li><li>`reclamo_garantia`: instalación o serial (uno de los dos), fecha, problema, solución, si estaba en garantía, `version`.</li><li>Amplía los `CHECK` de kárdex, historial de seriales y enlaces de comprobantes con `INSTALACION` y `ANULACION_INSTALACION`.</li><li>`CHECK` de meses de garantía entre 1 y 3, mano de obra y total no negativos, descuento ≤ subtotal; llaves foráneas con índices.</li></ul> |

## 6. Pruebas

**Casos de aceptación de la fase (BP-26)**, primero en rojo y después en verde (AG-04):

| Caso | Prueba | Qué verifica |
|---|---|---|
| CP-15 | `cp15_instalacionCon60mDeCableHabiendo50_noSeGuarda` | `STOCK_INSUFICIENTE` y nada se guarda |
| CP-20 | `cp20_instalacionDel1DeOctubre_garantiaHasta1DeEneroYPorVencerSusUltimos30Dias` | Vigente hasta el 1 de enero; Por vencer desde el 2 de diciembre; Vencida desde el 2 de enero |
| CP-14 | `cp14_serialInstalado_noEstaDisponibleParaVender` | El caso de la Fase 3, ahora con un serial usado en una instalación |

**Pruebas adicionales que pide la fase:**
- **Fotos:** tipos permitidos (JPEG, PNG, WebP), rechazo de un archivo que no es imagen aunque tenga extensión `.jpg`, tamaño máximo, máximo por grupo, agregar después de guardada y quitar.
- **Anulación:** el material y los seriales vuelven a bodega; las fotos y reclamos se conservan.
- **Concurrencia** entre una venta y una instalación de la última unidad.
- **Idempotencia** y **stock igual a la suma del kárdex** con instalaciones y anulaciones.
- **PDF** con mano de obra, seriales, garantías y condiciones; enlace público.
- **Garantías:** estados, filtros, búsqueda por serial, garantías de ventas e instalaciones juntas y exclusión de las anuladas; reclamos dentro y fuera de garantía.
- **Utilidad con mano de obra:** material US$ 100 a costo US$ 60 + mano de obra US$ 50 − descuento US$ 10 → total US$ 140 y utilidad US$ 80.

**Unitarias del dominio:** garantías y sus estados, cálculo con mano de obra, validaciones de la instalación y anulación.

Cobertura mínima de 80 % en dominio y aplicación; ninguna prueba se desactiva (AG-08).

## 7. Definición de terminado (12.1)

- [x] Verificación completa en verde en local (447 pruebas); falta confirmarla en la CI de GitHub.
- [x] CP-15 y CP-20 automatizados y pasando, CP-14 repetido con instalación, más la prueba de fotos.
- [x] Migración V10 aplicada sin errores sobre la base de la Fase 3.
- [x] Contrato y guía del frontend actualizados; CHANGELOG.md actualizado.
- [ ] Lista de verificación para ITALARM en Swagger:
  1. Hacer la vista previa de una instalación con cámaras con serial, cable y mano de obra, y ver el cobro, la utilidad y los vencimientos de garantía.
  2. Registrarla con dos técnicos y una garantía de mano de obra de 2 meses; subir fotos de antes, durante y después.
  3. Intentar usar más cable del que hay y un serial ya vendido.
  4. Descargar el PDF y abrir el enlace público sin sesión.
  5. Consultar las garantías por cliente y por serial, y registrar un reclamo con su solución.
  6. Anular la instalación y ver que el material vuelve a bodega y las fotos siguen ahí.

## 8. Riesgos y dependencias

- **Preguntas P-37 a P-45:** definen columnas de la migración V10 (técnicos, meses de garantía, reclamos). Como una migración aplicada no se modifica (AG-10), hay que responderlas antes de implementar.
- **Almacenamiento de fotos:** en local se guardan en disco; en el hosting se necesita el bucket S3 (P-04, aún sin definir). El código es el mismo; solo cambia la configuración.
- **Refactor de T1:** mueve reglas ya probadas de ventas a `shared`. Se hace primero y en un commit propio, con todas las pruebas de ventas en verde, para no mezclarlo con lo nuevo.
