# Plan de la Fase 1 — Catálogo, terceros, tasas y configuración (italarm-api)

> Estado: **terminada (backend)**. Probada y aprobada por ITALARM en su equipo el 01/10/2026, incluida la TRM automática desde datos.gov.co. Pendiente el despliegue al ambiente de pruebas (P-04) y las pantallas en italarm-web.
> Base: `docs/requerimientos.md`, secciones 3.3, 3.5, 3.6, 3.10, 3.17, 5, 9, 10 y 12.3, más las decisiones de `docs/preguntas.md`.
> Alcance: solo el backend (**italarm-api**). Las pantallas de esta fase se planean después en italarm-web, a partir del contrato OpenAPI que deja esta fase.

## 1. Objetivo y entregable

Registrar la información maestra (productos, categorías, unidades, clientes y proveedores) y tener funcionando las tasas de cambio.

Entregable (12.3): el catálogo, los clientes y los proveedores se pueden cargar por la API, y las tasas del día (TRM automática y tasa del bolívar manual) se consultan con su fecha, fuente, hora y usuario.

## 2. Qué queda fuera de esta fase

- Stock, costo, seriales, compras y kárdex: Fase 2. En esta fase el producto nace con stock 0 y sin costo (RF-16), y esos campos son de solo lectura.
- Historial de compras del proveedor (RF-38) y de movimientos del cliente (RF-76, RF-77): en esta fase responden con cero o vacío; se llenan desde las fases 2 a 4.
- Búsqueda de productos por serial (RF-51): Fase 2.
- Alertas por Sentry (9.5): en esta fase la falla de la TRM queda en el log con nivel ERROR y en el estado de las tasas; Sentry llega con el monitoreo de la Fase 6.

## 3. Tareas

Orden según AG-03: migración → dominio con pruebas → casos de uso → endpoints → OpenAPI. Una rama por bloque y commits con Conventional Commits.

### T1. Comunes de la API — `feat:`
- [x] `Pagina<T>`: formato estable de los listados paginados, `{ contenido, pagina, tamano, totalElementos, totalPaginas }`, con los parámetros `page`, `size` y `sort` (RT-04). Tamaño máximo por página: 100.
- [x] Control de versión en las ediciones: todo `PUT` de un dato maestro recibe su `version`. Si otro usuario lo modificó antes, responde 409 `MODIFICADO_POR_OTRO_USUARIO` (BP-12; ya existe el manejo del error).
- [x] Traducción de violaciones de restricciones de la base de datos (únicos, llaves foráneas) a códigos de negocio, como segunda línea de defensa (BP-09).

### T2. Almacenamiento de archivos (módulo `documentos`) — `feat:`
- [x] Interfaz `AlmacenamientoArchivos` (BP-14) con cuatro operaciones: guardar, obtener un enlace firmado de corta duración (15 minutos), eliminar y verificar si existe.
- [x] Implementación **S3** con el AWS SDK v2. Es compatible con Cloudflare R2, AWS S3, DigitalOcean Spaces y MinIO. Se configura con `ITALARM_S3_ENDPOINT`, `ITALARM_S3_REGION`, `ITALARM_S3_BUCKET`, `ITALARM_S3_ACCESS_KEY` e `ITALARM_S3_SECRET_KEY`. El bucket es privado (9.5).
- [x] Implementación **en disco** para el perfil `local`, para que puedas probar sin Docker ni cuenta S3. Guarda los archivos en una carpeta configurable y los sirve por `GET /api/v1/archivos/{clave}` con un enlace firmado (HMAC con vencimiento), igual que S3.
- [x] Validación de archivos subidos (BP-20):
  - el tipo se verifica por el contenido real del archivo, no por la extensión;
  - imágenes JPEG, PNG o WebP;
  - tamaño máximo según P-16.
- [x] Claves de archivo generadas por el sistema (`productos/{id}/foto-{uuid}.jpg`), nunca con el nombre que envía el usuario.

### T3. Catálogo: categorías, unidades de medida y productos — `feat:`
- [x] **Categorías** (RF-15):
  - crear, editar, listar y eliminar (solo si no tienen productos; si no, `CATEGORIA_CON_PRODUCTOS`);
  - nombre único sin distinguir mayúsculas;
  - la migración carga las 8 iniciales: Cámaras, Grabadores (DVR/NVR), Discos duros, Cable, Balunes, Fuentes de poder, Conectores, y Cajas y accesorios.
- [x] **Unidades de medida** (RF-148):
  - crear, editar, listar y eliminar (solo si no están en uso; si no, `UNIDAD_EN_USO`);
  - cada unidad tiene nombre, abreviatura e indicador de si admite cantidades con decimales (según P-09);
  - la migración carga Unidad (`und`), Metro (`m`) y Par (`par`).
- [x] **Productos** (3.3, RF-14 a RF-18):
  - Campos: código, nombre, marca, modelo, categoría, unidad, controla serial, precio instalador, precio cliente final, moneda del precio (USD por defecto), stock mínimo, descripción, foto y activo.
  - Campos de solo lectura: stock (0) y costo actual en USD (vacío hasta la primera compra, Fase 2).
  - Código único sin distinguir mayúsculas. Se guarda en mayúsculas y sin espacios a los lados.
  - Precios mayores o iguales a 0; stock mínimo mayor o igual a 0, en la unidad del producto.
  - Desactivar y activar. Un producto inactivo no aparece para vender, pero conserva su historial (RF-14).
  - Eliminar solo si no tiene movimientos (`PRODUCTO_CON_MOVIMIENTOS`). En esta fase no existen movimientos; desde la Fase 2 lo impiden también las llaves foráneas de la base de datos.
  - Cambiar "controla serial" o la unidad: ver P-17.
  - Foto: subir, reemplazar y quitar (T2). La respuesta trae `fotoUrl` firmada.
  - Listado paginado con filtros por categoría y por activo/inactivo, y búsqueda por nombre, código o marca (RF-51, sin serial).
  - Sin límite de cantidad de productos (RF-17).

### T4. Terceros: clientes y proveedores — `feat:`
- [x] **Clientes** (3.10, RF-75 a RF-78):
  - Campos: tipo (Instalador o Cliente final), nombre o razón social, tipo y número de documento (CC o NIT, opcional), teléfono/WhatsApp (obligatorio, formato según P-10), correo, dirección y ciudad.
  - Crear, editar y consultar.
  - La respuesta indica qué precio se le aplica (`precioAplicado: INSTALADOR | CLIENTE_FINAL`, RF-75/RF-78).
  - Listado paginado con filtro Todos / Instaladores / Clientes finales y búsqueda por nombre, documento, teléfono o ciudad (RF-76). Incluye `cantidadMovimientos` y `fechaUltimoMovimiento`, que por ahora quedan en 0 y vacío.
  - Eliminar o desactivar: según P-11. Documento duplicado: según P-12.
- [x] **Proveedores** (3.6, RF-37):
  - Campos: nombre o razón social, NIT, teléfono, correo, ciudad y moneda habitual (obligatoria).
  - Crear, editar, consultar y listar paginado con búsqueda.

### T5. Tasas de cambio (módulo `tasas`) — `feat:` (dominio con TDD, AG-04)
- [x] **Dominio puro** (BP-05):
  - `ParMoneda` (USD/COP, USD/VES).
  - `TasaCambio`: fecha de negocio, valor con 6 decimales, fuente (Superfinanciera o manual), usuario y hora.
  - `ConfirmacionTasa`: doble digitación y valor mayor que 0 (RF-35a).
  - `VariacionTasa`: porcentaje frente a la tasa anterior y si supera el límite configurado (RF-35b/c).
  - `TasaVigente`: la de hoy, o la última disponible marcada como desactualizada, con su aviso (RF-33).
- [x] **Vista previa del registro manual** (RF-35b): recibe el par y el valor. Devuelve la tasa anterior, la nueva, el porcentaje de variación y si supera el límite, sin guardar nada.
- [x] **Registro manual de la tasa del bolívar** (RF-29, RF-35, RN-07):
  - Es la tasa de hoy (fecha de Bogotá) y se registra una sola vez por día.
  - Si ya existe, se usa la corrección (`TASA_YA_REGISTRADA`).
  - Si las dos digitaciones no coinciden: `TASA_NO_CONFIRMADA` (código ya definido en RT-05).
  - Si la variación supera el límite y no llega `aceptarVariacion: true`: `TASA_VARIACION_NO_ACEPTADA`.
- [x] **Registro manual de la TRM** (RF-33): con las mismas validaciones. Solo se permite si hoy no hay TRM automática (`TRM_AUTOMATICA_DISPONIBLE`).
- [x] **Corrección** (RF-36): nuevo valor con doble digitación y alerta de variación. Guarda el valor anterior, el nuevo, el usuario, la fecha y un motivo opcional. Las transacciones ya guardadas no cambian (RN-04). Qué tasas se pueden corregir: según P-14.
- [x] **Tarea diaria de la TRM** (RF-28, BP-15):
  - Horario: según P-18. Se programa con `@Scheduled` en zona `America/Bogota`.
  - Reintentos si falla (según P-18). En cada falla deja un ERROR en el log y el estado queda visible en las tasas vigentes (`trmAutomaticaFallo: true`).
  - Idempotente: si la TRM del día ya está guardada, no hace nada. Además, la base de datos tiene una restricción única por par y fecha.
  - También corre al arrancar la API si falta la TRM de hoy. Así, si el hosting apaga la API de noche, la tasa se recupera al despertar.
  - Registra cada ejecución (inicio, fin, resultado, intentos, detalle).
  - Si ya hay una TRM manual para ese día, la oficial la reemplaza (P-13). El cambio queda como una corrección automática: valor manual → valor oficial, sin usuario y con el motivo "Reemplazada por la TRM oficial". Los documentos ya guardados conservan su tasa (RN-04).
- [x] **Fuente de la TRM detrás de una interfaz** `FuenteTrm` (BP-14):
  - Implementación: datos abiertos de la Superintendencia Financiera en datos.gov.co, conjunto `32sa-8pi3`, campos `valor`, `vigenciadesde` y `vigenciahasta`. Toma la TRM cuya vigencia cubre la fecha pedida, lo que resuelve fines de semana y festivos.
  - Desde el entorno del agente `datos.gov.co` está bloqueado, así que **la fuente se verifica en tu equipo** durante la demostración. Las pruebas usan un servidor HTTP simulado.
- [x] **Consultas**:
  - Tasas vigentes, para el menú y la barra superior (RF-30, RF-33): valor, fecha, fuente, hora, usuario, si es de hoy y el aviso para Inicio (RF-07).
  - Historial por par y rango de fechas, paginado, con sus correcciones (RF-34).
- [x] **Recordatorio de la tasa del bolívar sin registrar** (9.2): el aviso sale de las tasas vigentes (`esDeHoy: false`). No hay notificaciones push ni correos (no están en el alcance).

### T6. Configuración — `feat:`
- [x] Consultar y editar (RF-145 a RF-147):
  - Datos de la empresa: nombre, lema, NIT, ciudad, teléfono y correo.
  - Valores por defecto:
    - validez de cotización (8, 15 o 30 días);
    - garantía de mano de obra y de equipos (1 a 3 meses, RF-123);
    - condiciones de garantía;
    - pie de los PDF;
    - límite de variación de tasas (mayor que 0 y hasta 100 %).
- [x] Logo: subir, reemplazar y quitar (T2). La respuesta trae `logoUrl` firmada.
- [x] No hace falta una migración nueva: la tabla ya existe desde `V2`.

### T7. Gestión de usuarios (RF-148, RU-04) — `feat:` *(solo si ITALARM lo aprueba en P-15)*
- [x] Listar, crear (nombre, correo y contraseña inicial que cumpla la política), desactivar y activar usuarios.
- [x] Restablecer la contraseña de otro usuario.
- [x] Un usuario no puede desactivarse a sí mismo. Al desactivar un usuario se cierran todas sus sesiones.

### T8. Contrato para el frontend — `docs:`
- [x] Guardar el contrato en `contrato/openapi.json`, versionado en el repositorio. La CI falla si el contrato no corresponde al código.
- [x] `docs/guia-frontend.md`:
  - flujo del token;
  - formato de errores y lista de códigos;
  - dinero como texto con su moneda;
  - paginación;
  - control de versión;
  - subida de archivos y enlaces firmados;
  - tasas vigentes y avisos.
- [x] CHANGELOG.md, CLAUDE.md (módulos nuevos y decisiones) y README (variables nuevas).

## 4. Endpoints

Todos bajo `/api/v1` y con `Authorization: Bearer`. Los errores responden en Problem Details con `codigo`. Los listados son paginados (`page`, `size`, `sort`).

### Categorías y unidades
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| GET | `/categorias` | Todas, con `cantidadProductos` (sin paginar: son pocas). | — |
| POST | `/categorias` | Crear. | `CATEGORIA_DUPLICADA` |
| PUT | `/categorias/{id}` | Editar (con `version`). | `CATEGORIA_DUPLICADA` |
| DELETE | `/categorias/{id}` | Eliminar. | `CATEGORIA_CON_PRODUCTOS` |
| GET | `/unidades-medida` | Todas. | — |
| POST | `/unidades-medida` | Crear. | `UNIDAD_DUPLICADA` |
| PUT | `/unidades-medida/{id}` | Editar. | `UNIDAD_DUPLICADA` |
| DELETE | `/unidades-medida/{id}` | Eliminar. | `UNIDAD_EN_USO` |

### Productos
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| GET | `/productos?categoriaId=&activo=&buscar=` | Listado paginado. | — |
| GET | `/productos/{id}` | Detalle. | `RECURSO_NO_ENCONTRADO` |
| POST | `/productos` | Crear (stock 0, sin costo). | `PRODUCTO_CODIGO_DUPLICADO`, `CATEGORIA_NO_EXISTE`, `UNIDAD_NO_EXISTE` |
| PUT | `/productos/{id}` | Editar (con `version`). | los anteriores y `PRODUCTO_CAMBIO_NO_PERMITIDO` (P-17) |
| POST | `/productos/{id}/desactivar` | Desactivar. | — |
| POST | `/productos/{id}/activar` | Activar. | — |
| DELETE | `/productos/{id}` | Eliminar si no tiene movimientos. | `PRODUCTO_CON_MOVIMIENTOS` |
| PUT | `/productos/{id}/foto` | Subir o reemplazar la foto (`multipart/form-data`). | `ARCHIVO_TIPO_NO_PERMITIDO`, `ARCHIVO_DEMASIADO_GRANDE` |
| DELETE | `/productos/{id}/foto` | Quitar la foto. | — |

### Clientes y proveedores
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| GET | `/clientes?tipo=&buscar=` | Listado paginado. | — |
| GET | `/clientes/{id}` | Detalle. | `RECURSO_NO_ENCONTRADO` |
| POST | `/clientes` | Crear. | `CLIENTE_DOCUMENTO_DUPLICADO` (P-12), `TELEFONO_INVALIDO` (P-10) |
| PUT | `/clientes/{id}` | Editar (con `version`). | los anteriores |
| GET | `/proveedores?buscar=` | Listado paginado. | — |
| GET | `/proveedores/{id}` | Detalle. | `RECURSO_NO_ENCONTRADO` |
| POST | `/proveedores` | Crear. | — |
| PUT | `/proveedores/{id}` | Editar (con `version`). | — |

### Tasas
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| GET | `/tasas/vigentes` | TRM y tasa del bolívar vigentes, con `esDeHoy`, fuente, hora, usuario, `trmAutomaticaFallo` y avisos. | — |
| GET | `/tasas?par=&desde=&hasta=` | Historial paginado (RF-34). | — |
| GET | `/tasas/{id}` | Detalle con sus correcciones. | `RECURSO_NO_ENCONTRADO` |
| POST | `/tasas/vista-previa` | `{ par, valor }` → anterior, nueva, variación y si supera el límite. No guarda. | `VALIDACION` |
| POST | `/tasas/ves` | Registro manual del día: `{ valor, confirmacion, aceptarVariacion }` (RT-03). | `TASA_NO_CONFIRMADA`, `TASA_VARIACION_NO_ACEPTADA`, `TASA_YA_REGISTRADA` |
| POST | `/tasas/trm` | TRM manual si falló la automática. Mismo cuerpo. | los anteriores y `TRM_AUTOMATICA_DISPONIBLE` |
| POST | `/tasas/{id}/corregir` | `{ valor, confirmacion, aceptarVariacion, motivo }`. | `TASA_NO_CONFIRMADA`, `TASA_VARIACION_NO_ACEPTADA`, `TASA_NO_CORREGIBLE` (P-14) |

### Configuración, archivos y usuarios
| Método | Ruta | Descripción | Errores de negocio |
|---|---|---|---|
| GET | `/configuracion` | Datos de la empresa y valores por defecto, con `logoUrl`. | — |
| PUT | `/configuracion` | Editar (con `version`). | `VALIDACION` |
| PUT | `/configuracion/logo` | Subir o reemplazar el logo. | `ARCHIVO_TIPO_NO_PERMITIDO`, `ARCHIVO_DEMASIADO_GRANDE` |
| DELETE | `/configuracion/logo` | Quitar el logo. | — |
| GET | `/archivos/{clave}?expira=&firma=` | Solo en el perfil `local`: entrega un archivo con enlace firmado. Es público, porque la firma es el permiso. | `ENLACE_INVALIDO` |
| GET/POST | `/usuarios`, `/usuarios/{id}/desactivar`, `/usuarios/{id}/activar`, `/usuarios/{id}/restablecer-contrasena` | Solo si se aprueba P-15. | `USUARIO_CORREO_DUPLICADO`, `NO_PUEDE_DESACTIVARSE_A_SI_MISMO` |

## 5. Migraciones Flyway

| Script | Contenido |
|---|---|
| `V4__catalogo.sql` | Tablas `categoria`, `unidad_medida` y `producto`. <ul><li>`categoria`: nombre con índice único sobre `lower(nombre)`.</li><li>`unidad_medida`: nombre, abreviatura única y `admite_decimales`.</li><li>`producto`: `codigo` con índice único sobre `upper(codigo)`; nombre, marca, modelo; llaves foráneas a categoría y unidad con índice; `controla_serial`; `precio_instalador` y `precio_cliente_final` `NUMERIC(19,4)` con `CHECK ≥ 0`; `moneda_precio` con `CHECK IN ('USD','COP','VES')`; `stock NUMERIC(14,3)` con `DEFAULT 0` y `CHECK (stock >= 0)` (BP-09); `costo_actual_usd NUMERIC(19,4)` nulo; `stock_minimo NUMERIC(14,3)` con `CHECK ≥ 0`; `descripcion`; `foto_clave`; `activo`.</li><li>Auditoría y `version` en las tres tablas.</li><li>Datos iniciales: las 8 categorías y las 3 unidades.</li></ul> |
| `V5__terceros.sql` | Tablas `cliente` y `proveedor`. <ul><li>`cliente`: `tipo` con `CHECK IN ('INSTALADOR','CLIENTE_FINAL')`, nombre, `tipo_documento` (`CC`/`NIT`), `numero_documento` (único si P-12 lo aprueba), `telefono`, correo, dirección y ciudad.</li><li>`proveedor`: nombre, NIT, teléfono, correo, ciudad y `moneda_habitual` con `CHECK`.</li><li>Auditoría y `version`.</li><li>Índices por tipo y por `lower(nombre)` para la búsqueda (BP-19).</li></ul> |
| `V6__tasas.sql` | Tablas `tasa_cambio`, `correccion_tasa` y `ejecucion_tarea_trm`. <ul><li>`tasa_cambio`: `par`, `fecha DATE`, `valor NUMERIC(19,6)` con `CHECK > 0`, `fuente` (`SUPERFINANCIERA`/`MANUAL`), `registrada_por` (FK a usuario, nulo si es automática), `registrada_en TIMESTAMPTZ`, `version`. Única por `(par, fecha)`, que es la base de la idempotencia (BP-15).</li><li>`correccion_tasa`: tasa, valor anterior, valor nuevo, motivo, usuario y fecha. Solo se insertan filas, nunca se modifican.</li><li>`ejecucion_tarea_trm`: fecha objetivo, inicio, fin, resultado (`EXITO`/`FALLO`/`OMITIDA`), intentos y detalle.</li></ul> |
| `V7__usuarios_gestion.sql` | Solo si se aprueba P-15 y hace falta algún cambio de esquema; hoy la tabla `usuario` ya tiene lo necesario. |

## 6. Pruebas

**Casos de aceptación de la fase (BP-26)**, primero en rojo y después en verde (AG-04):
- `cp10_tasaDelBolivarDigitadaDistintaDosVeces_noSeGuarda`: 400 `TASA_NO_CONFIRMADA` y nada queda guardado.
- `cp11_tasaAnterior50YNueva500_exigeAceptarLaVariacion`: sin aceptar, 422 `TASA_VARIACION_NO_ACEPTADA`; con `aceptarVariacion: true`, se guarda.
- `cp12_sinTasaDelBolivarDeHoy_usaLaUltimaYAvisa`: con el reloj en un día sin tasa, las vigentes devuelven la última registrada, `esDeHoy: false` y el aviso.

**Tarea de la TRM (12.3), con el servicio simulado:**
- Éxito: guarda la TRM de hoy con fuente Superfinanciera.
- Falla: reintenta, registra la ejecución como FALLO, deja un ERROR en el log y marca `trmAutomaticaFallo`.
- Doble ejecución el mismo día: la segunda queda OMITIDA y no duplica.
- Fin de semana: toma la TRM cuya vigencia cubre la fecha.
- Al arrancar la API sin la TRM de hoy: la consulta.
- Si ya hay TRM manual ese día: la oficial la reemplaza y queda registrada la corrección (P-13).

**Unitarias del dominio:** doble confirmación, cálculo de variación (incluye el caso sin tasa anterior), selección de la tasa vigente y normalización del código de producto y del teléfono.

**Integración (Testcontainers):**
- CRUD de categorías, unidades, productos, clientes y proveedores, con sus reglas: duplicados, eliminar con productos, versión desactualizada (409) y búsqueda y paginación.
- Restricciones de la base de datos: stock negativo rechazado y código duplicado con otra combinación de mayúsculas.
- Auditoría: quién creó y quién editó cada registro.

**Archivos:**
- Almacenamiento S3 probado contra MinIO real con Testcontainers.
- Almacenamiento en disco: enlace firmado válido, vencido y adulterado.
- Rechazo por tipo real (un `.exe` renombrado a `.jpg`) y por tamaño.

**API (MockMvc):** validaciones con mensajes en español, formato de errores y `fotoUrl`/`logoUrl` presentes.

**Arquitectura:** las reglas de ArchUnit existentes se aplican a los módulos nuevos (`catalogo`, `terceros`, `tasas`, `documentos`, `configuracion`).

Cobertura mínima: 80 % en dominio y aplicación. Ninguna prueba se desactiva (AG-08).

## 7. Variables de entorno nuevas

| Variable | Uso | En local |
|---|---|---|
| `ITALARM_ALMACENAMIENTO` | `s3` o `disco` | `disco` |
| `ITALARM_ALMACENAMIENTO_CARPETA` | Carpeta de archivos (modo disco) | `./almacenamiento` (ignorada por git) |
| `ITALARM_ENLACES_CLAVE` | Clave para firmar enlaces (modo disco) | se genera al arrancar si falta |
| `ITALARM_S3_ENDPOINT`, `ITALARM_S3_REGION`, `ITALARM_S3_BUCKET`, `ITALARM_S3_ACCESS_KEY`, `ITALARM_S3_SECRET_KEY` | Conexión S3 | no se usan |
| `ITALARM_TRM_URL` | URL de la fuente de la TRM | la oficial por defecto |

## 8. Definición de terminado (12.1)

- [x] Verificación completa en verde en local: compilación, formato, Checkstyle, 290 pruebas, ArchUnit y JaCoCo ≥ 80 %. (La CI de GitHub corre al abrir el Pull Request.)
- [x] CP-10, CP-11 y CP-12 automatizados y pasando, más las pruebas de la tarea de la TRM.
- [x] Migraciones V4 a V6 aplicadas sin errores en local, sobre la base de la Fase 0. En el ambiente de pruebas, cuando exista (P-04).
- [x] `contrato/openapi.json` actualizado y `docs/guia-frontend.md` listo para italarm-web.
- [x] CHANGELOG.md actualizado.
- [x] Lista de verificación para ITALARM en Swagger (probada por ITALARM el 01/10/2026):
  1. Crear una categoría, una unidad y un producto con foto.
  2. Intentar eliminar una categoría con productos.
  3. Crear un cliente instalador y uno final, y un proveedor en COP.
  4. Ver las tasas vigentes. Registrar la tasa del bolívar digitándola distinto dos veces (debe rechazarla).
  5. Registrar una tasa con una variación grande y verificar la alerta.
  6. Corregir una tasa y ver su historial.
  7. Verificar la TRM automática del día contra la publicada por la Superfinanciera.
  8. Editar la configuración y subir el logo.

## 9. Cambios durante la implementación

| Plan | Implementado | Motivo |
|---|---|---|
| Probar el almacenamiento S3 contra MinIO con Testcontainers. | Se prueba el SDK real de AWS contra un servidor S3 simulado dentro de la prueba (incluye la decodificación `aws-chunked`). | MinIO ya no publica imágenes descargables y Docker Hub limita las descargas en el entorno del agente. |
| Código `TASA_NO_CORREGIBLE`. | No existe. | P-14: cualquier tasa se puede corregir. En su lugar se agregó `TASA_SIN_CAMBIO` (el valor nuevo es igual al actual). |
| — | `POST /tasas/trm/consultar`: consulta la TRM oficial en ese momento. | Permite reintentar desde la pantalla cuando falla la automática y verificar la fuente en la demostración. |
| — | `POST /tasas/vista-previa` acepta `tasaId` opcional. | La vista previa de una corrección compara con esa tasa, no con la del día anterior. |
| `V7__usuarios_gestion.sql`, si hacía falta. | No hizo falta. | La tabla `usuario` ya tenía lo necesario. |
| — | Los `BigDecimal` se declaran en el contrato como `string` con formato `decimal`. | La API los envía como texto (RT-06); antes el contrato decía `number` y el frontend habría generado tipos equivocados. |

## 10. Riesgos y dependencias

- **La fuente de la TRM no se puede verificar desde el entorno del agente**, porque `datos.gov.co` está bloqueado. Se implementa contra el formato publicado del conjunto `32sa-8pi3` y se valida en tu equipo. Si la fuente cambió, solo se ajusta la implementación de `FuenteTrm`.
- **Proveedor S3 sin definir (P-04)**: no bloquea la fase, porque en local se usa el almacenamiento en disco.
- **Preguntas P-09 a P-18**: respondidas. Las decisiones que afectan el esquema se aplican en las migraciones V4 a V6.
