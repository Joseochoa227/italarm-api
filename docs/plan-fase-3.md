# Plan de la Fase 3 — Ventas, comprobantes y anulaciones (italarm-api)

> Estado: **aprobado por ITALARM el 05/10/2026**, en implementación. Preguntas P-27 a P-36 respondidas: de acuerdo con las propuestas (ver `docs/preguntas.md`).
> Base: `docs/requerimientos.md`, secciones 3.4, 3.8, 3.9, 3.10, 3.12, 3.15, 4, 9.4 y 12.5, más las decisiones de `docs/preguntas.md`.
> Alcance: solo el backend (**italarm-api**). Las pantallas se hacen en italarm-web a partir del contrato OpenAPI que deja esta fase.

## 1. Objetivo y entregable

Vender material con el precio según el tipo de cliente, controlando stock y seriales, y compartir el comprobante por WhatsApp.

Entregable (12.5): ITALARM registra ventas reales y envía comprobantes por WhatsApp.

## 2. Qué queda fuera de esta fase

- **Instalaciones** y su comprobante (Fase 4). La venta reutilizará los mismos pasos de salida de inventario que usarán las instalaciones.
- **Consulta de garantías y reclamos** (RF-123 a RF-125): Fase 4. En esta fase cada serial vendido queda con su fecha de vencimiento, visible en el historial del serial (RF-24).
- **Cotizaciones y su conversión en venta** (Fase 5). La venta queda preparada para guardar la cotización de origen, pero el campo se llena desde la Fase 5 (RF-74, CP-24).
- **Reportes y exportación a Excel de ventas**: Fase 6.

## 3. Tareas

Orden según AG-03: migración → dominio con pruebas → casos de uso → endpoints → OpenAPI. Las reglas de negocio se escriben primero como prueba del caso CP-xx (AG-04).

### T1. Dominio de la venta (puro, sin Spring, con TDD) — `feat:`
- [ ] **Precio sugerido por línea** (RN-01, RF-78): precio instalador o cliente final según el tipo de cliente, convertido a la moneda de la venta con las tasas del día (P-28). Se puede cambiar a mano en cada línea (P-29).
- [ ] **Totales** (RF-100, RN-03, RN-12):
  - subtotal = suma de cantidad × precio;
  - descuento libre en porcentaje o en valor, nunca mayor que el subtotal (P-30);
  - total = subtotal − descuento;
  - costo del material = suma de cantidad × costo vigente en USD al momento de la salida (RF-68), convertido a la moneda de la venta con la tasa guardada;
  - utilidad = total − costo; porcentaje = utilidad ÷ total (CP-27).
- [ ] **Garantía de equipos** (RF-23, RN-10): cada serial vendido vence a los N meses de la fecha de la venta, con N de la configuración (por defecto 3; CP-25: 15 de octubre → 15 de enero).
- [ ] **Anulación** (RF-72, RF-73, P-31): exige motivo; una venta anulada no se vuelve a anular.

### T2. Persistencia (migración V9) — `feat:`
- [ ] Secuencia `seq_venta` (V-0001).
- [ ] Tablas `venta` y `linea_venta` (detalle en la sección 5).
- [ ] La venta guarda una copia de los datos del cliente que salen en el comprobante (P-35).
- [ ] Nuevos tipos de kárdex `VENTA` y `ANULACION_VENTA`, y de historial de serial `VENTA` y `ANULACION_VENTA` (nueva migración que amplía los `CHECK` de V7; V7 no se modifica, AG-10).

### T3. Salidas de inventario (módulo `inventario`) — `feat:`
- [ ] **Salida por venta**, en la misma transacción de la venta:
  - bloquea los productos y seriales en orden de id (BP-08);
  - exige stock suficiente (`STOCK_INSUFICIENTE`, RF-65) y seriales en bodega (`SERIAL_NO_DISPONIBLE`, CP-14);
  - la cantidad de una línea con serial es la cantidad de seriales elegidos (RF-21);
  - escribe el kárdex con el costo vigente;
  - deja los seriales `VENDIDO`, con la venta como documento de salida y su vencimiento de garantía.
- [ ] **Reingreso por anulación** (RF-72): el material vuelve al costo vigente sin cambiar el costo del producto (como un ajuste de entrada, RN-06), y los seriales vuelven a `EN_BODEGA` sin garantía (CP-18).
- [ ] Estos pasos quedan en `inventario.aplicacion` para que la Fase 4 los reutilice en las instalaciones.

### T4. Ventas (módulo `ventas`) — `feat:`
- [ ] **Vista previa** (RF-99 a RF-101, RF-69, BF-06), sin guardar nada. Por línea muestra:
  - precio sugerido;
  - cantidad disponible ("hay 24 und") y el aviso "Stock insuficiente · quedan N" si no alcanza (aviso, no error, para que el frontend bloquee Guardar);
  - costo en USD y su equivalente en COP y VES con las tasas de hoy y con las de la última compra del producto (P-32, CP-09);
  - subtotal en las tres monedas.

  Además devuelve el resumen (subtotal, descuento, total, costo, utilidad y porcentaje) en las tres monedas, las tasas que se guardarán y los avisos de tasa (RF-33, CP-12).
- [ ] **Registrar** (RF-97, RF-98, RF-102, RF-103):
  - cliente, moneda, líneas (producto, cantidad o seriales, precio), descuento y observaciones;
  - fecha de la venta: hoy (P-27);
  - producto inactivo: `PRODUCTO_INACTIVO`; producto repetido en dos líneas: `VENTA_PRODUCTO_REPETIDO`;
  - guarda las tasas del día (RN-04), el costo de cada línea y la utilidad (RF-68);
  - `Idempotency-Key` (RT-07).
- [ ] **Listado** (RF-105): filtros por cliente, producto, rango de fechas (por defecto, el mes en curso) e incluir o no las anuladas. Totales del período sin las anuladas (RF-73): vendido, costo y utilidad por moneda y su suma en USD.
- [ ] **Detalle**: líneas con seriales y garantía, resumen, tasas, usuario y anulación.
- [ ] **Editar datos descriptivos** (RF-70): observaciones y monedas adicionales del comprobante (P-34). Con control de versión.
- [ ] **Anular** (RF-72, RF-73, RF-106): `{ motivo }`; devuelve el material y los seriales; la venta queda visible como anulada con motivo, usuario y fecha.
- [ ] Concurrencia (BP-25): dos ventas simultáneas de la última unidad; una se guarda y la otra responde `STOCK_INSUFICIENTE`.

### T5. Comprobante de venta en PDF y WhatsApp — `feat:`
- [ ] **PDF con OpenPDF** (sección 9.1, RF-126 a RF-131):
  - encabezado con logo, nombre, lema, ciudad y teléfono de la empresa (Configuración); a la derecha "Comprobante de venta", consecutivo y fecha;
  - datos del cliente: nombre, documento y dirección (copia guardada en la venta, P-35);
  - tabla de ítems: descripción, cantidad con unidad, valor unitario y total; los seriales debajo de cada equipo con su garantía;
  - subtotal, descuento y total en la moneda de la venta y en las monedas adicionales elegidas, con la tasa de referencia y su fecha (RF-130, P-34);
  - pie de la configuración: pago de contado, garantía de 3 meses y "Documento no válido como factura" (RF-131);
  - si la venta está anulada, el PDF lo indica con una marca "ANULADA".
- [ ] **Descargar** (RF-133): con sesión, desde la API.
- [ ] **Enlace para compartir** (RF-134, P-33):
  - enlace público con un token aleatorio, sin sesión, que vence según P-33;
  - la respuesta trae el enlace, el texto del mensaje y el enlace `wa.me` con el número del cliente, para que el computador abra WhatsApp. En el celular, el frontend descarga el PDF y usa el compartir nativo.
- [ ] Mensaje propuesto: "Hola {cliente}, te compartimos el comprobante de venta {V-0001} de ITALARM: {enlace}".

### T6. Clientes e historial — `feat:`
- [ ] El listado y el detalle de clientes dejan de mostrar 0 movimientos: cuentan las ventas activas y la fecha de la última (RF-76, P-36).
- [ ] Historial del cliente (RF-77): consecutivo, fecha, descripción (resumen de productos) y valor de sus ventas, más los totales. Las instalaciones se agregan en la Fase 4.
- [ ] El historial del serial (RF-24) muestra a qué cliente se vendió, en qué venta y hasta cuándo tiene garantía.

### T7. Contrato, documentación y verificación — `docs:`
- [ ] Actualizar `contrato/openapi.json` y `docs/guia-frontend.md` (flujo de venta, vista previa, PDF y WhatsApp).
- [ ] CHANGELOG.md, CLAUDE.md (módulo `ventas`, decisiones) y README.
- [ ] Verificación completa en verde y prueba de ITALARM en Swagger (sección 7).

## 4. Endpoints

Todos bajo `/api/v1`, con `Authorization: Bearer` salvo el enlace público del comprobante. La creación acepta la cabecera `Idempotency-Key`.

### Ventas
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| POST | `/ventas/vista-previa` | Precios sugeridos, disponibilidad, costos con tasas de hoy y de la última compra, resumen con utilidad. No guarda. | `TASA_NO_DISPONIBLE`, `VALIDACION` |
| POST | `/ventas` | Registrar y descontar del inventario. | `STOCK_INSUFICIENTE`, `SERIAL_NO_DISPONIBLE`, `SERIALES_NO_COINCIDEN`, `CANTIDAD_INVALIDA`, `PRODUCTO_INACTIVO`, `VENTA_PRODUCTO_REPETIDO`, `DESCUENTO_INVALIDO`, `CLIENTE_NO_EXISTE`, `TASA_NO_DISPONIBLE` |
| GET | `/ventas?clienteId=&productoId=&desde=&hasta=&incluirAnuladas=` | Listado paginado con los totales del período. | — |
| GET | `/ventas/{id}` | Detalle con líneas, seriales, garantía y utilidad. | `RECURSO_NO_ENCONTRADO` |
| PUT | `/ventas/{id}` | Editar observaciones y monedas adicionales del comprobante (con `version`). | `MODIFICADO_POR_OTRO_USUARIO` |
| POST | `/ventas/{id}/anular` | `{ motivo }`. Devuelve material y seriales. | `VENTA_YA_ANULADA` |
| GET | `/ventas/{id}/comprobante` | Descargar el PDF. | — |
| POST | `/ventas/{id}/enlace` | Crea el enlace público del PDF; responde enlace, vencimiento, mensaje y enlace `wa.me`. | — |

### Comprobante público
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/comprobantes/{token}` | PDF sin sesión mientras el enlace esté vigente; si venció, 404 con un mensaje claro. |

### Clientes
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/clientes/{id}/historial` | Ventas del cliente con consecutivo, fecha, descripción y valor (instalaciones desde la Fase 4). |

## 5. Migraciones Flyway

| Script | Contenido |
|---|---|
| `V9__ventas.sql` | <ul><li>Secuencia `seq_venta`.</li><li>`venta`: consecutivo único, fecha, cliente y copia de sus datos para el comprobante, moneda, TRM y tasa del bolívar con sus fechas, subtotal, descuento (tipo y valor), total, total en USD, costo, utilidad, observaciones, monedas adicionales del comprobante, cotización de origen (vacía hasta la Fase 5), estado (`ACTIVA`/`ANULADA`), motivo, usuario y fecha de anulación, `version`.</li><li>`linea_venta`: producto, descripción, cantidad, precio unitario, precio sugerido, subtotal, costo unitario en USD al momento de la salida.</li><li>`enlace_comprobante`: SHA-256 del token, tipo y documento, vencimiento (preparado para instalaciones y cotizaciones).</li><li>Amplía los `CHECK` de kárdex y de historial de seriales con `VENTA` y `ANULACION_VENTA`.</li><li>`CHECK` de total y descuento no negativos y descuento ≤ subtotal; llaves foráneas a cliente y producto con índices.</li></ul> |

## 6. Pruebas

**Casos de aceptación de la fase (BP-26)**, primero en rojo y después en verde (AG-04):

| Caso | Prueba | Qué verifica |
|---|---|---|
| CP-08 | `cp08_ventaConCosto1750_conservaSuUtilidadAunqueElCostoSuba` | La utilidad sigue calculada con US$ 17,50 después de una compra a US$ 25 |
| CP-14 | `cp14_serialUsadoEnOtraSalida_noEstaDisponible` | Un serial vendido o dado de baja no se puede vender (en la Fase 4 se repite con una instalación) |
| CP-18 | `cp18_anularVentaDeDosCamarasConSerial_devuelveStockYSeriales` | Las 2 unidades vuelven al stock y sus seriales a `EN_BODEGA` |
| CP-25 | `cp25_ventaDel15DeOctubre_garantiaHasta15DeEnero` | Vencimiento de la garantía del serial |
| CP-27 | `cp27_ventaDe100ConDescuentoDe7_total93YUtilidadSobre93` | Total US$ 93; utilidad sobre US$ 93 |
| CP-12 | `cp12_sinTasaDelBolivarHoy_usaLaUltimaConAviso` | Se repite con una venta en VES |
| CP-09 | `cp09_costoConTasaDeHoyYDeLaUltimaCompra` | \$81.900 a la tasa de hoy y \$78.000 a la de la compra (se muestra en la vista previa de la venta; en la Fase 5 se repite en cotizaciones) |

**Pruebas adicionales que pide la fase:**
- **Venta sin stock rechazada:** `STOCK_INSUFICIENTE` y nada se guarda.
- **Concurrencia:** dos ventas simultáneas de la última unidad; una se guarda y la otra responde `STOCK_INSUFICIENTE`; el stock queda en 0.
- **Idempotencia:** la misma `Idempotency-Key` dos veces crea una sola venta.
- **Stock igual a la suma del kárdex** con ventas y anulaciones.
- **PDF:** se genera, contiene el consecutivo, el cliente, los ítems, los totales y el pie; con la venta anulada aparece la marca; el enlace público funciona sin sesión y deja de funcionar al vencer.
- **Otras:** precio en otra moneda que la venta, precio cambiado a mano por debajo del costo (aviso), descuento en porcentaje y en valor, descuento mayor que el subtotal rechazado, historial del cliente y del serial.

**Unitarias del dominio:** precio sugerido, totales y utilidad, descuento, garantía y anulación.

Cobertura mínima de 80 % en dominio y aplicación; ninguna prueba se desactiva (AG-08).

## 7. Definición de terminado (12.1)

- [ ] Verificación completa en verde (en local y en la CI de GitHub).
- [ ] CP-08, CP-14, CP-18, CP-25 y CP-27 automatizados y pasando, más la venta sin stock rechazada y la prueba de concurrencia.
- [ ] Migración V9 aplicada sin errores sobre la base de la Fase 2.
- [ ] Contrato y guía del frontend actualizados; CHANGELOG.md actualizado.
- [ ] Lista de verificación para ITALARM en Swagger:
  1. Hacer la vista previa de una venta a un instalador y a un cliente final y ver el precio, el costo con las dos tasas y la utilidad.
  2. Registrar una venta en COP con descuento y una cámara con serial; ver el stock, el kárdex y la garantía del serial.
  3. Intentar vender más de lo que hay y un serial ya vendido.
  4. Descargar el PDF y abrir el enlace público desde otro navegador sin sesión.
  5. Anular la venta y ver que el material y el serial vuelven a bodega.
  6. Revisar el historial del cliente.

## 8. Riesgos y dependencias

- **Preguntas P-27 a P-36:** definen columnas de la migración V9 (por ejemplo, la copia de los datos del cliente y el vencimiento del enlace). Como una migración aplicada no se modifica (AG-10), hay que responderlas antes de implementar.
- **Datos de la empresa para el PDF** (sección 15, punto 6): logo, NIT, ciudad y teléfono aún no se han cargado en Configuración. El PDF funciona sin ellos, pero conviene cargarlos antes de enviar comprobantes reales.
- **Enlace público:** cualquiera que tenga el enlace ve el comprobante (nombre del cliente, productos y valores). El token es aleatorio de 256 bits, solo se guarda su SHA-256 y el enlace vence (P-33).
