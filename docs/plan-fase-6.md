# Plan de la Fase 6 — Inicio, reportes y Excel, validados en local (italarm-api)

> Estado: **borrador, pendiente de la aprobación de ITALARM**. Antes de implementar hay que responder las preguntas P-57 a P-63 de `docs/preguntas.md`.
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

- [ ] **Saludo** (RF-06): nombre del usuario y fecha de hoy.
- [ ] **Avisos de tasas** (RF-07):
  - falta la tasa del bolívar de hoy;
  - la TRM automática de hoy no llegó.
  - El frontend los muestra antes que cualquier otro bloque, con el botón Registrar tasa.
- [ ] **Cuatro indicadores del mes en curso** (RF-08), en la moneda principal y sus equivalentes (P-57):
  - ventas del mes, con la cantidad;
  - instalaciones del mes, con la cantidad;
  - utilidad del mes, con el porcentaje sobre lo cobrado;
  - inventario valorizado, con la cantidad de productos en bodega.
  - Sin documentos anulados.
- [ ] **Stock bajo** (RF-10): productos activos por debajo del mínimo, con stock, mínimo y nivel (stock ÷ mínimo, para la barra), y el total.
- [ ] **Garantías por vencer** (RF-11): las de los próximos 30 días (P-43), con cliente, consecutivo, vencimiento y días restantes. Reutiliza la vista `garantia`.
- [ ] **Cotizaciones en evaluación** (RF-12, RF-91): con cliente, valor, días para vencer y la marca de "por vencer". Primero las que vencen antes.
- [ ] **Últimos movimientos** (RF-13, P-61): ventas, instalaciones, compras y ajustes, con tipo, consecutivo, cliente o proveedor, usuario, fecha y hora y valor, y el id para abrir su detalle.

Cada bloque trae como máximo 10 filas y el total, para el enlace "Ver todo".

### T2. Reportes por período (módulo `reportes`) — `feat:`

- [ ] **Período** (RF-135, P-60): `SEMANA` (lunes a domingo), `MES` o `ANIO` alrededor de una fecha, o un rango libre `desde`–`hasta`. La respuesta trae el período anterior y el siguiente para las flechas.
- [ ] **Indicadores del período** (RF-136): ventas de material, instalaciones (con cantidad), utilidad total con el margen e inventario valorizado (P-58), en todas las monedas.
- [ ] **Serie para el gráfico de barras** (RF-137): ventas e instalaciones por semana, o por mes si el período es el año.
- [ ] **Productos más vendidos y más usados en instalaciones** (RF-138, P-59), con cantidad y unidad.
- [ ] **Compras por proveedor** (RF-139), con el total en USD de cada uno.
- [ ] **Stock bajo** (RF-140): lo mismo que en Inicio, completo.
- [ ] **Cotizaciones del período** (RF-142): cantidad por estado y porcentaje de aprobadas. Aprobadas = aprobadas + convertidas, sobre las que ya tuvieron respuesta.
- [ ] **Seriales** (RF-141): cuántos hay en bodega, vendidos e instalados, y el listado filtrable por estado y producto.
- [ ] **Utilidad por documento** (RF-143, P-63): listado paginado de ventas e instalaciones del período, con costo, cobrado, utilidad y margen de cada una.
- [ ] Todas las cifras se calculan con los valores en USD guardados en cada documento (RN-04): un reporte no cambia aunque cambien las tasas después.

### T3. Exportación a Excel — `feat:`

- [ ] **Generador común** (RF-144, RNF-06):
  - interfaz `GeneradorExcel` en `documentos.aplicacion`, con implementación en Apache POI (ya incluido por la carga inicial);
  - cada módulo arma un libro con hojas, títulos, columnas y filas tipadas (texto, número, dinero con su moneda, fecha).
  - En el Excel los montos quedan como **números** con formato (COP sin decimales; USD y VES con 2; RNF-04), para que se puedan sumar.
- [ ] **Qué se exporta** (P-62), con los mismos filtros de cada listado, sin paginar:
  - inventario valorizado;
  - kárdex de un producto;
  - compras, ventas, instalaciones y cotizaciones;
  - garantías;
  - seriales;
  - utilidad por documento;
  - el reporte del período, con una hoja por bloque.
- [ ] Encabezado de cada hoja: ITALARM, nombre del reporte, filtros o período, y fecha y hora de generación.
- [ ] Límite de filas por exportación (P-62); si se supera, `EXPORTACION_MUY_GRANDE` con el mensaje de acotar los filtros.

### T4. Revisión de rendimiento — `perf:`

- [ ] **Datos de volumen** para probar en local: un generador (perfil `volumen`, nunca en producción) con un volumen parecido a 3 años de operación:
  - 500 productos y 300 clientes;
  - 3.000 compras, 6.000 ventas y 1.500 instalaciones;
  - 2.000 cotizaciones y 20.000 seriales.
- [ ] Medir con esos datos Inicio, reportes, listados y exportaciones; revisar los planes de consulta (`EXPLAIN ANALYZE`).
- [ ] Agregar los índices que falten en una migración nueva **V12** (solo si la revisión lo pide; las aplicadas no se modifican, AG-10).
- [ ] Meta: Inicio y reportes responden en menos de 500 ms en el servidor, para que la pantalla cargue en menos de 3 segundos con conexión móvil (RNF-08).

### T5. Revisión de seguridad — `fix:` / `docs:`

- [ ] **Endpoints:** todos exigen sesión salvo los públicos acordados (ingreso, Swagger P-06, comprobantes por enlace y archivos firmados), con una prueba que recorra el contrato.
- [ ] **Archivos:** tipos, tamaños y nombres (P-16). **Filtros:** escape de `%` y `_`.
- [ ] **Errores:** Problem Details sin detalles internos.
- [ ] **Cabeceras** de seguridad y CORS limitado al frontend.
- [ ] **Dependencias:** actualizadas a la última versión de corrección, sin vulnerabilidades conocidas.
- [ ] **Secretos:** ninguno en el repositorio (AG-09).
- [ ] Lo que se encuentre se corrige en esta fase. Lo que dependa del hosting queda anotado para la Fase 7.

### T6. Copia y restauración en local — `docs:`

- [ ] Guía `docs/copias-locales.md` para Windows: copia de la base con `pg_dump` y de la carpeta de archivos (fotos, logo y facturas), y cómo restaurarlas.
- [ ] Se prueba la restauración sobre una base vacía. Sirve para no perder las pruebas de ITALARM y como base de las copias automáticas de la Fase 7.

### T7. Validación de punta a punta en local — `docs:`

- [ ] Lista de verificación de todo el sistema, de la Fase 0 a la 6, para que ITALARM la recorra con el frontend en su computador (sección 7).
- [ ] Los casos CP-01 a CP-29 pasan en la verificación automática; la lista indica cómo repetir los principales desde las pantallas.

### T8. Documentación — `docs:`

- [ ] Contrato `contrato/openapi.json`, guía del frontend (Inicio, reportes y Excel), CHANGELOG.md y CLAUDE.md.

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
| `V12__indices_reportes.sql` | Solo si la revisión de rendimiento (T4) lo pide: índices para las consultas de Inicio y reportes. No se agregan tablas. |

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

- [ ] Verificación completa en verde en local y en la CI de GitHub, con CP-01 a CP-29 pasando.
- [ ] Inicio y reportes por debajo de 500 ms con los datos de volumen.
- [ ] Revisión de seguridad hecha y sus hallazgos corregidos o anotados para la Fase 7.
- [ ] Restauración de una copia local probada.
- [ ] Contrato y guía del frontend actualizados; CHANGELOG.md actualizado.
- [ ] **Validación de ITALARM en local, con el frontend.** Con esta validación se pasa a planear la Fase 7. La lista detallada se entrega con la implementación; incluye:
  1. Inicio: aviso de tasa, indicadores, stock bajo, garantías y cotizaciones por vencer, últimos movimientos.
  2. Reportes por semana, mes y año; moverse con las flechas; exportar cada reporte a Excel y abrirlo.
  3. Exportar a Excel inventario, kárdex, compras, ventas, instalaciones, cotizaciones y garantías.
  4. Recorrido completo:
     - carga inicial → compra → venta con PDF y WhatsApp → instalación con fotos;
     - cotización → conversión → anulación → reclamo de garantía.
  5. Hacer una copia local, borrar la base y restaurarla.

## 8. Riesgos y dependencias

- **Preguntas P-57 a P-63:** definen qué muestran los indicadores y los reportes. Conviene responderlas antes de implementar.
- **Frontend:** la validación en local necesita las pantallas de italarm-web. El backend puede terminar antes, y se valida con Swagger mientras tanto.
- **Fase 7:** depende de definir el hosting (P-04). Ninguna tarea de esta fase la bloquea.
