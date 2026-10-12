# Plan de la Fase 6 — Inicio, reportes y Excel, validados en local (italarm-api)

> Estado: **aprobado por ITALARM el 12/10/2026**, implementado; pendiente de la validación de ITALARM en local (lista de la sección 9). Preguntas P-57 a P-63 respondidas: de acuerdo con las propuestas (ver `docs/preguntas.md`).
> Base: `docs/requerimientos.md`, secciones 3.2, 3.16, 5 (RNF-04, RNF-06, RNF-08) y 12.8, más las decisiones de `docs/preguntas.md`.
> Alcance: solo el backend (**italarm-api**). Las pantallas se hacen en italarm-web a partir del contrato OpenAPI que deja esta fase.

## 1. Objetivo y entregable

Completar la visión del negocio (pantalla de Inicio, reportes y exportación a Excel) y dejar el sistema completo **probado en local** con el frontend.

Entregable: ITALARM usa en su computador todas las pantallas del sistema, con Inicio, reportes y Excel, y valida que todo funciona antes de salir a producción.

Por decisión de ITALARM (P-56), la sección 12.8 se divide en dos fases:
- **Fase 6 (este plan):** todo lo que se prueba en local.
- **Fase 7 — Salida a producción:** hosting, dominio, copias de seguridad automáticas, monitoreo, carga de los datos reales y manual de uso. Se planea cuando ITALARM valide la Fase 6 en local y defina el hosting (P-04).

## 2. Qué queda fuera de esta fase

- Todo el despliegue y la operación (Fase 7): ambiente de producción, dominio y HTTPS, base de datos administrada, almacenamiento S3 real, copias de seguridad automáticas diarias (RNF-05), prueba de restauración en el hosting, monitoreo y alertas, carga de los datos reales y manual corto de uso (RNF-10).
- Los gráficos: el backend entrega los datos de cada barra y el frontend los dibuja.

## 3. Tareas

Orden según AG-03: consultas y reglas con pruebas → casos de uso → endpoints → OpenAPI.

### T1. Pantalla de Inicio (nuevo módulo `reportes`) — `feat:`

Un solo endpoint, `GET /inicio`, para que la pantalla cargue con una petición (RNF-08). Trae:

- [x] **Saludo** (RF-06): nombre del usuario y fecha de hoy.
- [x] **Avisos de tasas** (RF-07):
  - falta la tasa del bolívar de hoy;
  - la TRM automática de hoy no llegó.
  - El frontend los muestra antes que cualquier otro bloque, con el botón Registrar tasa.
- [x] **Cuatro indicadores del mes en curso** (RF-08), en la moneda principal y sus equivalentes (P-57):
  - ventas del mes, con la cantidad;
  - instalaciones del mes, con la cantidad;
  - utilidad del mes, con el porcentaje sobre lo cobrado;
  - inventario valorizado, con la cantidad de productos en bodega.
  - Sin documentos anulados.
- [x] **Stock bajo** (RF-10): productos activos por debajo del mínimo, con stock, mínimo y nivel (stock ÷ mínimo, para la barra), y el total.
- [x] **Garantías por vencer** (RF-11): las de los próximos 30 días (P-43), con cliente, consecutivo, vencimiento y días restantes. Reutiliza la vista `garantia`.
- [x] **Cotizaciones en evaluación** (RF-12, RF-91): con cliente, valor, días para vencer y la marca de "por vencer". Primero las que vencen antes.
- [x] **Últimos movimientos** (RF-13, P-61): ventas, instalaciones, compras y ajustes, con tipo, consecutivo, cliente o proveedor, usuario, fecha y hora y valor, y el id para abrir su detalle.

Cada bloque trae como máximo 10 filas y el total, para el enlace "Ver todo".

### T2. Reportes por período (módulo `reportes`) — `feat:`

- [x] **Período** (RF-135, P-60): `SEMANA` (lunes a domingo), `MES` o `ANIO` alrededor de una fecha, o un rango libre `desde`–`hasta`. La respuesta trae el período anterior y el siguiente para las flechas.
- [x] **Indicadores del período** (RF-136): ventas de material, instalaciones (con cantidad), utilidad total con el margen e inventario valorizado (P-58), en todas las monedas.
- [x] **Serie para el gráfico de barras** (RF-137): ventas e instalaciones por semana, o por mes si el período es el año.
- [x] **Productos más vendidos y más usados en instalaciones** (RF-138, P-59), con cantidad y unidad.
- [x] **Compras por proveedor** (RF-139), con el total en USD de cada uno.
- [x] **Stock bajo** (RF-140): lo mismo que en Inicio, completo.
- [x] **Cotizaciones del período** (RF-142): cantidad por estado y porcentaje de aprobadas. Aprobadas = aprobadas + convertidas, sobre las que ya tuvieron respuesta.
- [x] **Seriales** (RF-141): cuántos hay en bodega, vendidos e instalados, y el listado filtrable por estado y producto.
- [x] **Utilidad por documento** (RF-143, P-63): listado paginado de ventas e instalaciones del período, con costo, cobrado, utilidad y margen de cada una.
- [x] Todas las cifras se calculan con los valores en USD guardados en cada documento (RN-04): un reporte no cambia aunque cambien las tasas después.

### T3. Exportación a Excel — `feat:`

- [x] **Generador común** (RF-144, RNF-06):
  - interfaz `GeneradorExcel` en `documentos.aplicacion`, con implementación en Apache POI (ya incluido por la carga inicial);
  - cada módulo arma un libro con hojas, títulos, columnas y filas tipadas (texto, número, dinero con su moneda, fecha).
  - En el Excel los montos quedan como **números** con formato (COP sin decimales; USD y VES con 2; RNF-04), para que se puedan sumar.
- [x] **Qué se exporta** (P-62), con los mismos filtros de cada listado, sin paginar:
  - inventario valorizado;
  - kárdex de un producto;
  - compras, ventas, instalaciones y cotizaciones;
  - garantías;
  - seriales;
  - utilidad por documento;
  - el reporte del período, con una hoja por bloque.
- [x] Encabezado de cada hoja: ITALARM, nombre del reporte, filtros o período, y fecha y hora de generación.
- [x] Límite de filas por exportación (P-62); si se supera, `EXPORTACION_MUY_GRANDE` con el mensaje de acotar los filtros.

### T4. Revisión de rendimiento — `perf:`

- [x] **Datos de volumen** para probar en local: un generador (perfil `volumen`, nunca en producción) con un volumen parecido a 3 años de operación:
  - 500 productos y 300 clientes;
  - 3.000 compras, 6.000 ventas y 1.500 instalaciones;
  - 2.000 cotizaciones y 20.000 seriales.
- [x] Medir con esos datos Inicio, reportes, listados y exportaciones; revisar los planes de consulta (`EXPLAIN ANALYZE`).
- [x] Agregar los índices que falten en una migración nueva **V12** (solo si la revisión lo pide; las aplicadas no se modifican, AG-10).
- [x] Meta: Inicio y reportes responden en menos de 500 ms en el servidor, para que la pantalla cargue en menos de 3 segundos con conexión móvil (RNF-08).

### T5. Revisión de seguridad — `fix:` / `docs:`

- [x] **Endpoints:** todos exigen sesión salvo los públicos acordados (ingreso, Swagger P-06, comprobantes por enlace y archivos firmados), con una prueba que recorra el contrato.
- [x] **Archivos:** tipos, tamaños y nombres (P-16). **Filtros:** escape de `%` y `_`.
- [x] **Errores:** Problem Details sin detalles internos.
- [x] **Cabeceras** de seguridad y CORS limitado al frontend.
- [x] **Dependencias:** actualizadas a la última versión de corrección, sin vulnerabilidades conocidas.
- [x] **Secretos:** ninguno en el repositorio (AG-09).
- [x] Lo que se encuentre se corrige en esta fase. Lo que dependa del hosting queda anotado para la Fase 7.

### T6. Copia y restauración en local — `docs:`

- [x] Guía `docs/copias-locales.md` para Windows: copia de la base con `pg_dump` y de la carpeta de archivos (fotos, logo y facturas), y cómo restaurarlas.
- [x] Se prueba la restauración sobre una base vacía. Sirve para no perder las pruebas de ITALARM y como base de las copias automáticas de la Fase 7.

### T7. Validación de punta a punta en local — `docs:`

- [x] Lista de verificación de todo el sistema, de la Fase 0 a la 6, para que ITALARM la recorra con el frontend en su computador (sección 7).
- [x] Los casos CP-01 a CP-29 pasan en la verificación automática; la lista indica cómo repetir los principales desde las pantallas.

### T8. Documentación — `docs:`

- [x] Contrato `contrato/openapi.json`, guía del frontend (Inicio, reportes y Excel), CHANGELOG.md y CLAUDE.md.

## 4. Endpoints

Todos bajo `/api/v1`, con `Authorization: Bearer`.

### Inicio y reportes

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/inicio` | Saludo, avisos de tasas, indicadores del mes, stock bajo, garantías y cotizaciones por vencer, últimos movimientos. |
| GET | `/reportes/periodo?tipo=SEMANA\|MES\|ANIO&fecha=` o `?desde=&hasta=` | Indicadores, serie del gráfico, más vendidos e instalados, compras por proveedor, stock bajo y cotizaciones por estado. |
| GET | `/reportes/utilidad?desde=&hasta=&tipo=VENTA\|INSTALACION` | Utilidad por documento, paginada. |
| GET | `/reportes/seriales?estado=&productoId=` | Conteo por estado y listado paginado de seriales. |

### Exportaciones a Excel

Todas devuelven un `.xlsx` con los mismos filtros del listado correspondiente. Error: `EXPORTACION_MUY_GRANDE` (422).

| Método | Ruta |
|---|---|
| GET | `/reportes/periodo/excel`, `/reportes/utilidad/excel`, `/reportes/seriales/excel` |
| GET | `/inventario/excel`, `/inventario/productos/{id}/kardex/excel` |
| GET | `/compras/excel`, `/ventas/excel`, `/instalaciones/excel`, `/cotizaciones/excel`, `/garantias/excel` |

## 5. Migraciones Flyway

| Script | Contenido |
|---|---|
| — | No hizo falta la V12: con los datos de volumen, ninguna consulta pasó de 100 ms con los índices existentes (sección 8). |

## 6. Pruebas

**Pruebas de la fase:**
- **Inicio:**
  - indicadores del mes con ventas e instalaciones en varias monedas, sin anuladas, y documentos del mes anterior que no cuentan;
  - avisos de tasa con y sin la tasa del bolívar de hoy;
  - stock bajo, garantías y cotizaciones por vencer en los límites de 30 y 3 días;
  - orden de los últimos movimientos.
- **Períodos:** semana de lunes a domingo, mes y año, rango libre, y el período anterior y siguiente, incluido el cambio de año.
- **Reportes:**
  - serie por semana y por mes;
  - más vendidos e instalados;
  - compras por proveedor;
  - porcentaje de cotizaciones aprobadas;
  - utilidad por documento que cuadra con el detalle de cada venta e instalación;
  - todo sin documentos anulados.
- **Excel:**
  - se abre con Apache POI;
  - trae las hojas, los encabezados y los montos como números;
  - aplica los mismos filtros que el listado;
  - responde `EXPORTACION_MUY_GRANDE` al pasar el límite.
- **Seguridad:** todos los endpoints del contrato responden 401 sin sesión, salvo los públicos.
- **Rendimiento:** medición con los datos de volumen. Se anota en el plan; no corre en la verificación de cada commit.

**Unitarias:** cálculo de períodos y semanas, porcentaje de aprobadas, nivel de stock y formato de las celdas de dinero.

Cobertura mínima de 80 % en dominio y aplicación; ninguna prueba se desactiva (AG-08).

## 7. Definición de terminado

- [x] Verificación completa en verde en local, con CP-01 a CP-29 pasando; falta confirmarla en la CI de GitHub.
- [x] Inicio y reportes por debajo de 500 ms con los datos de volumen (sección 8).
- [x] Revisión de seguridad hecha; lo que depende del hosting queda anotado para la Fase 7 (sección 8).
- [x] Restauración de una copia local probada (`docs/copias-locales.md`).
- [x] Contrato y guía del frontend actualizados; CHANGELOG.md actualizado.
- [ ] Validación de ITALARM en local con el frontend (sección 9). Con ella se pasa a planear la Fase 7.

## 8. Resultados de las revisiones

### Rendimiento (T4)

Medido el 12/10/2026 con el perfil `volumen`:
- **Datos:** 500 productos, 300 clientes, 3.000 compras, 6.000 ventas, 1.500 instalaciones, 2.000 cotizaciones, 20.000 seriales y 21.000 movimientos de kárdex.
- **Ambiente:** PostgreSQL 16 en Docker y la API en la misma máquina.
- **Medición:** tiempo total de la respuesta, mediana de 5 llamadas tras una de calentamiento.

| Endpoint | Tiempo |
|---|---|
| `GET /inicio` | ~70 ms |
| `GET /reportes/periodo` (mes, año) | 40–50 ms |
| `GET /reportes/periodo` (rango de 3 años) | ~75 ms |
| `GET /reportes/utilidad`, `/reportes/seriales` | ~25 ms |
| Listados de ventas, compras e instalaciones (3 años) | 40–50 ms |
| Inventario (con búsqueda por serial), kárdex, garantías, clientes, cotizaciones | 20–35 ms |
| Excel de 6.000 ventas / 7.500 documentos de utilidad / 20.000 seriales | 0,5 s / 0,3 s / 0,4 s |

- Los Excel tardaban 2 a 5 segundos por `autoSizeColumn`. Se cambió por un ancho calculado por caracteres.
- No hizo falta agregar índices (V12).

### Seguridad (T5)

- **Endpoints:** `SeguridadContratoIntegracionTest` recorre todo el contrato. Todos responden 401 sin sesión, salvo los tres públicos: ingreso, archivo con enlace firmado y comprobante por enlace.
- **Ya cumplido:**
  - CORS limitado al frontend;
  - cabeceras CSP, `Referrer-Policy: no-referrer`, `X-Content-Type-Options` y `X-Frame-Options`;
  - Problem Details sin detalles internos (los errores inesperados solo se registran en el log);
  - `%` y `_` escapados en todas las búsquedas;
  - imágenes validadas por contenido;
  - contraseñas con BCrypt y tokens guardados como SHA-256;
  - ningún secreto en el repositorio.
- **Dependencias:**
  - Spring Boot 3.5.16, springdoc 2.8.17, Apache POI 5.5.1 y OpenPDF 3.0.5 son la última versión de su línea;
  - AWS SDK subió a 2.55.14.
- **Para la Fase 7:**
  - HSTS y HTTPS, que los da el hosting;
  - un análisis automático de vulnerabilidades de dependencias en la CI;
  - planear el paso a Spring Boot 4 antes de que termine el soporte de la línea 3.5.

## 9. Lista de verificación para ITALARM en local

Con la API (`.\mvnw.cmd spring-boot:run`) y el frontend (`npm run dev`) corriendo, en http://localhost:5173. Mientras el frontend no tenga una pantalla, se puede hacer el paso en Swagger (http://localhost:8080/swagger-ui.html).

**Antes de empezar:** haz una copia de la base y de la carpeta de archivos (`docs/copias-locales.md`).

1. **Ingreso y configuración:**
   - ingresar con el correo y la contraseña inicial y cambiar la contraseña;
   - revisar los datos de la empresa y el logo;
   - crear un usuario y desactivarlo.
2. **Tasas:**
   - ver la TRM del día;
   - registrar la tasa del bolívar dos veces con valores distintos: no se guarda (CP-10);
   - registrarla bien y ver la alerta de variación.
3. **Catálogo y terceros:** crear una categoría, un producto con serial y uno sin serial con stock mínimo, un cliente instalador, un cliente final y un proveedor.
4. **Carga inicial:** descargar la plantilla, cargar productos y seriales, ver el error de un serial repetido (CP-29) y confirmar.
5. **Compras e inventario:**
   - registrar una compra en COP con seriales y adjuntar la factura;
   - ver el costo, el kárdex y el historial de costo;
   - hacer un ajuste;
   - anular la compra si fue la última.
6. **Venta:**
   - vista previa con el precio según el tipo de cliente y el costo a las dos tasas;
   - guardar con descuento;
   - descargar el PDF y abrir el enlace de WhatsApp;
   - anularla y ver que el material vuelve.
7. **Instalación:**
   - con dos técnicos, cámaras con serial, cable y mano de obra;
   - subir fotos de antes, durante y después;
   - PDF;
   - consultar sus garantías y registrar un reclamo.
8. **Cotización:**
   - crearla, enviarla por WhatsApp (queda En evaluación) y editarla (v2);
   - aprobarla y convertirla en instalación;
   - anular esa instalación: la cotización vuelve a Aprobada;
   - duplicar una cotización rechazada.
9. **Inicio:**
   - aviso de tasa (borra la del bolívar de hoy para verlo);
   - indicadores del mes contra lo registrado;
   - stock bajo con su barra;
   - garantías y cotizaciones por vencer;
   - últimos movimientos que abren su detalle.
10. **Reportes:**
    - por semana, mes y año, moviéndose con las flechas, y un rango libre;
    - el gráfico, los más vendidos, las compras por proveedor y el porcentaje de cotizaciones aprobadas;
    - utilidad por documento y seriales.
11. **Excel:** exportar inventario, kárdex, compras, ventas, instalaciones, cotizaciones, garantías y cada reporte, y abrirlos: los montos se pueden sumar.
12. **Celular:** repetir una venta y una instalación con fotos desde el celular, en la misma red Wi-Fi (Paso 5 de la guía de inicio del frontend).
13. **Copia y restauración:**
    - hacer una copia;
    - registrar una venta de prueba;
    - restaurar la copia;
    - la venta de prueba ya no está y el resto sí.

Cuando todo esté bien, se escribe el plan de la Fase 7 (salida a producción).

## 10. Riesgos y dependencias

- **Preguntas P-57 a P-63:** definen qué muestran los indicadores y los reportes. Conviene responderlas antes de implementar.
- **Frontend:** la validación en local necesita las pantallas de italarm-web. El backend puede terminar antes, y se valida con Swagger mientras tanto.
- **Fase 7:** depende de definir el hosting (P-04). Ninguna tarea de esta fase la bloquea.
