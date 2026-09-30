# Guía de la API para el frontend (italarm-web)

Esta guía es para el agente que construye **italarm-web**. Explica lo que el contrato OpenAPI no dice. Los tipos y las llamadas se generan desde el contrato; nunca se escriben a mano (RT-08, BF-04).

- **Contrato:** [`contrato/openapi.json`](../contrato/openapi.json). Está versionado en este repositorio y la CI verifica que corresponda al código.
- **Swagger en local:** http://localhost:8080/swagger-ui.html
- **Base de todas las rutas:** `/api/v1`. En el frontend se define con `VITE_API_URL`, por ejemplo `http://localhost:8080`.
- **Requerimientos y decisiones:** `docs/requerimientos.md` y `docs/preguntas.md`. Las decisiones de `preguntas.md` prevalecen.

## 1. Sesión (P-05)

1. `POST /api/v1/sesion` con `{ "correo", "contrasena" }` responde `{ token, usuario }`. Guarda el `token` en `localStorage`.
2. Envía `Authorization: Bearer <token>` en **todas** las peticiones.
3. Al cargar la app, llama a `GET /api/v1/sesion`:
   - si responde **200**, el usuario está conectado;
   - si responde **401**, borra el token y muestra la pantalla de ingreso.
4. Cualquier **401** en cualquier petición significa lo mismo: sesión cerrada, volver al ingreso.
5. Cerrar sesión: `DELETE /api/v1/sesion` y luego borrar el token.

Más reglas de la sesión:
- El token **no vence** (P-03). Deja de servir al cerrar sesión, al cambiar la contraseña (se cierran las demás sesiones) o cuando se desactiva el usuario.
- No hay cookies ni CSRF. CORS permite el origen de `ITALARM_CORS_ORIGENES` (en local, `http://localhost:5173`).
- Como el token está en `localStorage`, el frontend debe tener una CSP estricta y nunca insertar HTML sin sanear (riesgo aceptado en el plan de la Fase 0).

## 2. Errores

Todos los errores responden con `Content-Type: application/problem+json` (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Datos inválidos",
  "status": 400,
  "detail": "Revisa los datos ingresados.",
  "codigo": "VALIDACION",
  "correlationId": "5b0c…",
  "errores": [{ "campo": "precioInstalador", "mensaje": "El precio no puede ser negativo." }]
}
```

Cómo usar cada campo:
- **`codigo`:** es estable. Decide el comportamiento por él, nunca por el texto.
- **`detail`:** ya viene en español y se puede mostrar tal cual al usuario.
- **`errores`:** solo aparece con `VALIDACION`. Trae un mensaje por campo; muéstralo junto al campo del formulario (BF-05).
- **`correlationId`:** también llega en la cabecera `X-Correlation-Id`. Sirve para buscar el error en los logs; si el frontend la envía, se reutiliza.

| HTTP | Códigos |
|---|---|
| 400 | `VALIDACION`, `CANTIDAD_INVALIDA`, `CATEGORIA_NO_EXISTE`, `UNIDAD_NO_EXISTE`, `TELEFONO_INVALIDO`, `DOCUMENTO_INCOMPLETO`, `CONFIGURACION_INVALIDA`, `ARCHIVO_TIPO_NO_PERMITIDO`, `ARCHIVO_DEMASIADO_GRANDE`, `TASA_INVALIDA`, `TASA_NO_CONFIRMADA`, `TASA_SIN_CAMBIO`, `CONTRASENA_NO_COINCIDE`, `CONTRASENA_DEBIL`, `METODO_NO_PERMITIDO`, `FORMATO_NO_SOPORTADO` |
| 401 | `NO_AUTENTICADO`, `CREDENCIALES_INVALIDAS` |
| 403 | `ACCESO_DENEGADO`, `ENLACE_INVALIDO` |
| 404 | `RECURSO_NO_ENCONTRADO` |
| 409 | `MODIFICADO_POR_OTRO_USUARIO`, `DATOS_EN_CONFLICTO`, `CATEGORIA_DUPLICADA`, `UNIDAD_DUPLICADA`, `PRODUCTO_CODIGO_DUPLICADO`, `CLIENTE_DOCUMENTO_DUPLICADO`, `USUARIO_CORREO_DUPLICADO`, `TASA_YA_REGISTRADA`, `TRM_AUTOMATICA_DISPONIBLE`, `CONTRASENA_ACTUAL_INCORRECTA` |
| 413 | `ARCHIVO_DEMASIADO_GRANDE` (cuando el archivo supera los 6 MB y ni siquiera llega a validarse) |
| 422 | `CATEGORIA_CON_PRODUCTOS`, `UNIDAD_EN_USO`, `PRODUCTO_CON_MOVIMIENTOS`, `PRODUCTO_CAMBIO_NO_PERMITIDO`, `TASA_VARIACION_NO_ACEPTADA`, `NO_PUEDE_DESACTIVARSE_A_SI_MISMO`, `USAR_CAMBIO_DE_CONTRASENA` |
| 500 | `ERROR_INTERNO` (mostrar un mensaje genérico y ofrecer reintentar, BF-09) |

## 3. Dinero, cantidades y tasas (RT-06)

- **Siempre viajan como texto decimal, nunca como número:** `"19.5000"`, `"3912.450000"`, `"12.5"`. En el contrato son `type: string, format: decimal`. No los conviertas a `number` para hacer cuentas; usa una librería decimal si necesitas calcular una vista previa.
- **El dinero va con su moneda:** `{ "monto": "25.5000", "moneda": "USD" }`. Las monedas son `USD`, `COP` y `VES`.
- **Al enviar**, manda los valores como texto: `"precioInstalador": "25.50"`.
- **Formato al mostrar (BF-07)**, centralizado en `lib/` con `Intl.NumberFormat`:
  - `$ 1.250.000` para COP, sin decimales;
  - `US$ 1.939,04` para USD;
  - `Bs 1.234,56` para VES;
  - fechas en `dd/mm/aaaa`.
- **Cantidades:** vienen sin ceros sobrantes (`"0"`, `"12.5"`). La unidad dice si admite decimales (`unidadMedida.admiteDecimales`): Metro admite hasta 2 decimales; Unidad y Par, solo enteros (P-09).
- **Equivalentes en otras monedas (RF-31):** el frontend los puede calcular como vista previa con `GET /api/v1/tasas/vigentes`. Los valores oficiales de los documentos los calcula y guarda el backend desde la Fase 2 (BF-06).

## 4. Listados paginados (RT-04)

Parámetros: `page` (desde 0), `size` (máximo 100, por defecto 20) y `sort=campo,asc|desc`. La respuesta:

```json
{ "contenido": [ … ], "pagina": 0, "tamano": 20, "totalElementos": 57, "totalPaginas": 3 }
```

Filtros de cada listado:

| Listado | Filtros | Orden por defecto |
|---|---|---|
| `GET /productos` | `categoriaId`, `activo` (`true`/`false`/vacío), `buscar` (nombre, código, marca) | nombre |
| `GET /clientes` | `tipo` (`INSTALADOR`/`CLIENTE_FINAL`), `buscar` (nombre, documento, teléfono, ciudad) | nombre |
| `GET /proveedores` | `buscar` (nombre, NIT, ciudad) | nombre |
| `GET /tasas` | `par` (`USD_COP`/`USD_VES`), `desde`, `hasta` (`aaaa-mm-dd`) | fecha, de la más reciente a la más antigua |

`GET /categorias`, `GET /unidades-medida` y `GET /usuarios` devuelven listas sin paginar, porque son pocos registros.

## 5. Edición con control de versión (BP-12)

- Todo dato maestro trae `version`. Al editar (`PUT`), envía la `version` que recibiste.
- Si otro usuario lo modificó antes, la respuesta es **409 `MODIFICADO_POR_OTRO_USUARIO`**. En ese caso, recarga el registro, muestra los datos actuales y deja que el usuario vuelva a guardar.
- Aplica a categorías, unidades, productos, clientes, proveedores y configuración (en configuración la `version` va siempre en el cuerpo).

## 6. Archivos: foto de producto y logo

- **Subir:**
  - `PUT /productos/{id}/foto` o `PUT /configuracion/logo`, con `multipart/form-data` y el campo **`archivo`**;
  - formatos JPEG, PNG o WebP, máximo 5 MB (P-16);
  - comprime la imagen en el navegador antes de subirla (BF-14).
- **Validación del backend:** revisa el contenido real del archivo. Un archivo renombrado a `.jpg` se rechaza con `ARCHIVO_TIPO_NO_PERMITIDO`.
- **Mostrar:** la respuesta trae `fotoUrl` o `logoUrl`, un enlace firmado que **vence en 15 minutos**. Úsalo directamente en `<img src>` y no lo guardes: si vence, vuelve a pedir el producto o la configuración.
- **Quitar:** `DELETE /productos/{id}/foto` o `DELETE /configuracion/logo`.

## 7. Tasas de cambio (sección 3.5)

- **Recuadro de tasas y aviso de Inicio (RF-07, RF-30, RF-33):** `GET /tasas/vigentes`. Consúltalo al entrar y cada pocos minutos. La respuesta trae:
  - `trm` y `bolivar`, cada una con `valor`, `fecha`, `fuente` (`SUPERFINANCIERA`/`MANUAL`), `registradaEn` y `registradaPor`;
  - `esDeHoy` y `aviso` en cada una: si `aviso` no es `null`, muéstralo destacado en Inicio con el botón **Registrar tasa**;
  - `trmAutomaticaFallo`: si es `true`, ofrece registrar la TRM a mano o reintentar la consulta.
- **Registrar la tasa del bolívar**, con el diálogo de doble digitación (RF-35):
  1. El usuario digita el valor dos veces. Si no coinciden, no envíes nada (el backend igual responde `TASA_NO_CONFIRMADA`).
  2. `POST /tasas/vista-previa` con `{ par: "USD_VES", valor }` devuelve `anterior`, `nueva`, `porcentaje`, `limite` y `superaLimite`. Muéstralos antes de guardar.
  3. Si `superaLimite` es `true`, muestra una alerta destacada que el usuario debe aceptar expresamente.
  4. `POST /tasas/ves` con `{ valor, confirmacion, aceptarVariacion }`. Si la variación supera el límite y no se envía `aceptarVariacion: true`, la respuesta es `TASA_VARIACION_NO_ACEPTADA`.
- **TRM manual (solo si falló la automática):** `POST /tasas/trm`, con el mismo flujo. Si después llega la TRM oficial, la reemplaza (P-13).
- **Reintentar la TRM oficial:** `POST /tasas/trm/consultar`. Responde `EXITO`, `FALLO` u `OMITIDA` (ya estaba guardada).
- **Corregir una tasa (RF-36):** `POST /tasas/{id}/corregir` con `{ valor, confirmacion, aceptarVariacion, motivo }`. La vista previa de una corrección se pide con `tasaId`. El detalle `GET /tasas/{id}` trae el historial de `correcciones`.

## 8. Clientes, productos y usuarios

- **Clientes:**
  - `precioAplicado` y `precioAplicadoDescripcion` dan el texto "Se le aplicará el precio instalador" (RF-75).
  - El teléfono llega con indicativo (`+573001234567`) y sirve directo para `https://wa.me/573001234567`. El usuario lo puede escribir sin indicativo: se asume +57 (P-10).
  - `cantidadMovimientos` y `fechaUltimoMovimiento` quedan en 0 y vacío hasta las fases 3 a 5.
- **Productos:**
  - `stock` y `costoActual` son de solo lectura; en la Fase 1 son `0` y `null`.
  - `bajoMinimo` marca la etiqueta **Bajo** (RF-52).
  - Un producto inactivo no se ofrece para vender.
- **Clientes y proveedores no se eliminan** (P-11): no muestres botón de eliminar.
- **Usuarios:**
  - Cambiar la propia contraseña: `PUT /usuarios/actual/contrasena` (pide la actual).
  - Gestión de usuarios: `GET/POST /usuarios`, `POST /usuarios/{id}/desactivar|activar|restablecer-contrasena`.
  - Política de contraseñas: al menos 8 caracteres, con mayúscula, minúscula, número y signo.

## 9. Cómo mantener el contrato al día

Cuando el backend cambia un endpoint:

1. Se actualiza `contrato/openapi.json` con `./mvnw test -Dtest=ApiComunIntegracionTest -Dcontrato.actualizar=true` y se sube junto con el cambio.
2. En italarm-web se regenera el cliente (orval u openapi-typescript) desde ese archivo; con los dos repositorios en la misma sesión, se lee directamente.
3. TypeScript señala lo que cambió.
