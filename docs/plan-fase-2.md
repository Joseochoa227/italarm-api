# Plan de la Fase 2 — Compras, inventario, costo y carga inicial (italarm-api)

> Estado: **aprobado por ITALARM el 01/10/2026**, implementado; pendiente de la prueba de ITALARM (lista de la sección 7). Preguntas P-19 a P-26 respondidas: de acuerdo con las propuestas (ver `docs/preguntas.md`).
> Base: `docs/requerimientos.md`, secciones 3.4, 3.6 a 3.9, 3.18, 4, 5, 9.4, 10 y 12.4, más las decisiones de `docs/preguntas.md`.
> Alcance: solo el backend (**italarm-api**). Las pantallas se hacen después en italarm-web, a partir del contrato OpenAPI que deja esta fase.

## 1. Objetivo y entregable

El corazón del sistema: que el stock, los seriales y el costo en USD cuadren siempre.

Entregable (12.4): ITALARM carga su inventario real y registra compras, y el costo en USD se calcula correctamente.

## 2. Qué queda fuera de esta fase

- **Ventas e instalaciones** (fases 3 y 4). En esta fase las únicas salidas de inventario son los ajustes de salida. Por eso la prueba de concurrencia de BP-25 ("dos ventas simultáneas de la última unidad") se hace ahora con dos ajustes de salida simultáneos y se repetirá con ventas en la Fase 3.
- **Garantía de los seriales** (RF-23): el vencimiento se calcula al salir en una venta o instalación (fases 3 y 4). En esta fase el serial ya tiene el campo, pero vacío.
- **Costo con la tasa de hoy y la de la última compra** (RF-69): se muestra al cotizar, vender e instalar (fases 3 a 5). En esta fase solo se guarda la tasa de cada compra en el historial de costos, que es el dato que se necesitará.
- **Exportar a Excel** (RF-50, RNF-06): Fase 6. En esta fase Excel solo se usa para la plantilla y la carga inicial.

## 3. Tareas

Orden según AG-03: migración → dominio con pruebas → casos de uso → endpoints → OpenAPI. Las reglas de negocio se escriben primero como prueba del caso CP-xx (AG-04).

### T1. Dominio del inventario (puro, sin Spring, con TDD) — `feat:`
- [x] **`MotorCosto`** (sección 3.8, RF-66, BP-05). Calcula el nuevo costo en USD a partir de: stock actual, costo actual, cantidad comprada y costo unitario en USD.

  | Regla | Cuándo aplica | Resultado |
  |---|---|---|
  | `SUBE` | El costo nuevo es mayor que el actual. | Todo el stock queda al costo nuevo. |
  | `PROMEDIO` | El costo nuevo es menor o igual que el actual. | Promedio ponderado. |
  | `SIN_STOCK` | No hay unidades en bodega. | El costo de la factura. |

  Calcula con 6 decimales y guarda con 4 (BP-06). La regla `SIN_STOCK` se agrega para que el historial (RF-57) diga exactamente qué pasó; el documento solo nombra "sube" y "promedio".
- [x] **`ConversorUsd`**: convierte el costo de la factura a USD. COP ÷ TRM; VES ÷ tasa del bolívar; USD sin cambio.
- [x] **`ReglaStock`**: una salida nunca deja el stock negativo (`STOCK_INSUFICIENTE`, RF-62, RF-65, RN-05). Las cantidades respetan la unidad (P-09, ya existe `ReglaCantidad`).
- [x] **`Serial`**:
  - estados `EN_BODEGA`, `VENDIDO`, `INSTALADO`, `DADO_DE_BAJA` (RF-22) y `ANULADO` (el serial de una compra anulada; ver P-22);
  - transiciones válidas: solo un serial `EN_BODEGA` puede salir;
  - normalización del número según P-22.
- [x] **Validación de seriales de una línea** (RF-20): la cantidad de seriales igual a la cantidad, sin repetidos dentro del documento ni con los que ya existen del producto.
- [x] **Anulabilidad de una compra** según P-23, con el motivo cuando no se puede anular.

### T2. Persistencia del inventario (migración V7) — `feat:`
- [x] Secuencias de consecutivos por tipo de documento (BP-11): `seq_compra` (C-0001), `seq_ajuste` (AJ-001) y `seq_inventario_inicial` (II-001). El formato se aplica al mostrar. Un consecutivo nunca se reutiliza; si una operación falla después de pedir su número, puede quedar un salto (RN-09 exige que sean únicos y no se reutilicen, no que sean continuos).
- [x] **Kárdex** (`movimiento_inventario`), solo de inserción (BP-10):
  - producto, fecha de negocio y fecha y hora de registro;
  - tipo: `COMPRA`, `ANULACION_COMPRA`, `AJUSTE_ENTRADA`, `AJUSTE_SALIDA` o `INVENTARIO_INICIAL`. Las fases siguientes agregan venta, instalación y sus anulaciones;
  - documento (tipo, id y consecutivo), entrada, salida, saldo, costo unitario USD vigente, motivo (en los ajustes) y usuario.
- [x] **Historial de costo** (RF-57, RF-67):
  - producto y compra;
  - fecha;
  - moneda, tasa y costo unitario de la factura;
  - costo en USD;
  - costo anterior, costo nuevo y regla.
- [x] **Seriales** (`serial`):
  - producto, número y estado;
  - documento de entrada (compra, ajuste o inventario inicial) y de salida;
  - fecha de entrada;
  - vencimiento de garantía (vacío por ahora).

  Índice único `(producto, número)` para los seriales no anulados (BP-09).
- [x] **Historial de cada serial** (`movimiento_serial`, solo de inserción): para RF-24.
- [x] **Ajustes** (`ajuste`): consecutivo, fecha, producto, motivo, descripción, cantidad, costo unitario USD y usuario.
- [x] **Inventario inicial** (`inventario_inicial` y sus líneas): consecutivo, fecha, nombre del archivo y usuario.
- [x] **Idempotencia** (`idempotencia`): clave, usuario, operación y documento resultante (RT-07).
- [x] `ProductoInventario`: una entidad del módulo inventario sobre la tabla `producto`, que solo maneja stock, costo, unidad y "controla serial". Así el inventario mueve el stock sin depender del dominio de catálogo. No toca la `version` del producto, para que una compra no le genere conflicto a quien está editando el producto.
- [x] `MovimientosProducto`: la implementa el inventario consultando el kárdex. Con esto se activan P-17 (serial y unidad fijos con movimientos) y RF-14 (no eliminar un producto con movimientos).

### T3. Concurrencia e integridad — `feat:`
- [x] Toda operación que mueve inventario bloquea las filas de los productos y seriales afectados (`SELECT … FOR UPDATE`), siempre en orden de id para evitar bloqueos mutuos (BP-08).
- [x] Segunda línea de defensa: `CHECK (stock >= 0)` ya existe; se agregan el índice único de seriales y llaves foráneas en todas las relaciones (BP-09).
- [x] **Idempotency-Key** en la creación de compras, ajustes y carga inicial (RT-07). Si se repite la misma clave, el mismo usuario recibe el mismo documento en lugar de crear otro.

### T4. Compras (módulo `compras`, migración V8) — `feat:`
- [x] **Registrar una compra** (RF-39, RF-42 a RF-45):
  - Datos: proveedor, número de factura, fecha (P-19), moneda y líneas con producto, cantidad, costo unitario en la moneda de la factura y seriales.
  - Guarda las tasas del día con la compra (RF-32, RN-04): TRM y tasa del bolívar vigentes para esa fecha, aunque la factura sea en USD.
  - Si la moneda es COP o VES y no hay ninguna tasa para convertir: `TASA_NO_DISPONIBLE`. Si la tasa no es la del día, la compra se guarda igual y queda el aviso (RF-33).
  - En una sola transacción: bloquea los productos, aplica el motor de costo línea por línea, actualiza stock y costo, crea los seriales `EN_BODEGA`, y escribe el kárdex y el historial de costo.
  - Total en la moneda de la factura y su equivalente en USD.
- [x] **Vista previa** (RF-41, RF-42): para cada línea, el costo actual en USD, el costo nuevo, la regla y el subtotal en las tres monedas, sin guardar nada. Es el valor oficial que muestra el frontend (BF-06).
- [x] **Factura adjunta** (RF-44): foto (JPEG, PNG, WebP) o **PDF** (se agrega la validación del PDF por su firma `%PDF-`). Máximo 5 MB. Se puede subir, reemplazar y quitar, porque es un dato descriptivo (RF-70).
- [x] **Listado** (RF-46, RF-47):
  - filtros por proveedor, producto, rango de fechas (por defecto, el mes en curso) e incluir o no las anuladas;
  - cada compra muestra proveedor, consecutivo, fecha, factura, usuario, resumen de productos, total y equivalente, tasa guardada y enlace a la factura;
  - **totales del período sin las anuladas** (RF-73): por moneda y su suma en USD.
- [x] **Detalle** (RF-48), que indica además si la compra se puede anular y, si no, por qué.
- [x] **Anular** (RF-71, RF-73):
  - exige un motivo;
  - verifica las condiciones de P-23;
  - descuenta el stock (kárdex `ANULACION_COMPRA`), devuelve el costo al valor anterior según el historial y deja los seriales `ANULADO`;
  - la compra queda visible como anulada, con motivo, usuario y fecha.

  Si no se puede anular: `COMPRA_NO_ANULABLE` (código de RT-05), con el motivo.
- [x] El historial de compras de un proveedor (RF-38) es el mismo listado filtrado por proveedor.

### T5. Ajustes de inventario — `feat:`
- [x] **Registrar un ajuste** (RF-58 a RF-62): un producto por ajuste.
  - Motivo: `PERDIDA`, `DANO`, `CONTEO_FISICO`, `GARANTIA` u `OTRO` (este último con descripción obligatoria).
  - Cantidad: positiva es entrada; negativa, salida.
  - Ajuste de entrada:
    - entra al costo vigente, sin aplicar la regla de costo (RF-61, RN-06);
    - si el producto nunca tuvo costo, se exige el costo unitario en USD (P-25);
    - para productos con serial, se ingresan los seriales nuevos.
  - Ajuste de salida:
    - no puede dejar el stock negativo (`STOCK_INSUFICIENTE`);
    - para productos con serial, se eligen los seriales que se dan de baja (`DADO_DE_BAJA`); deben estar `EN_BODEGA` (`SERIAL_NO_DISPONIBLE`, código de RT-05).
  - Consecutivo AJ-001 y movimiento en el kárdex con su motivo.
- [x] Listado y detalle de ajustes. Anulación de ajustes: según P-24.

### T6. Consultas de inventario — `feat:`
- [x] **Listado de inventario** (RF-49 a RF-52):
  - filtros por categoría y búsqueda por nombre, código, marca **o serial**;
  - por producto: stock con su unidad, etiqueta "Bajo", costo actual en USD y valor en bodega;
  - encabezado: cantidad de productos y valor total del inventario.

  Los valores llegan en USD y convertidos a COP y VES con las tasas vigentes. Si falta una tasa, el equivalente queda vacío con el aviso.
- [x] **Detalle del producto** (RF-53 a RF-55): indicadores en las tres monedas (stock, mínimo, costo, valor en bodega, precios) y resumen de seriales por estado.
- [x] **Kárdex del producto** (RF-56): paginado. Columnas: fecha, movimiento (con motivo en los ajustes), documento, entrada, salida, saldo y usuario.
- [x] **Historial de costo** (RF-57).
- [x] **Seriales** (RF-24, RF-55): listado del producto por estado, búsqueda de un número desde cualquier pantalla y su historial (entrada con proveedor y compra; salidas en las fases siguientes).

### T7. Carga inicial desde Excel (sección 3.18) — `feat:`
- [x] **Plantilla** (RF-149), generada con Apache POI, con encabezados, una fila de ejemplo y una hoja de instrucciones. Los ejemplos quedan en la hoja Instrucciones, para que una fila de ejemplo olvidada en la plantilla no se cargue como dato real. Hojas:
  - `Productos`: código, nombre, marca, modelo, categoría (por nombre), unidad (por abreviatura), controla serial (Sí/No), precio instalador, precio cliente final, moneda del precio, stock mínimo y descripción;
  - `Inventario inicial`: código del producto, cantidad, costo unitario en USD y seriales (formato según P-26);
  - `Clientes`: los campos de la sección 3.10;
  - `Proveedores`: los campos de la sección 3.6.
- [x] **Validar** (RF-150): revisa todo el archivo sin guardar nada y devuelve los errores por hoja y fila, más un resumen de lo que se cargaría. Errores que detecta:
  - códigos repetidos en el archivo o ya existentes;
  - seriales repetidos, faltantes o que no corresponden a la cantidad;
  - cantidades y costos inválidos;
  - categorías y unidades inexistentes;
  - teléfonos o documentos inválidos o duplicados;
  - productos con movimientos (RF-152).
- [x] **Confirmar** (RF-151): vuelve a validar y, si no hay ningún error, guarda todo en una sola transacción:
  - productos, clientes y proveedores;
  - un documento II-001 con un movimiento de entrada por producto en el kárdex;
  - el costo cargado como costo inicial, sin aplicar la regla de costo;
  - los seriales `EN_BODEGA`.

  Si hay un solo error, no se guarda nada. Archivo `.xlsx` de máximo 5 MB.

### T8. Contrato y documentación — `docs:`
- [x] `contrato/openapi.json` actualizado y `docs/guia-frontend.md` con:
  - compra y su vista previa;
  - seriales con escáner;
  - anulación;
  - ajustes;
  - carga inicial;
  - Idempotency-Key.
- [x] CHANGELOG.md, CLAUDE.md (módulos `inventario` y `compras`, decisiones) y README.

## 4. Endpoints

Todos bajo `/api/v1`, con `Authorization: Bearer`. Las creaciones aceptan la cabecera `Idempotency-Key`.

### Compras
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| POST | `/compras/vista-previa` | Costo actual → nuevo y regla por línea; subtotales en las tres monedas. No guarda. | `TASA_NO_DISPONIBLE`, `VALIDACION` |
| POST | `/compras` | Registrar y sumar al inventario. | `SERIALES_NO_COINCIDEN`, `SERIAL_DUPLICADO`, `CANTIDAD_INVALIDA`, `TASA_NO_DISPONIBLE`, `PRODUCTO_INACTIVO`, `COMPRA_PRODUCTO_REPETIDO` (P-20) |
| GET | `/compras?proveedorId=&productoId=&desde=&hasta=&incluirAnuladas=` | Listado paginado con los totales del período. | — |
| GET | `/compras/{id}` | Detalle, con `anulable` y `motivoNoAnulable`. | `RECURSO_NO_ENCONTRADO` |
| POST | `/compras/{id}/anular` | `{ motivo }`. | `COMPRA_NO_ANULABLE`, `COMPRA_YA_ANULADA` |
| PUT | `/compras/{id}/factura` | Subir o reemplazar la factura (`multipart`, imagen o PDF). | `ARCHIVO_TIPO_NO_PERMITIDO`, `ARCHIVO_DEMASIADO_GRANDE` |
| DELETE | `/compras/{id}/factura` | Quitar la factura. | — |

### Inventario, kárdex y seriales
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/inventario?categoriaId=&buscar=` | Listado valorizado con encabezado de totales (busca también por serial). |
| GET | `/inventario/productos/{id}` | Indicadores en las tres monedas y resumen de seriales. |
| GET | `/inventario/productos/{id}/kardex` | Kárdex paginado. |
| GET | `/inventario/productos/{id}/historial-costo` | Cambios de costo. |
| GET | `/inventario/productos/{id}/seriales?estado=` | Seriales del producto. |
| GET | `/seriales?numero=` | Buscar un serial desde cualquier pantalla. |
| GET | `/seriales/{id}` | Historial completo del serial. |

### Ajustes
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| POST | `/ajustes` | `{ productoId, motivo, descripcion, cantidad, costoUnitarioUsd, seriales }`. | `STOCK_INSUFICIENTE`, `SERIAL_NO_DISPONIBLE`, `SERIALES_NO_COINCIDEN`, `SERIAL_DUPLICADO`, `COSTO_REQUERIDO`, `CANTIDAD_INVALIDA` |
| GET | `/ajustes?productoId=&desde=&hasta=` | Listado paginado. | — |
| GET | `/ajustes/{id}` | Detalle. | `RECURSO_NO_ENCONTRADO` |

### Carga inicial
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| GET | `/carga-inicial/plantilla` | Descarga la plantilla `.xlsx`. | — |
| POST | `/carga-inicial/validar` | `multipart`: devuelve errores por hoja y fila y un resumen. No guarda. | `ARCHIVO_TIPO_NO_PERMITIDO` |
| POST | `/carga-inicial` | `multipart`: guarda si no hay errores. Crea el documento II-00N. | `CARGA_INICIAL_CON_ERRORES` (trae la lista de errores) |
| GET | `/carga-inicial` | Cargas realizadas (consecutivo, fecha, usuario y resumen). | — |

## 5. Migraciones Flyway

| Script | Contenido |
|---|---|
| `V7__inventario.sql` | <ul><li>Secuencias `seq_ajuste` y `seq_inventario_inicial`.</li><li>Tablas `movimiento_inventario` (kárdex), `historial_costo`, `serial`, `movimiento_serial`, `ajuste`, `inventario_inicial`, `linea_inventario_inicial` e `idempotencia`.</li><li>Índices por producto, fecha y documento (BP-19).</li><li>`CHECK` de cantidades (entrada y salida mayores o iguales a 0, nunca ambas), de saldo mayor o igual a 0, de estados y de motivos.</li><li>Índice único de seriales no anulados por producto.</li></ul> |
| `V8__compras.sql` | <ul><li>Secuencia `seq_compra`.</li><li>Tablas `compra` y `linea_compra`.</li><li>`compra`: consecutivo único, fecha, proveedor, factura, moneda, TRM, tasa del bolívar y sus fechas, total, total en USD, clave de la factura adjunta, estado (`ACTIVA`/`ANULADA`), motivo, usuario y fecha de anulación.</li><li>`linea_compra`: producto, cantidad, costo unitario en la moneda de la factura, costo unitario en USD y subtotal.</li><li>Llaves foráneas a proveedor y producto (desde aquí ya no se puede eliminar un producto comprado).</li></ul> |

## 6. Pruebas

**Casos de aceptación de la fase (BP-26)**, primero en rojo y después en verde (AG-04):

| Caso | Prueba | Qué verifica |
|---|---|---|
| CP-01 | `cp01_compraConPrecioMayor_subeElCosto` | Stock 20; costo US$ 25 |
| CP-02 | `cp02_compraConPrecioMenor_promedia` | Stock 20; costo US$ 17,50 |
| CP-03 | `cp03_compraConPrecioIgual_aplicaPromedio` | Stock 20; costo US$ 20 |
| CP-04 | `cp04_compraEnCopMasBarata_promediaEnUsd` | Costo US$ 19,50; la compra guarda COP y TRM 4.000 |
| CP-05 | `cp05_compraEnCopMasCara_sube` | Costo US$ 22 |
| CP-06 | `cp06_compraEnVes_convierteConLaTasaDelBolivar` | Factura US$ 19; costo US$ 19,50 |
| CP-07 | `cp07_productoSinStock_tomaElCostoDeLaFactura` | Stock 5; costo US$ 18 |
| CP-13 | `cp13_tresCamarasConDosSeriales_noSeGuarda` | `SERIALES_NO_COINCIDEN` y nada se guarda |
| CP-16 | `cp16_anularUltimaCompraSinSalidas_revierteStockYCosto` | Stock y costo vuelven a los anteriores |
| CP-17 | `cp17_anularCompraConUnaUnidadVendida_noSePermite` | `COMPRA_NO_ANULABLE` con indicación de usar un ajuste. En esta fase la salida se simula con un ajuste de salida. |
| CP-19 | `cp19_conteoFisicoConDosConectoresDeMas_entraAlCostoVigente` | Stock +2 a US$ 0,50; el costo no cambia |
| CP-28 | `cp28_cargaInicialDeDiezCamaras_creaII001` | Stock 10, costo US$ 20, kárdex con II-001 y seriales en bodega |
| CP-29 | `cp29_serialRepetidoEnLaFila8_noGuardaNada` | Error en la hoja de inventario, fila 8 |

**Pruebas adicionales que pide la fase (12.4, BP-10, BP-25):**
- **Concurrencia:** dos ajustes de salida simultáneos de la última unidad. Uno se guarda y el otro responde `STOCK_INSUFICIENTE`; el stock queda en 0, nunca en negativo.
- **Stock igual a la suma del kárdex:** después de una secuencia de compras, ajustes, anulaciones y carga inicial, el stock de cada producto es igual a sus entradas menos sus salidas en el kárdex, y el saldo del último movimiento coincide.
- **Idempotencia:** la misma `Idempotency-Key` dos veces crea una sola compra.
- **Otras:** compras en las tres monedas con sus tasas guardadas, factura en PDF y en imagen, anulación con seriales (quedan `ANULADO` y el número se puede volver a registrar) y búsqueda de un serial con su historial. También que un producto con movimientos ya no se puede eliminar ni cambiar su serial o su unidad (P-17).

**Unitarias del dominio:** motor de costo con los 7 casos, conversión a USD, validación de seriales, stock insuficiente, transiciones de serial y anulabilidad.

Cobertura mínima de 80 % en dominio y aplicación; ninguna prueba se desactiva (AG-08).

## 7. Definición de terminado (12.1)

- [x] Verificación completa en verde en local (384 pruebas); falta confirmarla en la CI de GitHub.
- [x] CP-01 a CP-07, CP-13, CP-16, CP-17, CP-19, CP-28 y CP-29 automatizados y pasando, más la prueba de concurrencia y la de stock igual al kárdex.
- [x] Migraciones V7 y V8 aplicadas sin errores sobre la base de la Fase 1.
- [x] Contrato y guía del frontend actualizados; CHANGELOG.md actualizado.
- [ ] Lista de verificación para ITALARM en Swagger:
  1. Descargar la plantilla, cargar una parte del inventario real con un error a propósito, ver el error por fila, corregirlo y confirmar.
  2. Registrar compras en USD, COP y VES con seriales y verificar el costo resultante contra los ejemplos de la sección 3.8.
  3. Adjuntar la factura en foto y en PDF.
  4. Hacer un ajuste de entrada por conteo físico y uno de salida por daño con serial.
  5. Anular la última compra de un producto y ver que el stock y el costo vuelven atrás; intentar anular una que ya no se puede.
  6. Revisar el kárdex, el historial de costo y buscar un serial.

## 8. Riesgos y dependencias

- **Preguntas P-19 a P-26:** varias definen columnas y reglas de las migraciones V7 y V8. Como una migración aplicada no se modifica (AG-10), hay que responderlas antes de implementar.
- **Orden de las compras y regla de costo:** la regla depende del orden en que se registran las compras. Si se permiten fechas anteriores (P-19), el orden que manda es el de registro, no el de la fecha de la factura.
- **Volumen:** es la fase más grande del proyecto. Se entrega por bloques (dominio → compras → ajustes → consultas → carga inicial), cada uno con sus pruebas en verde y su commit.
