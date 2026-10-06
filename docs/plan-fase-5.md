# Plan de la Fase 5 — Cotizaciones (italarm-api)

> Estado: **aprobado por ITALARM el 06/10/2026**, implementado; pendiente de la prueba de ITALARM (lista de la sección 7). Preguntas P-46 a P-55 respondidas: de acuerdo con las propuestas (ver `docs/preguntas.md`).
> Base: `docs/requerimientos.md`, secciones 3.8, 3.9, 3.11, 3.15, 4 (RN-11, RN-13, RN-14) y 12.7, más las decisiones de `docs/preguntas.md`.
> Alcance: solo el backend (**italarm-api**). Las pantallas se hacen en italarm-web a partir del contrato OpenAPI que deja esta fase.

## 1. Objetivo y entregable

Objetivo: cotizar, hacer seguimiento y convertir la cotización en venta o instalación sin volver a digitar.

Entregable (12.7): ITALARM gestiona todas sus cotizaciones en el sistema.

## 2. Qué queda fuera de esta fase

- **Bloque de cotizaciones en evaluación en Inicio** (RF-12 y RF-91): la pantalla de Inicio es de la Fase 6. En esta fase queda el filtro `porVencer` del listado, que Inicio usará.
- **Reporte de cotizaciones por estado y porcentaje de aprobadas** (RF-142) y su exportación a Excel: Fase 6.
- **Vista previa del PDF en vivo** (RF-85): la dibuja el frontend con los datos de la vista previa. El backend ofrece además el PDF de una cotización guardada, incluso en borrador.
- **Reserva de material**: las cotizaciones no apartan ni descuentan inventario (RF-86, RN-11).

## 3. Tareas

Orden según AG-03: migración → dominio con pruebas → casos de uso → endpoints → OpenAPI. Las reglas de negocio se escriben primero como prueba del caso CP-xx (AG-04).

### T1. Dominio de la cotización (puro, sin Spring, con TDD) — `feat:`

- [x] **Máquina de estados** (sección 3.11, BP-05). Transiciones permitidas; cualquier otra lanza `TRANSICION_NO_PERMITIDA` (422) con el estado actual en el mensaje:

  | Desde | Acción | Hacia |
  |---|---|---|
  | Borrador | enviar (WhatsApp, enlace o marcar enviada) | En evaluación |
  | Borrador, En evaluación | Cliente aprobó | Aprobada |
  | Borrador, En evaluación, Aprobada | rechazar (motivo opcional) | Rechazada |
  | Borrador, En evaluación | pasa la fecha de validez (tarea diaria) | Vencida |
  | Aprobada | se guarda la venta o instalación | Convertida |
  | Convertida | se anula la venta o instalación generada | Aprobada (RF-74) |

  - Una cotización Convertida no se vuelve a convertir (RN-14).
  - Rechazada y Vencida son finales: solo se pueden duplicar (P-48).
- [x] **Validez y vencimiento** (RF-83, RN-13): validez de 8, 15 o 30 días (por defecto, la de Configuración).
  - Vence el día `fecha + validez`.
  - Pasa a Vencida desde el día siguiente (P-47). CP-21: creada el 1 de octubre con 15 días → vence el 16 → el 17 queda Vencida.
  - Días para vencer = vencimiento − hoy (RF-90). "Por vencer" = En evaluación con 3 días o menos (RF-91).
- [x] **Datos** (RF-80, RF-83):
  - tipo (Instalación o Venta de material), cliente, moneda y líneas de material (producto, cantidad, precio unitario; sin seriales);
  - si es de instalación: mano de obra y descripción del trabajo;
  - descuento (RF-82, mismas reglas que P-30 y P-40) y observaciones.
  - Una venta de material exige al menos una línea. Una instalación exige material o mano de obra mayor que 0, como en P-41.
- [x] **Edición** (RF-88, P-46):
  - en Borrador se edita libremente;
  - en En evaluación, al editar se guarda una nueva versión (v2, v3…) y se conserva una copia de la anterior;
  - en los demás estados no se edita.
- [x] Cálculo con las reglas comunes de `shared.dominio` (`PrecioSugerido`, `Descuento`, `CalculoDocumento`), sin duplicarlas.

### T2. Persistencia (migración V11) — `feat:`

- [x] Secuencia `seq_cotizacion` y el caso `COTIZACION` en `GeneradorConsecutivos` (COT-0001, RN-09).
- [x] Tablas `cotizacion`, `linea_cotizacion` y `version_cotizacion` (detalle en la sección 5).
- [x] Llaves foráneas de `venta.cotizacion_id` e `instalacion.cotizacion_id` hacia `cotizacion`, con índices.
  - Índice único parcial: una cotización tiene como máximo un documento generado activo (segunda línea de defensa de RN-14).

### T3. Cotizaciones (nuevo módulo `cotizaciones`) — `feat:`

- [x] **Vista previa** (RF-81, RF-84, CP-09), sin guardar nada. Reutiliza `comercial.aplicacion` (`PreparacionMaterial`, `VistaPreviaMaterial`). Devuelve:
  - por línea: precio sugerido según el tipo de cliente; disponibilidad solo informativa (RF-86: el stock no impide guardar la cotización); costo con las tasas de hoy y de la última compra; subtotal;
  - resumen: material, mano de obra, descuento, total, costo y utilidad estimada en las tres monedas;
  - fecha de vencimiento calculada y avisos de tasa.
- [x] **Registrar** en Borrador, con las tasas de hoy (P-49), el costo vigente de cada línea (para el aviso de RF-96) y `Idempotency-Key` (RT-07). No toca el inventario (RN-11, CP-26).
- [x] **Listado** (RF-89, RF-90):
  - filtros por estado, cliente, tipo, rango de fechas y `porVencer`;
  - cada fila trae consecutivo y versión, cliente, tipo, fecha, total, estado, vencimiento, días para vencer y si está por vencer.
- [x] **Detalle**:
  - líneas, cobro, tasas, versiones anteriores, motivo de rechazo, usuario y fechas;
  - el documento generado (tipo, id y consecutivo) para ir de una a otra (RF-95).
- [x] **Editar** (`PUT`, con `version`): en Borrador, o como nueva versión en En evaluación (P-46).
- [x] **Acciones de estado**: `enviar`, `aprobar` y `rechazar` (`{ motivo, detalle }`, P-51).
- [x] **Duplicar** (RF-87): crea una cotización nueva en Borrador con hoy como fecha, las tasas de hoy y el mismo cliente, tipo, líneas, precios, mano de obra y descripción (P-52).
- [x] **Seguimiento por WhatsApp** (RF-92): enlace `wa.me` con el número del cliente y un mensaje de seguimiento (P-53).
- [x] **Tarea diaria de vencimiento** (RN-13, CP-21): todos los días a las 00:05 de Bogotá, y al arrancar la API. Pasa a Vencida las cotizaciones en Borrador o En evaluación cuyo vencimiento ya pasó (P-47). Usa el `Clock` inyectado, para probarla con `RelojPrueba`.
- [x] Movimientos del cliente: sus cotizaciones se consultan aparte con el filtro `clienteId` (P-36).

### T4. Conversión en venta o instalación — `feat:`

- [x] **Datos para convertir** (RF-93, RF-94, RF-96, CP-22): `GET /cotizaciones/{id}/conversion`, solo para cotizaciones Aprobadas. Devuelve:
  - el formulario de venta o de instalación precargado: cliente, moneda, productos, cantidades, precios cotizados, descuento, mano de obra, descripción y la dirección del cliente;
  - los avisos de lo que cambió desde la cotización: precio sugerido distinto del cotizado, costo actual distinto del cotizado (con la utilidad que queda) y stock insuficiente ("quedan N").
  - El usuario completa en el frontend lo que falta: seriales, técnicos, fecha, fotos y garantía.
- [x] **Guardar la conversión** (RF-95, RF-96, CP-22, CP-23):
  - `POST /ventas` y `POST /instalaciones` aceptan el campo opcional `cotizacionId`;
  - en la misma transacción se valida que la cotización esté Aprobada, que sea del mismo tipo y del mismo cliente (P-54), y se pasa a Convertida enlazada al documento;
  - si no hay stock, se responde `STOCK_INSUFICIENTE`, la cotización sigue Aprobada y nada se guarda (CP-23);
  - los precios que lleguen en la solicitud son los que se guardan: los cotizados, salvo que el usuario los cambie (P-54).
- [x] **Anulación del documento generado** (RF-74, CP-24): al anular la venta o instalación, la cotización vuelve a Aprobada en la misma transacción.
- [x] Para que `ventas` e `instalaciones` no dependan del dominio de `cotizaciones` (sección 10.1), la interfaz `comercial.aplicacion.OrigenCotizacion` (`convertir`, `revertir`) la implementa el módulo de cotizaciones, igual que `MovimientosCliente`.
- [x] Sobre RT-03: la acción "convertir" no guarda nada por sí sola, porque la cotización solo pasa a Convertida cuando se guarda la venta o la instalación. Por eso se expone como `GET /cotizaciones/{id}/conversion` más el `cotizacionId` en el registro, en lugar de un `POST /cotizaciones/{id}/convertir`.

### T5. PDF de la cotización y enlace — `feat:`

- [x] PDF "Cotización" (RF-126 a RF-131):
  - encabezado con COT-0001 (y la versión si es v2 o más), fecha y "Válida hasta";
  - datos del cliente; ítems con la mano de obra como una línea más; descuento y totales;
  - otras monedas elegidas por el usuario, con la tasa y su fecha (como P-34);
  - condiciones de Configuración y el pie;
  - marca de agua "VENCIDA" o "RECHAZADA" cuando corresponda.
- [x] Descargar el PDF (`GET`) no cambia el estado.
  - Crear el enlace para WhatsApp (`POST /enlace`) pasa la cotización de Borrador a En evaluación (sección 3.11).
  - Si el usuario solo descarga el PDF para enviarlo por su cuenta, el frontend llama a `POST /enviar` (P-50).
- [x] El enlace público `GET /api/v1/comprobantes/{token}` sirve también cotizaciones (`FuenteComprobantes`), con vencimiento de 30 días (P-33).

### T6. Documentación — `docs:`

- [x] Contrato `contrato/openapi.json` regenerado, guía del frontend (sección de cotizaciones y conversión), CHANGELOG.md y CLAUDE.md.

## 4. Endpoints

Todos bajo `/api/v1`, con `Authorization: Bearer`. Las creaciones aceptan la cabecera `Idempotency-Key`.

### Cotizaciones

| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| POST | `/cotizaciones/vista-previa` | Precios, disponibilidad, costos con las dos tasas, cobro con utilidad y vencimiento. No guarda. | `TASA_NO_DISPONIBLE`, `VALIDACION` |
| POST | `/cotizaciones` | Registrar en Borrador. | `COTIZACION_INVALIDA`, `DESCUENTO_INVALIDO`, `CANTIDAD_INVALIDA`, `PRODUCTO_INACTIVO`, `CLIENTE_NO_EXISTE`, `TASA_NO_DISPONIBLE` |
| GET | `/cotizaciones?estado=&clienteId=&tipo=&desde=&hasta=&porVencer=` | Listado paginado. | — |
| GET | `/cotizaciones/{id}` | Detalle con líneas, versiones y documento generado. | `RECURSO_NO_ENCONTRADO` |
| PUT | `/cotizaciones/{id}` | Editar (Borrador) o guardar nueva versión (En evaluación), con `version`. | `TRANSICION_NO_PERMITIDA`, `MODIFICADO_POR_OTRO_USUARIO` |
| POST | `/cotizaciones/{id}/enviar` | Marcar como enviada. | `TRANSICION_NO_PERMITIDA` |
| POST | `/cotizaciones/{id}/aprobar` | Cliente aprobó. | `TRANSICION_NO_PERMITIDA` |
| POST | `/cotizaciones/{id}/rechazar` | `{ motivo, detalle }`, ambos opcionales. | `TRANSICION_NO_PERMITIDA` |
| POST | `/cotizaciones/{id}/duplicar` | Nueva cotización en Borrador. | `TASA_NO_DISPONIBLE` |
| GET | `/cotizaciones/{id}/conversion` | Formulario de venta o instalación precargado, con avisos de cambios. | `TRANSICION_NO_PERMITIDA` |
| GET | `/cotizaciones/{id}/comprobante` | Descargar el PDF. | — |
| POST | `/cotizaciones/{id}/enlace` | Enlace público del PDF con el mensaje y el enlace `wa.me`; la pasa a En evaluación si estaba en Borrador. | — |
| GET | `/cotizaciones/{id}/seguimiento` | Enlace `wa.me` con el mensaje de seguimiento. | — |

### Cambios en ventas e instalaciones

| Método | Ruta | Cambio |
|---|---|---|
| POST | `/ventas`, `/instalaciones` | Campo opcional `cotizacionId`. Nuevos errores: `COTIZACION_NO_CONVERTIBLE` (no está Aprobada, es de otro tipo o de otro cliente). |
| POST | `/ventas/{id}/anular`, `/instalaciones/{id}/anular` | Si el documento venía de una cotización, esta vuelve a Aprobada. |
| GET | `/ventas/{id}`, `/instalaciones/{id}` | Incluyen la cotización de origen (id y consecutivo). |

## 5. Migraciones Flyway

| Script | Contenido |
|---|---|
| `V11__cotizaciones.sql` | <ul><li>Secuencia `seq_cotizacion`.</li><li>`cotizacion`:<ul><li>consecutivo único, número de versión, tipo (`VENTA`/`INSTALACION`) y fecha;</li><li>validez en días (8, 15 o 30) y vencimiento;</li><li>cliente y copia de sus datos (P-35);</li><li>moneda, tasas con sus fechas, descripción, mano de obra, subtotal, descuento (tipo, valor y monto), total, costo y utilidad estimados y sus valores en USD;</li><li>observaciones y monedas adicionales del PDF;</li><li>estado, fecha de envío, de aprobación y de rechazo, motivo y detalle del rechazo;</li><li>tipo e id del documento generado, `version`.</li></ul></li><li>`linea_cotizacion`: producto, cantidad, precio unitario, costo unitario en USD al cotizar y subtotal.</li><li>`version_cotizacion`: cotización, número de versión, fecha, usuario y copia de la versión anterior (líneas y cobro, en `JSONB`).</li><li>`CHECK`:<ul><li>estados y tipos válidos;</li><li>validez de 8, 15 o 30 días;</li><li>mano de obra y total no negativos, descuento ≤ subtotal;</li><li>una cotización de venta sin mano de obra.</li></ul></li><li>Llaves foráneas con índices, incluidas `venta.cotizacion_id` e `instalacion.cotizacion_id`.</li><li>Índice para la tarea de vencimiento (`estado`, `vencimiento`).</li></ul> |

## 6. Pruebas

**Casos de aceptación de la fase (BP-26)**, primero en rojo y después en verde (AG-04):

| Caso | Prueba | Qué verifica |
|---|---|---|
| CP-09 | `cp09_costoDe19_50ConTrm4200YCompraA4000_muestra81900Y78000` | La vista previa de la cotización muestra el costo a la tasa de hoy y a la de la última compra |
| CP-21 | `cp21_cotizacionEnviadaCon15DiasYPasan16_quedaVencida` | La tarea diaria la vence; el día 15 todavía no |
| CP-22 | `cp22_convertirCotizacionAprobadaEnInstalacion_quedaConvertidaYEnlazada` | 4 cámaras y 120 m de cable: el formulario precargado trae cliente, ítems, precios y mano de obra; al guardar, Convertida y enlazada en ambos sentidos |
| CP-23 | `cp23_convertir4CamarasHabiendo3_noSeGuarda` | `STOCK_INSUFICIENTE`; la cotización sigue Aprobada y nada se guarda |
| CP-24 | `cp24_anularVentaQueVieneDeCotizacion_cotizacionVuelveAAprobada` | Además, se puede convertir de nuevo |
| CP-26 | `cp26_cotizacionPor4De5Camaras_noDescuentaStock` | El stock sigue en 5 y se pueden vender las 5 |

**Pruebas adicionales que pide la fase:**
- **Transiciones no permitidas** (unitarias, una por combinación): aprobar una Rechazada, Vencida o Convertida; enviar una Aprobada; convertir una Convertida (RN-14) o una que no está Aprobada; editar una Aprobada, Rechazada, Vencida o Convertida; vencer una Aprobada.
- **Versiones** (RF-88): editar una En evaluación crea la v2, conserva la v1 y recalcula el vencimiento.
- **Duplicar**: nuevo consecutivo, Borrador, tasas de hoy.
- **Conversión**: de una cotización de venta a venta y de instalación a instalación; rechazo por tipo o cliente distintos; avisos de precio, costo y stock cambiados; idempotencia.
- **PDF** con validez, mano de obra, descuento y marca de agua; enlace público; la creación del enlace pasa a En evaluación.

**Unitarias del dominio:** máquina de estados, vencimiento y días para vencer, validaciones de datos y versiones.

Cobertura mínima de 80 % en dominio y aplicación; ninguna prueba se desactiva (AG-08).

## 7. Definición de terminado (12.1)

- [x] Verificación completa en verde en local (489 pruebas); falta confirmarla en la CI de GitHub.
- [x] CP-09, CP-21, CP-22, CP-23, CP-24 y CP-26 automatizados y pasando, más las pruebas de transiciones no permitidas.
- [x] Migración V11 aplicada sin errores sobre la base de la Fase 4.
- [x] Contrato y guía del frontend actualizados; CHANGELOG.md actualizado.
- [ ] Lista de verificación para ITALARM en Swagger:
  1. Hacer la vista previa de una cotización de instalación con cámaras, cable y mano de obra, y ver los costos a la tasa de hoy y a la de compra.
  2. Guardarla, descargar el PDF y crear el enlace de WhatsApp: queda En evaluación.
  3. Editarla y ver la v2; duplicarla.
  4. Marcarla como aprobada, pedir los datos de conversión y registrar la instalación con `cotizacionId`: la cotización queda Convertida.
  5. Anular esa instalación y ver que la cotización vuelve a Aprobada.
  6. Rechazar otra cotización con motivo, e intentar aprobarla después.

## 8. Riesgos y dependencias

- **Preguntas P-46 a P-55:** definen columnas de la migración V11 (versiones, motivos de rechazo) y las transiciones. Como una migración aplicada no se modifica (AG-10), hay que responderlas antes de implementar.
- **Cambios en ventas e instalaciones:** el registro y la anulación de las fases 3 y 4 ganan el enlace con la cotización. Las pruebas existentes deben seguir pasando sin cambios.
- **Tarea de vencimiento:** si la API está apagada a las 00:05, las cotizaciones se vencen al arrancar. Mientras tanto, el listado calcula los días para vencer con la fecha de hoy, así que nunca muestra datos engañosos.
