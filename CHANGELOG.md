# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [Sin publicar] — Fase 5: Cotizaciones

### Agregado
- Cotizaciones de venta o de instalación (migración V11), con consecutivo COT-0001:
  - vista previa con precio sugerido según el tipo de cliente, costo con las tasas de hoy y de la última compra (CP-09), cobro con utilidad estimada y vencimiento;
  - registro en Borrador con validez de 8, 15 o 30 días, descuento, mano de obra y descripción (en las de instalación), tasas de hoy e `Idempotency-Key`; no aparta ni descuenta material (CP-26);
  - estados Borrador, En evaluación, Aprobada, Convertida, Rechazada (con motivo y detalle opcionales) y Vencida, con las transiciones en el dominio (`TRANSICION_NO_PERMITIDA`);
  - edición: en Borrador reemplaza; En evaluación guarda una nueva versión (COT-0001 v2) y conserva la anterior;
  - duplicar con los precios cotizados y las tasas de hoy;
  - listado con filtros por estado, cliente, tipo, fechas y `porVencer` (en evaluación con 3 días o menos), con días para vencer;
  - tarea diaria a las 00:05 y al arrancar que vence las cotizaciones en Borrador o En evaluación (CP-21);
  - PDF con "Válida hasta", mano de obra como una línea más y marca de agua "VENCIDA" o "RECHAZADA"; enlace público para WhatsApp (pasa a En evaluación) y mensaje de seguimiento.
- Conversión (RF-93 a RF-96): `GET /cotizaciones/{id}/conversion` devuelve el formulario precargado y los avisos de precio, costo y stock; `POST /ventas` y `POST /instalaciones` aceptan `cotizacionId`, dejan la cotización Convertida y enlazada (CP-22) y no guardan nada si falta stock (CP-23). Al anular el documento generado, la cotización vuelve a Aprobada (CP-24).

### Cambiado
- El detalle de ventas e instalaciones trae `cotizacion` (id y consecutivo) cuando vienen de una cotización.
- `PRODUCTO_INACTIVO` pasa a `shared` y también se aplica al cotizar.

## [Sin publicar] — Fase 4: Instalaciones, fotos y garantías

### Agregado
- Instalaciones (migración V10):
  - vista previa con material (precio, disponibilidad y costos con las tasas de hoy y de la última compra), mano de obra, descuento, cobro con utilidad en USD, COP y VES y vencimientos de garantía;
  - registro con cliente, dirección, fecha (puede ser anterior a hoy), técnicos, descripción, material con seriales, garantía de mano de obra de 1 a 3 meses y condiciones; descuenta el material del inventario (CP-15) e `Idempotency-Key`;
  - listado con filtros por cliente, técnico, fecha y estado de la garantía, y totales del período sin las anuladas;
  - edición de dirección, descripción, técnicos, condiciones y observaciones;
  - anulación que devuelve material y seriales al costo vigente;
  - fotos Antes, Durante y Después (hasta 30 por grupo), agregar después de guardada y quitar.
- Comprobante de instalación en PDF con mano de obra, seriales, vencimientos y condiciones de garantía, y enlace público para WhatsApp.
- Garantías: consulta de la mano de obra de cada instalación y de cada equipo con serial vendido o instalado, con estado Vigente, Por vencer o Vencida (CP-20), filtros por cliente, tipo y serial; reclamos de garantía con solución posterior y marca de fuera de garantía.
- Los técnicos son los usuarios activos (`GET /usuarios/tecnicos`); el historial del cliente suma sus instalaciones y el del serial muestra sus reclamos.

### Cambiado
- El precio sugerido, el descuento y el cálculo de totales y utilidad pasan a `shared`, y la preparación del material y la vista previa al paquete `comercial`, para que ventas, instalaciones y cotizaciones apliquen las mismas reglas. El código de error del precio negativo es ahora `PRECIO_INVALIDO`.
- En el contrato, las vistas comunes cambian de nombre: `ClienteDocumentoVista`, `TasasDocumentoVista`, `ResumenCobroVista` (agrega `material` y `manoDeObra`) y `LineaVistaPrevia`.

## [Sin publicar] — Fase 3: Ventas, comprobantes y anulaciones

### Agregado
- Ventas (migración V9):
  - vista previa con precio sugerido según el tipo de cliente, disponibilidad con aviso de stock insuficiente, costo con las tasas de hoy y de la última compra (CP-09) y resumen con utilidad en USD, COP y VES;
  - registro con descuento en porcentaje o valor, costo vigente y utilidad guardados (CP-08, CP-27), tasas del día (CP-12), seriales vendidos con garantía de 3 meses (CP-25) e `Idempotency-Key`;
  - listado con filtros y totales del período sin las anuladas; detalle; edición de observaciones y monedas del comprobante;
  - anulación que devuelve material y seriales al costo vigente (CP-18);
  - venta sin stock rechazada y dos ventas simultáneas de la última unidad: solo una se guarda.
- Comprobante de venta en PDF (OpenPDF) con datos de la empresa, del cliente, ítems con seriales y garantía, totales en otras monedas y pie; marca "ANULADA" en las ventas anuladas.
- Enlace público del comprobante para WhatsApp: vence a los 30 días y trae el mensaje y el enlace `wa.me` del cliente.
- Clientes con cantidad de movimientos, fecha del último e historial; el historial del serial muestra el cliente y la garantía.

## [Sin publicar] — Fase 2: Compras, inventario, costo y carga inicial

### Agregado
- Inventario (migración V7):
  - kárdex solo de inserción, historial de costo, seriales con su historial y consecutivos por tipo de documento;
  - motor de costo en USD (sube, promedio ponderado o costo de la factura sin stock) con los casos CP-01 a CP-07;
  - bloqueo de productos y seriales en orden de id: dos salidas simultáneas de la última unidad no dejan el stock negativo;
  - `Idempotency-Key` en la creación de compras, ajustes y carga inicial.
- Compras (migración V8):
  - vista previa del cambio de costo por línea con subtotales en USD, COP y VES;
  - registro con las tasas de la fecha de la factura, seriales y avisos de tasa;
  - listado con filtros y totales del período sin las anuladas;
  - detalle con `anulable` y su motivo, y anulación que revierte stock, costo y seriales (CP-16, CP-17);
  - factura adjunta en imagen o PDF.
- Ajustes de entrada y salida con motivo, costo requerido si el producto no tiene costo (P-25) y baja de seriales (CP-19).
- Consultas: inventario valorizado con búsqueda por serial, detalle en tres monedas, kárdex, historial de costo, seriales del producto y búsqueda e historial de un serial.
- Carga inicial desde Excel (Apache POI): plantilla, validación por hoja y fila sin guardar y confirmación en una sola transacción con el documento II-001 (CP-28, CP-29).
- Desde esta fase, un producto con movimientos no se elimina ni cambia su serial o su unidad (P-17).

## [Sin publicar] — Fase 1: Catálogo, terceros, tasas y configuración

### Agregado
- Catálogo (migración V4):
  - categorías y unidades de medida con sus valores iniciales;
  - productos con código único, precios en USD u otra moneda, stock mínimo según la unidad, foto, activar/desactivar y búsqueda paginada.
- Clientes y proveedores (migración V5):
  - teléfono con indicativo internacional;
  - documento único;
  - precio aplicado según el tipo de cliente.
- Tasas de cambio (migración V6):
  - TRM automática diaria desde datos.gov.co, con reintentos e idempotencia;
  - tasa del bolívar manual con doble digitación y alerta de variación;
  - TRM manual si falla la automática;
  - correcciones con historial, tasas vigentes con avisos e historial por fechas;
  - casos CP-10, CP-11 y CP-12.
- Configuración: edición de los datos de la empresa, el logo y los valores por defecto.
- Gestión de usuarios: crear, desactivar/activar y restablecer contraseña.
- Almacenamiento de archivos:
  - S3 (Cloudflare R2, AWS S3, DigitalOcean Spaces) o disco local, con enlaces firmados de 15 minutos;
  - validación de imágenes por contenido.
- Listados paginados, control de versión en las ediciones y traducción de restricciones de la base de datos a códigos de negocio.
- Contrato `contrato/openapi.json` versionado y `docs/guia-frontend.md` para italarm-web.

## [Sin publicar] — Fase 0: Fundaciones

### Agregado
- Proyecto Spring Boot 3.5 / Java 21 con Maven Wrapper, organizado en módulos de negocio (sección 10.1).
- PostgreSQL local con Docker Compose y migraciones Flyway:
  - `V1` usuarios Jose y Victor;
  - `V2` configuración con los valores por defecto;
  - `V3` sesiones.
- Ingreso con correo y contraseña, sesión con token `Bearer` sin vencimiento, cierre de sesión y consulta del usuario actual (`/api/v1/sesion`).
- Cambio de contraseña con doble digitación y política de mayúscula, minúscula, número y signo; cierra las demás sesiones del usuario (`PUT /api/v1/usuarios/actual/contrasena`).
- Asignación de la contraseña inicial desde la variable `ITALARM_CLAVE_INICIAL`.
- Objeto de valor `Dinero`, redondeo centralizado, formato de dinero es-CO y fechas de negocio en hora de Colombia con reloj inyectable.
- Errores en Problem Details (RFC 9457) con código de negocio y mensajes en español, más un identificador de correlación en cada petición y en los logs.
- Auditoría automática (`created_*`, `updated_*`) y control de versión optimista.
- Seguridad:
  - contraseñas con BCrypt;
  - CORS limitado al frontend;
  - cabeceras de seguridad (CSP, X-Frame-Options, Referrer-Policy, nosniff).
- OpenAPI y Swagger UI públicos; salud con Actuator (`/actuator/health`, liveness y readiness).
- Calidad:
  - Spotless (Google Java Format);
  - Checkstyle;
  - ArchUnit;
  - JaCoCo con 80 % mínimo en dominio y aplicación.
- Dockerfile multi-etapa con usuario no root y CI en GitHub Actions (verificación completa, contrato OpenAPI, cobertura e imagen Docker).
