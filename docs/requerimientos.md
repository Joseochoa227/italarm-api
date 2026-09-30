> Copia en Markdown del documento de requerimientos v0.7 de ITALARM, para uso del agente de desarrollo (Claude Code). Guardar como docs/requerimientos.md en cada repositorio. Las capturas del prototipo están en el documento Word y en el archivo Italarm.html.

**# DOCUMENTO DE REQUERIMIENTOS**

Sistema de Inventario, Ventas, Cotizaciones e Instalaciones de Cámaras de Seguridad

Versión 0.7 — Borrador para revisión

30 de septiembre de 2026

*Los puntos resaltados en amarillo están pendientes por definir.*

**
## 1. Introducción

### 1.1 Descripción del negocio

ITALARM se dedica a la instalación de cámaras de seguridad. Además de realizar instalaciones, compra material (cámaras, grabadores, discos duros, cable, conectores, fuentes, entre otros) que puede utilizar en sus propias instalaciones o vender directamente a otros instaladores y a clientes finales.

Actualmente el control de compras, ventas e instalaciones se lleva en Excel y en cuaderno, lo que dificulta saber con exactitud cuánto material hay disponible, cuánto costó y cuánta utilidad deja cada venta o instalación.

### 1.2 Objetivo del sistema

Contar con un sistema sencillo, accesible desde celular y computador, que permita:

- Llevar el inventario de todo el material comprado, por cantidad y por número de serie, con su disponibilidad en tiempo real.

- Registrar las compras a proveedores en dólares (USD), pesos colombianos (COP) o bolívares (VES).

- Elaborar cotizaciones, hacerles seguimiento mientras el cliente las evalúa y, cuando el cliente aprueba, convertirlas en venta o instalación sin volver a digitar.

- Registrar las ventas de material a instaladores y a clientes finales.

- Registrar cada instalación con su cliente, material usado, mano de obra, fotos y garantía.

- Mostrar todos los precios en USD, COP y VES usando las tasas de cambio del día: la TRM se consulta de forma automática y la tasa del bolívar se ingresa manualmente.

- Generar cotizaciones y comprobantes en PDF para enviar por WhatsApp.

- Conocer en todo momento la utilidad de cada venta, de cada instalación y del período.

### 1.3 Alcance

El sistema es una herramienta de control interno. No reemplaza al software contable ni emite facturación electrónica. Está pensado inicialmente para una sola bodega y dos usuarios, pero debe permitir crecer en número de productos, clientes y movimientos sin cambios de fondo.

### 1.4 Enfoque de desarrollo

El sistema se construirá como software a la medida, desarrollado por un programador o empresa de software a partir de este documento. Este documento servirá como base para solicitar cotizaciones, acordar el alcance con el desarrollador y verificar que el sistema entregado cumpla lo pedido. El desarrollo lo realizará un agente de Claude Code supervisado por ITALARM, siguiendo la arquitectura (sección 9), las buenas prácticas (secciones 10 y 11), el plan por fases (sección 12) y la guía de trabajo (sección 13). La sección 14 incluye los casos de prueba de aceptación.

### 1.5 Prototipo de referencia

Acompaña a este documento un prototipo navegable (archivo Italarm.html) que muestra el diseño visual esperado, la organización de las pantallas y el flujo de trabajo. El prototipo usa datos de ejemplo y no guarda información.

En caso de diferencia entre el prototipo y este documento, prevalece este documento. Las diferencias conocidas están listadas en la sección 8. En el Anexo A se incluyen capturas de las pantallas principales.

### 1.6 Glosario

| **Término**   | **Significado**                                                                                                                                   |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| Instalador    | Cliente que compra material para hacer sus propias instalaciones. Se le aplica el precio instalador (mayorista).                                  |
| Cliente final | Persona o empresa dueña del lugar donde se instalan las cámaras. Se le aplica el precio cliente final.                                            |
| TRM           | Tasa Representativa del Mercado: valor oficial del dólar en pesos colombianos, publicado a diario por la Superintendencia Financiera de Colombia. |
| Tasa USD/VES  | Valor del dólar en bolívares venezolanos. La registra el usuario manualmente cada día.                                                            |
| Anulación     | Forma de corregir un documento guardado: queda visible marcado como anulado y se revierte su efecto en el inventario.                             |
| Kárdex        | Historial de entradas y salidas de un producto, con el saldo después de cada movimiento.                                                          |
| Serial        | Número de serie único que identifica una unidad física de un equipo (cámara, DVR, disco).                                                         |
| Stock mínimo  | Cantidad por debajo de la cual el sistema avisa que hay que volver a comprar.                                                                     |
| Consecutivo   | Número único y automático de cada documento (ej. V-0319, I-0088, COT-0045).                                                                       |
| DVR / NVR     | Grabadores de video. DVR para cámaras análogas, NVR para cámaras IP.                                                                              |

## 2. Usuarios y acceso

El sistema será usado por dos personas: Jose y Victor. Ambos realizan las instalaciones y administran la bodega.

| **ID** | **Requerimiento**                                                                                                                                                                                                                                              |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RU-01  | El sistema tendrá dos usuarios iniciales, Jose y Victor, cada uno con su propio usuario y contraseña.                                                                                                                                                          |
| RU-02  | Ambos usuarios tendrán acceso completo a todas las funciones (no se requieren roles diferenciados por ahora).                                                                                                                                                  |
| RU-03  | Cada movimiento (compra, venta, instalación, cotización, ajuste, anulación y registro o corrección de tasas) quedará registrado con el usuario que lo hizo, la fecha y la hora, y esa información se mostrará en listados, kárdex y detalle de cada documento. |
| RU-04  | Se podrán agregar más usuarios en el futuro si la empresa crece.                                                                                                                                                                                               |
| RU-05  | Pantalla de ingreso con usuario y contraseña. Opción para cerrar sesión desde el menú (junto al nombre del usuario).                                                                                                                                           |
| RU-06  | Los técnicos disponibles para asignar a una instalación serán los usuarios del sistema (Jose y Victor).                                                                                                                                                        |
| RU-07  | El usuario podrá cambiar su contraseña.                                                                                                                                                                                                                        |

## 3. Requerimientos funcionales

Esta sección describe cada módulo del sistema: qué hace, qué información maneja y cómo se comporta. Los módulos siguen el orden del menú del prototipo.

### 3.1 Navegación general

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                       |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-01  | En computador: menú lateral fijo con las opciones Inicio, Inventario, Nueva venta, Nueva instalación, Compras, Cotizaciones, Clientes y Reportes. En la parte inferior del menú se muestran las tasas de cambio del día (TRM y tasa del bolívar) y el usuario conectado con la opción de cerrar sesión. |
| RF-02  | La opción Inventario del menú mostrará un indicador con la cantidad de productos por debajo del stock mínimo.                                                                                                                                                                                           |
| RF-03  | En celular: barra superior con el nombre de la empresa, la TRM del día y el usuario; y barra inferior con Inicio, Inventario, Nuevo (+), Clientes y Reportes.                                                                                                                                           |
| RF-04  | El botón Nuevo (+) del celular abrirá un menú con: Nueva venta, Nueva instalación, Registrar compra y Nueva cotización.                                                                                                                                                                                 |
| RF-05  | Todos los formularios mostrarán, en la parte superior, el consecutivo que tendrá el documento, la fecha y el usuario que lo registra.                                                                                                                                                                   |

### 3.2 Pantalla de inicio (panel principal)

Es la primera pantalla después de ingresar. Muestra el estado del negocio del mes en curso.

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                                         |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-06  | Saludo con el nombre del usuario y la fecha actual.                                                                                                                                                                                                                                                                       |
| RF-07  | Si no se ha registrado la tasa del bolívar del día (o la TRM automática falló), se muestra un aviso destacado con el botón Registrar tasa, antes que cualquier otro bloque.                                                                                                                                               |
| RF-08  | Cuatro indicadores del mes en curso, cada uno con su valor en la moneda principal y su equivalente en las otras monedas: Ventas del mes (con cantidad de ventas), Instalaciones del mes (con cantidad), Utilidad del mes (con porcentaje sobre lo cobrado) e Inventario valorizado (con cantidad de productos en bodega). |
| RF-09  | Accesos rápidos: Nueva venta, Nueva instalación, Registrar compra y Nueva cotización.                                                                                                                                                                                                                                     |
| RF-10  | Bloque Stock bajo: lista de productos por debajo del stock mínimo, con cantidad actual, mínimo y una barra de nivel. Enlace a Ver inventario.                                                                                                                                                                             |
| RF-11  | Bloque Garantías por vencer: instalaciones y ventas con garantía que vence en los próximos 30 días, con cliente, consecutivo, fecha de vencimiento y días restantes.                                                                                                                                                      |
| RF-12  | Bloque Cotizaciones en evaluación: cotizaciones enviadas pendientes de respuesta del cliente, con cliente, valor y días que faltan para vencer.                                                                                                                                                                           |
| RF-13  | Bloque Últimos movimientos: los movimientos más recientes (ventas, instalaciones, compras, ajustes) con tipo, consecutivo, cliente o proveedor, usuario, fecha/hora y valor. Al tocar un movimiento se abre su detalle.                                                                                                   |

### 3.3 Catálogo de productos

Actualmente se manejan alrededor de 10 productos: todo el material necesario para una instalación de cámaras de seguridad. Todo se vende por unidad (o por metro en el caso del cable); no se manejan kits ni combos.

#### Datos de un producto

| **Campo**            | **Tipo / valores**   | **Obligatorio** | **Notas**                                                                |
|----------------------|----------------------|-----------------|--------------------------------------------------------------------------|
| Código               | Texto corto          | Sí              | Único. Ej: CAM-D2. Se usa como prefijo de referencia.                    |
| Nombre               | Texto                | Sí              | Ej: Cámara domo 2MP.                                                     |
| Marca                | Texto                | No              | Ej: Hikvision, Dahua.                                                    |
| Modelo               | Texto                | No              | Ej: DS-2CE56D0T.                                                         |
| Categoría            | Lista                | Sí              | Ver categorías iniciales.                                                |
| Unidad de medida     | Unidad / Metro / Par | Sí              | El cable se maneja en metros; los balunes en pares.                      |
| Controla serial      | Sí / No              | Sí              | Sí para todo equipo con serial de fábrica.                               |
| Costo actual         | USD (calculado)      | —               | Lo calcula el sistema con cada compra; no se digita (sección 3.8).       |
| Precio instalador    | Moneda               | Sí              | Principalmente en USD.                                                   |
| Precio cliente final | Moneda               | Sí              | Principalmente en USD.                                                   |
| Moneda del precio    | USD / COP / VES      | Sí              | Por defecto USD.                                                         |
| Stock mínimo         | Número               | No              | Alerta al quedar por debajo.                                             |
| Foto                 | Imagen               | No              |                                                                          |
| Descripción          | Texto largo          | No              |                                                                          |
| Activo               | Sí / No              | Sí              | Un producto inactivo no aparece para vender, pero conserva su historial. |

| **ID** | **Requerimiento**                                                                                                                                                                                                         |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-14  | Crear, editar y desactivar productos. No se podrá eliminar un producto que ya tenga movimientos.                                                                                                                          |
| RF-15  | Categorías iniciales: Cámaras, Grabadores (DVR/NVR), Discos duros, Cable, Balunes, Fuentes de poder, Conectores y Cajas y accesorios. El usuario podrá crear, editar o eliminar categorías (solo si no tienen productos). |
| RF-16  | Al crear un producto, queda con stock 0. El stock y el costo se cargan al registrar la primera compra.                                                                                                                    |
| RF-17  | No habrá límite en la cantidad de productos que se pueden registrar.                                                                                                                                                      |
| RF-18  | Cada producto tendrá un precio de venta para instaladores y un precio de venta para cliente final.                                                                                                                        |

### 3.4 Números de serie y cable por metro

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                                            |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-19  | Llevan serial todos los equipos que traen número de serie de fábrica (por ejemplo cámaras, grabadores DVR/NVR, discos duros y fuentes de poder); se controlan por cantidad y por serial. El cable, los conectores y demás consumibles se controlan solo por cantidad.                                                        |
| RF-20  | Al registrar una compra de un producto con serial, se ingresará el número de serie de cada unidad (digitado o leído con la cámara del celular como código de barras). La cantidad de seriales debe coincidir con la cantidad comprada y no se permitirán seriales repetidos; si no coinciden, no se puede guardar la compra. |
| RF-21  | Al vender o usar en una instalación un producto con serial, se seleccionará qué serial(es) salen. El sistema solo mostrará los seriales disponibles en bodega y la cantidad se calculará según los seriales elegidos.                                                                                                        |
| RF-22  | Estados de un serial: En bodega, Vendido (con consecutivo de la venta), Instalado (con consecutivo de la instalación) y Dado de baja (por ajuste de pérdida o daño). Un serial vendido, instalado o dado de baja nunca aparece como disponible.                                                                              |
| RF-23  | Garantía de equipos: todo equipo con serial tiene 3 meses de garantía contados desde la fecha de la venta o de la instalación en que salió. El vencimiento se calcula automáticamente y queda guardado en cada serial.                                                                                                       |
| RF-24  | Buscar un número de serie desde cualquier pantalla y ver su historial completo: proveedor y compra de entrada, fecha, y a qué cliente se vendió o en qué instalación se usó, con su garantía.                                                                                                                                |
| RF-25  | El cable se manejará en metros: la compra se registra en metros y la venta o el uso en instalaciones se descuenta en metros. No se requiere conversión automática entre bobinas y metros.                                                                                                                                    |

### 3.5 Monedas y tasas de cambio

El sistema manejará tres monedas: dólar estadounidense (USD), que es la moneda principal del negocio; peso colombiano (COP) y bolívar venezolano (VES).

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                                                                                                                                        |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-26  | Toda compra, venta, instalación o cotización podrá registrarse en USD, COP o VES mediante un selector de moneda en el formulario.                                                                                                                                                                                                                                                                                        |
| RF-27  | Los precios de venta de los productos se definirán principalmente en USD. El sistema permitirá, si se requiere, fijar el precio de un producto en otra moneda.                                                                                                                                                                                                                                                           |
| RF-28  | El sistema consultará automáticamente, cada día a primera hora, la TRM (USD/COP) publicada por la Superintendencia Financiera de Colombia.                                                                                                                                                                                                                                                                               |
| RF-29  | La tasa USD/VES la ingresará manualmente el usuario cada día, desde el aviso de la pantalla de Inicio o desde el recuadro de tasas del menú.                                                                                                                                                                                                                                                                             |
| RF-30  | Las tasas del día se mostrarán siempre visibles (menú lateral en computador, barra superior en celular), con la fecha, la fuente (Superfinanciera o manual), la hora y el usuario que la registró.                                                                                                                                                                                                                       |
| RF-31  | Todos los precios y totales se mostrarán en la moneda del documento y su equivalente en las demás monedas, convertidos con las tasas vigentes.                                                                                                                                                                                                                                                                           |
| RF-32  | Cada transacción guardará las tasas con las que se registró (se muestra en el formulario: "TRM \$ 3.912,45 · se guarda con la venta"), para que los valores históricos no cambien cuando cambien las tasas.                                                                                                                                                                                                              |
| RF-33  | Si la consulta automática de la TRM falla, o si no se ha registrado la tasa del bolívar del día, el sistema usará la última tasa disponible y mostrará un aviso visible para actualizarla. La TRM también podrá ingresarse manualmente en caso de falla.                                                                                                                                                                 |
| RF-34  | El sistema guardará un historial diario de las tasas de cambio, consultable por fecha, indicando si cada tasa fue automática o manual y qué usuario la registró.                                                                                                                                                                                                                                                         |
| RF-35  | Doble confirmación al registrar una tasa manual (USD/VES, o TRM cuando falle la consulta automática): (a) el usuario digita el valor dos veces y ambos deben coincidir; (b) antes de guardar, el sistema muestra la tasa anterior, la nueva y el porcentaje de variación; (c) si la variación supera un límite configurable (por defecto 5 %), se muestra una alerta destacada que el usuario debe aceptar expresamente. |
| RF-36  | Una tasa mal registrada podrá corregirse, quedando registro del valor anterior, el nuevo y el usuario. Las transacciones ya guardadas conservan la tasa con la que se registraron; si una compra quedó con una tasa errada, se corrige anulándola y registrándola de nuevo (sección 3.9).                                                                                                                                |

### 3.6 Proveedores y compras

#### Proveedores

| **Campo**             | **Tipo / valores** | **Obligatorio** | **Notas**                                       |
|-----------------------|--------------------|-----------------|-------------------------------------------------|
| Nombre / razón social | Texto              | Sí              |                                                 |
| NIT o documento       | Texto              | No              |                                                 |
| Teléfono              | Texto              | No              |                                                 |
| Correo                | Correo             | No              |                                                 |
| Ciudad                | Texto              | No              |                                                 |
| Moneda habitual       | USD / COP / VES    | Sí              | Se propone por defecto al registrar una compra. |

| **ID** | **Requerimiento**                                                                                                                              |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-37  | Crear, editar y consultar proveedores. La pantalla de Compras mostrará la lista de proveedores con su moneda habitual, NIT, ciudad y teléfono. |
| RF-38  | Consultar el historial de compras de cada proveedor.                                                                                           |

#### Registro de una compra

| **ID** | **Requerimiento**                                                                                                                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-39  | Registrar compras con: consecutivo automático (C-0001), fecha, proveedor, número de factura del proveedor, moneda, tasas del día, productos, cantidades y costo unitario.                                 |
| RF-40  | Al elegir el proveedor, se propone su moneda habitual; el usuario puede cambiarla.                                                                                                                        |
| RF-41  | Para cada producto agregado, el sistema mostrará en tiempo real cómo cambiará su costo en USD y qué regla se aplica: "Costo US\$ 20,00 → US\$ 19,50 · promedio" o "Costo US\$ 20,00 → US\$ 22,00 · sube". |
| RF-42  | Cada línea mostrará cantidad, costo unitario en la moneda de la factura y subtotal en las demás monedas. Si se cambia la moneda de la factura, los costos se convierten automáticamente.                  |
| RF-43  | Para productos con serial, cada línea pedirá los números de serie de las unidades compradas.                                                                                                              |
| RF-44  | Adjuntar foto o PDF de la factura del proveedor (desde el celular se podrá tomar la foto directamente).                                                                                                   |
| RF-45  | Botón Guardar y sumar al inventario: al guardar, las cantidades se suman al stock, se actualiza el costo y se registran los movimientos en el kárdex.                                                     |

#### Listado de compras

| **ID** | **Requerimiento**                                                                                                                                                                                                                                     |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-46  | Listado de compras del mes con el total comprado. Cada compra muestra: proveedor, consecutivo, fecha, número de factura, usuario, resumen de productos, total en la moneda original y su equivalente, la tasa guardada y acceso a la factura adjunta. |
| RF-47  | Filtrar compras por proveedor, producto y rango de fechas.                                                                                                                                                                                            |
| RF-48  | Consultar el detalle de una compra. Una compra guardada no se edita ni se borra: solo se anula, bajo las condiciones de la sección 3.9.                                                                                                               |

### 3.7 Inventario

#### Listado de inventario

| **ID** | **Requerimiento**                                                                                                                                                                                          |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-49  | Encabezado con la cantidad de productos y el valor total del inventario en todas las monedas.                                                                                                              |
| RF-50  | Botones: Exportar a Excel, Registrar compra y Nuevo producto.                                                                                                                                              |
| RF-51  | Filtro por categoría (Todas, Cámaras, Grabadores, etc.) y buscador por nombre, código, marca o serial.                                                                                                     |
| RF-52  | Cada producto muestra: nombre, código, categoría, marca, stock con su unidad, etiqueta Bajo si está por debajo del mínimo, costo actual en USD con su equivalente, y valor en bodega en todas las monedas. |

#### Detalle de un producto

| **ID** | **Requerimiento**                                                                                                                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-53  | Datos del producto (código, categoría, nombre, marca, modelo, unidad) y botones Editar y Ajustar inventario.                                                                                              |
| RF-54  | Indicadores: stock actual y mínimo, costo actual, valor en bodega, precio instalador y precio cliente final, cada uno en todas las monedas.                                                               |
| RF-55  | Para productos con serial: lista de números de serie con su estado (En bodega, Vendido · V-0318, Instalado · I-0087, Dado de baja). Al tocar un serial se ve su historial.                                |
| RF-56  | Kárdex del producto con columnas: Fecha, Movimiento (Compra, Venta, Instalación, Ajuste con su motivo, Anulación), Documento, Entrada, Salida, Saldo y Usuario. Al tocar un documento se abre su detalle. |
| RF-57  | Historial de cambios de costo: fecha, compra que lo originó, moneda y tasa de la factura, costo anterior, costo nuevo y regla aplicada (sube o promedio).                                                 |

#### Ajustes de inventario

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                           |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-58  | Ajuste manual desde el detalle del producto, con: motivo (Pérdida, Daño, Conteo físico, Garantía u Otro con descripción), cantidad (positiva = entrada, negativa = salida) y vista previa del nuevo stock antes de guardar.                                                 |
| RF-59  | Para productos con serial, en un ajuste de salida se debe elegir qué serial(es) se dan de baja; en un ajuste de entrada se deben ingresar los seriales.                                                                                                                     |
| RF-60  | Cada ajuste tiene consecutivo automático (AJ-001) y queda en el kárdex y en los últimos movimientos.                                                                                                                                                                        |
| RF-61  | Los ajustes de entrada (por ejemplo, unidades de más encontradas en un conteo físico) ingresan al costo vigente del producto y no aplican la regla de costo de la sección 3.8. Si el producto nunca ha tenido compras, el usuario deberá ingresar el costo unitario en USD. |
| RF-62  | Un ajuste de salida no puede dejar el stock en negativo.                                                                                                                                                                                                                    |

#### Alertas y validaciones de stock

| **ID** | **Requerimiento**                                                                                                                                                                                       |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-63  | Mostrar alerta cuando un producto quede por debajo de su stock mínimo (etiqueta Bajo, bloque en Inicio, indicador en el menú y reporte).                                                                |
| RF-64  | Al vender, instalar o convertir una cotización, cada línea mostrará la cantidad disponible ("hay 24 und"). Si la cantidad pedida supera el stock, se mostrará el aviso "Stock insuficiente · quedan N". |
| RF-65  | Se impide registrar cualquier salida (venta, instalación o ajuste) mayor al stock disponible: el botón Guardar queda bloqueado hasta corregir la cantidad. El stock nunca queda en negativo.            |

### 3.8 Cálculo del costo del inventario

El costo de cada producto se lleva siempre en dólares (USD), que es la moneda principal del negocio. Se recalcula con cada nueva compra (factura), comparando el costo unitario de la nueva factura con el costo actual del producto en bodega:

- **Conversión a USD:** si la factura viene en COP o VES, su costo unitario se convierte a USD con la tasa del día de la factura (TRM para COP y tasa registrada para VES). La factura conserva su moneda y tasa originales.

- **Si el precio sube:** si la nueva factura, en USD, trae un precio mayor, todas las unidades en bodega pasan a costar el precio mayor.

- **Si el precio baja:** si la nueva factura, en USD, trae un precio menor o igual, el costo se calcula como promedio ponderado en USD entre las unidades en bodega y las nuevas.

- **Sin stock:** si el producto no tiene unidades en bodega al momento de la compra, el costo pasa a ser el de la nueva factura.

Ejemplos:

| **Caso**             | **Situación**                                                                           | **Resultado**                                  |
|----------------------|-----------------------------------------------------------------------------------------|------------------------------------------------|
| Sube                 | Hay 10 cámaras a US\$20. Se compran 10 más a US\$25.                                    | Las 20 cámaras quedan a US\$25.                |
| Baja                 | Hay 10 cámaras a US\$20. Se compran 10 más a US\$15.                                    | (10 × 20 + 10 × 15) ÷ 20 = US\$17,50 cada una. |
| Baja, factura en COP | Hay 10 cámaras a US\$20. Se compran 10 en COP a \$76.000 con TRM de \$4.000 (= US\$19). | (10 × 20 + 10 × 19) ÷ 20 = US\$19,50 cada una. |
| Sube, factura en COP | Hay 10 cámaras a US\$20. Se compran 10 en COP a \$88.000 con TRM de \$4.000 (= US\$22). | Las 20 cámaras quedan a US\$22.                |

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-66  | Aplicar automáticamente la regla de costo descrita al registrar cada compra, incluida la conversión a USD y el caso sin stock.                                                                                                                                                                                                                                                                                                                                   |
| RF-67  | Guardar el historial de cambios de costo por producto: fecha, factura, moneda y tasa de la factura, costo anterior, costo nuevo y regla aplicada (sube o promedio).                                                                                                                                                                                                                                                                                              |
| RF-68  | La utilidad de ventas e instalaciones se calculará con el costo vigente del producto en el momento de la salida, y ese costo quedará guardado en el documento; cambios de costo posteriores no alteran la utilidad ya registrada.                                                                                                                                                                                                                                |
| RF-69  | Al cotizar, vender o registrar una instalación, el sistema mostrará para cada producto su costo en USD, su equivalente en COP y VES con las tasas del día, y su equivalente con las tasas de la última compra, para que se vea el efecto del cambio. Ejemplo: con un costo de US\$19,50, TRM de hoy \$4.200 y TRM de la última compra \$4.000, se muestra \$81.900 a la tasa de hoy frente a \$78.000 a la tasa de compra (diferencia de \$3.900 por el cambio). |

### 3.9 Anulación de documentos

Como la regla de costo depende del orden de las compras, los documentos guardados no se editan: se anulan y, si es necesario, se registran de nuevo.

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                                                                                                                |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-70  | Las compras, ventas, instalaciones y ajustes guardados no se podrán editar en sus cantidades, costos, precios, monedas ni tasas. Los datos descriptivos (observaciones, fotos, descripción del trabajo) sí podrán editarse.                                                                                                                                                                      |
| RF-71  | Una compra solo podrá anularse si es la compra más reciente de cada producto incluido y todas sus unidades siguen en bodega (incluidos sus seriales). Al anularla, se descuentan las cantidades y el costo de cada producto vuelve al valor anterior según el historial de costos. Si no se cumplen estas condiciones, el sistema lo indica y la corrección se hará con un ajuste de inventario. |
| RF-72  | Al anular una venta o una instalación, el material regresa al inventario al costo vigente del producto y sus seriales vuelven a quedar disponibles en bodega.                                                                                                                                                                                                                                    |
| RF-73  | Toda anulación exigirá indicar el motivo, quedará registrada con usuario, fecha y hora, y el documento anulado seguirá visible marcado como anulado (en listados, kárdex y reportes, sin sumar en los totales).                                                                                                                                                                                  |
| RF-74  | Si la venta o instalación anulada provenía de una cotización, la cotización vuelve al estado Aprobada para poder convertirse de nuevo si se requiere.                                                                                                                                                                                                                                            |

### 3.10 Clientes

#### Datos de un cliente

| **Campo**             | **Tipo / valores**         | **Obligatorio** | **Notas**                                    |
|-----------------------|----------------------------|-----------------|----------------------------------------------|
| Tipo de cliente       | Instalador / Cliente final | Sí              | Define el precio que se aplica.              |
| Nombre o razón social | Texto                      | Sí              |                                              |
| Documento             | CC / NIT                   | No              | Aparece en cotizaciones y comprobantes.      |
| Teléfono / WhatsApp   | Texto                      | Sí              | Número al que se envían los PDF.             |
| Correo                | Correo                     | No              |                                              |
| Dirección             | Texto                      | No              | Se propone como dirección de la instalación. |
| Ciudad                | Texto                      | No              |                                              |

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                        |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-75  | Crear y editar clientes. Al elegir el tipo, el formulario indica qué precio se le aplicará ("Se le aplicará el precio instalador").                                                                                                                                                                      |
| RF-76  | Listado de clientes con filtro Todos / Instaladores / Clientes finales y buscador. Cada cliente muestra: iniciales, nombre, tipo, teléfono, ciudad, cantidad de movimientos y fecha del último.                                                                                                          |
| RF-77  | Detalle del cliente: datos de contacto, botón de WhatsApp que abre el chat, botones para iniciar una Venta, Instalación o Cotización con ese cliente ya seleccionado, precio que se le aplica, cantidad de compras de material e instalaciones, e historial con consecutivo, descripción, fecha y valor. |
| RF-78  | El tipo de cliente determinará automáticamente qué precio se aplica (precio instalador o precio cliente final), con posibilidad de modificar el precio manualmente en cada línea del documento.                                                                                                          |
| RF-79  | Se podrá crear un cliente nuevo directamente desde los formularios de venta, instalación y cotización, sin salir de ellos.                                                                                                                                                                               |

### 3.11 Cotizaciones

Las cotizaciones permiten presentarle al cliente una propuesta de venta de material o de instalación. Mientras el cliente la evalúa, la cotización queda en seguimiento; cuando el cliente la aprueba, se convierte en venta o en instalación con un solo clic.

#### Ciclo de vida de una cotización

| **Estado**    | **Qué significa**                               | **Cómo llega a ese estado**                                                          |
|---------------|-------------------------------------------------|--------------------------------------------------------------------------------------|
| Borrador      | Se está elaborando. Se puede editar libremente. | Al crear la cotización.                                                              |
| En evaluación | Se envió al cliente y se espera su respuesta.   | Al enviarla por WhatsApp o descargar el PDF, o al marcarla manualmente como enviada. |
| Aprobada      | El cliente aceptó.                              | El usuario marca Cliente aprobó. Luego la convierte.                                 |
| Convertida    | Ya se generó la venta o instalación.            | Automáticamente al convertir. Queda enlazada al documento generado.                  |
| Rechazada     | El cliente no aceptó.                           | El usuario la marca como rechazada, con motivo opcional (precio, competencia, otro). |
| Vencida       | Pasó la fecha de validez sin respuesta.         | Automáticamente al cumplirse la fecha de validez.                                    |

#### Elaboración

| **ID** | **Requerimiento**                                                                                                                                                                                                                               |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-80  | Crear cotizaciones con consecutivo automático (COT-0001) de dos tipos: Instalación o Venta de material.                                                                                                                                         |
| RF-81  | Cada ítem muestra el costo con las tasas de hoy y de la última compra, para cotizar con margen real (sección 3.8).                                                                                                                              |
| RF-82  | Descuento opcional por cualquier porcentaje o valor, a decisión del usuario.                                                                                                                                                                    |
| RF-83  | Datos de la cotización: cliente, tipo, validez (8, 15 o 30 días; por defecto 15), fecha de vencimiento calculada, moneda, ítems (producto, cantidad, precio unitario) y, si es de instalación, valor de mano de obra y descripción del trabajo. |
| RF-84  | Los precios se toman automáticamente según el tipo de cliente y se pueden modificar.                                                                                                                                                            |
| RF-85  | Vista previa del PDF en tiempo real al lado del formulario (en computador) mientras se elabora.                                                                                                                                                 |
| RF-86  | La cotización no aparta ni descuenta material. El inventario solo se descuenta al convertirla en instalación o venta; en ese momento el sistema valida el stock disponible y no permite guardar si no alcanza.                                  |
| RF-87  | Duplicar una cotización existente para crear una nueva similar.                                                                                                                                                                                 |
| RF-88  | Una cotización en evaluación se puede editar; al hacerlo, se guarda como nueva versión y se puede reenviar.                                                                                                                                     |

#### Listado y seguimiento

| **ID** | **Requerimiento**                                                                                                                                    |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-89  | Listado de cotizaciones con filtro por estado (Borrador, En evaluación, Aprobada, Convertida, Rechazada, Vencida), por cliente y por fecha.          |
| RF-90  | Cada cotización muestra: consecutivo, cliente, tipo, fecha, valor total, estado y días que faltan para vencer.                                       |
| RF-91  | Las cotizaciones en evaluación próximas a vencer (3 días o menos) se resaltan y aparecen en la pantalla de Inicio para hacer seguimiento al cliente. |
| RF-92  | Botón para escribirle al cliente por WhatsApp desde la cotización (seguimiento).                                                                     |

#### Conversión a venta o instalación

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                        |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-93  | Botón Cliente aprobó · convertir en instalación (o en venta, según el tipo de cotización).                                                                                                                                                                                               |
| RF-94  | Al convertir, se abre el formulario de instalación o venta con todos los datos de la cotización ya cargados (cliente, productos, cantidades, precios, mano de obra, descripción y moneda). El usuario completa lo que falte: seriales de los equipos, técnicos, fecha, fotos y garantía. |
| RF-95  | Al guardar, la cotización pasa a Convertida y queda enlazada con la venta o instalación generada (desde una se puede ir a la otra).                                                                                                                                                      |
| RF-96  | Si los precios, costos o el stock cambiaron desde la cotización, el sistema avisa antes de guardar. Se respetan los precios cotizados salvo que el usuario los cambie. Si no hay stock suficiente no se puede guardar.                                                                   |

### 3.12 Ventas de material

Todas las ventas son de contado; no se requiere control de crédito ni de cuentas por cobrar. Tampoco se registrarán pagos ni medios de pago (efectivo, transferencia, etc.) en el sistema.

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                 |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-97  | Formulario de nueva venta con consecutivo automático (V-0001), fecha y usuario, organizado en dos pasos: 01 Cliente y 02 Productos, más un resumen lateral.                                                                                                                                       |
| RF-98  | Paso Cliente: elegir el cliente; se muestra su tipo y el precio que se le aplicará. Elegir la moneda de la venta; se muestra la tasa que se guardará con la venta.                                                                                                                                |
| RF-99  | Paso Productos: agregar productos desde botones o buscador. Cada línea muestra precio por unidad, costo con las tasas de hoy y de la última compra (sección 3.8), cantidad disponible, seriales asignados (si aplica), cantidad, subtotal en la moneda de la venta y su equivalente en las otras. |
| RF-100 | Resumen: subtotal, descuento (cualquier porcentaje o valor, a decisión del usuario; por defecto 0), total de contado en todas las monedas, costo del material y utilidad en valor y porcentaje.                                                                                                   |
| RF-101 | Si hay productos sin stock suficiente, se muestra el aviso en la línea y no se permite guardar hasta corregir la cantidad.                                                                                                                                                                        |
| RF-102 | Al guardar, las cantidades y seriales se descuentan del inventario y se registran en el kárdex.                                                                                                                                                                                                   |
| RF-103 | Los equipos con serial vendidos quedan con garantía de 3 meses desde la fecha de la venta.                                                                                                                                                                                                        |
| RF-104 | Pantalla de confirmación: "Venta V-0319 registrada" con cliente y total, y botones Enviar comprobante por WhatsApp, Descargar PDF y Registrar otra venta.                                                                                                                                         |
| RF-105 | Listado de ventas con filtros por cliente, producto y fecha, y detalle de cada venta.                                                                                                                                                                                                             |
| RF-106 | Una venta guardada no se edita ni se borra: solo se anula con motivo (sección 3.9).                                                                                                                                                                                                               |

### 3.13 Instalaciones

El formulario de instalación se organiza en cuatro pasos (01 Cliente y trabajo, 02 Material usado, 03 Fotos, 04 Garantía) más un panel lateral de Cobro.

#### 01 Cliente y trabajo

| **ID** | **Requerimiento**                                                                                                                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-107 | Consecutivo automático (I-0001), cliente, dirección de la instalación (se propone la del cliente y se puede cambiar), fecha, técnico(s) que la realizaron (selección múltiple) y descripción del trabajo. |

#### 02 Material usado

| **ID** | **Requerimiento**                                                                                                                                                                                                                                |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-108 | Agregar el material usado, igual que en una venta: precio según el tipo de cliente, costo con las tasas de hoy y de la última compra, cantidad disponible, seriales, cantidad y subtotal. No se permite usar más material del que hay en bodega. |
| RF-109 | Ese material se descontará automáticamente del inventario al guardar.                                                                                                                                                                            |

#### 03 Fotos

| **ID** | **Requerimiento**                                                                                                            |
|--------|------------------------------------------------------------------------------------------------------------------------------|
| RF-110 | Adjuntar fotos en tres grupos: Antes, Durante y Después. Desde el celular se podrá tomar la foto directamente con la cámara. |
| RF-111 | Cada grupo muestra cuántas fotos tiene. Se podrán agregar fotos después de guardada la instalación.                          |
| RF-112 | Las fotos se comprimen automáticamente para no ocupar demasiado espacio.                                                     |

#### 04 Garantía

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                       |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-113 | Garantía de mano de obra (instalación): máximo 3 meses desde la fecha de la instalación; por defecto 3 meses, con opción de un plazo menor. Se muestra la fecha de vencimiento calculada, y la instalación aparece en Garantías por vencer durante sus últimos 30 días. |
| RF-114 | Garantía de equipos: 3 meses desde la fecha de la instalación para todos los equipos instalados (no se elige plazo). La fecha de vencimiento se muestra y queda en cada serial.                                                                                         |
| RF-115 | Condiciones de la garantía: texto con un valor por defecto configurable (ej. "No cubre daños por descargas eléctricas, humedad o manipulación de terceros.") que se puede modificar en cada instalación.                                                                |

#### Cobro

| **ID** | **Requerimiento**                                                                                                                                                                              |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-116 | Selector de moneda del cobro.                                                                                                                                                                  |
| RF-117 | Material cobrado: suma automática del material a precio de venta.                                                                                                                              |
| RF-118 | Mano de obra: valor digitado por el usuario.                                                                                                                                                   |
| RF-119 | Total cobrado en todas las monedas, costo real del material y utilidad en valor y porcentaje.                                                                                                  |
| RF-120 | Pantalla de confirmación: "Instalación I-0088 registrada" con cliente, total y vencimiento de garantía, y botones Enviar comprobante por WhatsApp, Descargar PDF y Registrar otra instalación. |
| RF-121 | Listado de instalaciones con filtros por cliente, técnico, fecha y estado de garantía, y detalle de cada instalación con sus fotos.                                                            |
| RF-122 | De una instalación guardada solo se pueden editar los datos descriptivos (fotos, descripción, condiciones). Para corregir material o valores se anula con motivo (sección 3.9).                |

### 3.14 Garantías

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                                                                                |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-123 | Todas las garantías (mano de obra y equipos) son de máximo 3 meses. Consulta de garantías con estados: Vigente, Por vencer (30 días o menos) y Vencida, filtrable por cliente y por tipo (instalación o venta).                                                                                                                                                  |
| RF-124 | Buscar por serial o por cliente para saber si un equipo está en garantía, hasta cuándo, y en qué venta o instalación salió.                                                                                                                                                                                                                                      |
| RF-125 | Registrar un reclamo de garantía sobre una instalación o un serial: fecha, descripción del problema y solución, como nota de seguimiento. El manejo del equipo dañado (cambio, baja o devolución al proveedor) es un proceso manual a cargo de la empresa; si un equipo de reemplazo sale de la bodega, se registra con un ajuste de salida con motivo Garantía. |

### 3.15 Comprobantes, cotizaciones en PDF y WhatsApp

| **ID** | **Requerimiento**                                                                                                                                                                                            |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-126 | Se generarán tres tipos de PDF: Cotización, Comprobante de venta y Comprobante de instalación.                                                                                                               |
| RF-127 | Encabezado del PDF: logo y nombre de la empresa, lema ("Instalación de cámaras de seguridad"), ciudad y teléfono; a la derecha, tipo de documento y consecutivo, fecha y, en cotizaciones, fecha de validez. |
| RF-128 | Datos del cliente: nombre, documento (CC/NIT) y dirección.                                                                                                                                                   |
| RF-129 | Tabla de ítems: descripción, cantidad con unidad, valor unitario y total. La mano de obra aparece como una línea más ("Mano de obra · instalación y configuración").                                         |
| RF-130 | Totales en la moneda del documento y en las otras monedas que el usuario elija mostrar, con la tasa de referencia y su fecha.                                                                                |
| RF-131 | Pie con condiciones: pago de contado, garantía de 3 meses (equipos y mano de obra) y la leyenda "Documento no válido como factura".                                                                          |
| RF-132 | El comprobante de instalación incluirá la fecha de vencimiento de las garantías y los seriales de los equipos instalados.                                                                                    |
| RF-133 | Botón Descargar PDF.                                                                                                                                                                                         |
| RF-134 | Botón Enviar por WhatsApp: desde el celular abrirá la opción de compartir con el PDF adjunto; en computador abrirá WhatsApp con el número del cliente y un mensaje con el enlace para descargar el PDF.      |

### 3.16 Reportes

| **ID** | **Requerimiento**                                                                                                                                                   |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-135 | Selector de período: Semana, Mes o Año, con flechas para moverse entre períodos, y opción de rango de fechas libre.                                                 |
| RF-136 | Indicadores del período: Ventas de material, Instalaciones (con cantidad), Utilidad total (con porcentaje de margen) e Inventario valorizado, en todas las monedas. |
| RF-137 | Gráfico de barras de ventas e instalaciones por semana (o por mes, si el período es el año).                                                                        |
| RF-138 | Productos más vendidos y más usados en instalaciones, con cantidad y unidad.                                                                                        |
| RF-139 | Compras por proveedor en el período, con gráfico de barras.                                                                                                         |
| RF-140 | Productos por debajo del stock mínimo, con enlace al inventario.                                                                                                    |
| RF-141 | Consulta de seriales: disponibles en bodega, vendidos y usados en instalaciones.                                                                                    |
| RF-142 | Cotizaciones del período: cantidad por estado y porcentaje de cotizaciones aprobadas.                                                                               |
| RF-143 | Utilidad por venta y por instalación (listado con costo, cobrado y utilidad de cada documento).                                                                     |
| RF-144 | Botón Excel para exportar cualquier reporte.                                                                                                                        |

### 3.17 Configuración

| **ID** | **Requerimiento**                                                                                                                                     |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-145 | Datos de la empresa para los PDF: nombre (ITALARM), logo, lema, NIT, ciudad, teléfono y correo.                                                       |
| RF-146 | Límite de variación para la alerta de tasas manuales (por defecto 5 %).                                                                               |
| RF-147 | Valores por defecto: validez de las cotizaciones, garantía de mano de obra y de equipos (3 meses), texto de condiciones de garantía y pie de los PDF. |
| RF-148 | Gestión de usuarios, categorías y unidades de medida.                                                                                                 |

### 3.18 Carga inicial de datos

ITALARM ya tiene material en bodega y clientes registrados en Excel. El sistema debe permitir cargarlos al arrancar, para no empezar vacío.

| **ID** | **Requerimiento**                                                                                                                                                                                                                                               |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RF-149 | Descargar una plantilla de Excel con hojas para: productos, inventario inicial (producto, cantidad, costo unitario en USD y seriales), clientes y proveedores.                                                                                                  |
| RF-150 | Al subir el archivo, el sistema valida todo antes de guardar (códigos repetidos, seriales repetidos o faltantes, cantidades y costos inválidos, categorías inexistentes) y muestra los errores por hoja y fila. Si hay un solo error, no se guarda nada.        |
| RF-151 | Si el archivo es válido, se crea un documento de Inventario inicial (consecutivo II-001) con un movimiento de entrada por producto en el kárdex. El costo inicial es el costo cargado en USD (no se aplica la regla de costo), y los seriales quedan En bodega. |
| RF-152 | La carga de inventario inicial de un producto solo se permite si ese producto no tiene movimientos. Queda registrada con usuario, fecha y hora.                                                                                                                 |

## 4. Reglas de negocio

Resumen de las reglas que el sistema debe aplicar automáticamente:

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                      |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RN-01  | El precio aplicado depende del tipo de cliente: instalador → precio instalador; cliente final → precio cliente final. Siempre puede modificarse manualmente en el documento.                                                                                           |
| RN-02  | El costo del inventario se lleva en USD. Sube al precio mayor cuando una compra (convertida a USD con la tasa de su día) trae un precio más alto, y se promedia cuando trae un precio igual o más bajo. Sin stock, toma el precio de la nueva compra (sección 3.8).    |
| RN-03  | Utilidad = total cobrado (material + mano de obra − descuento) − costo del material al momento de la salida. Porcentaje de utilidad = utilidad ÷ total cobrado.                                                                                                        |
| RN-04  | Cada documento guarda las tasas de cambio de su fecha; los valores históricos nunca se recalculan.                                                                                                                                                                     |
| RN-05  | Compras suman stock; ventas, instalaciones y ajustes de salida restan stock. El stock nunca puede quedar en negativo. Cada movimiento queda en el kárdex con usuario, fecha y hora.                                                                                    |
| RN-06  | Los ajustes de entrada ingresan al costo vigente y no modifican el costo del producto.                                                                                                                                                                                 |
| RN-07  | La TRM se obtiene automáticamente; la tasa del bolívar la registra el usuario con doble confirmación.                                                                                                                                                                  |
| RN-08  | Ningún documento guardado (compra, venta, instalación, ajuste) se edita en valores ni se borra: se anula con motivo y se revierte su efecto en el inventario. Una compra solo se anula si es la última de cada producto y sus unidades siguen en bodega (sección 3.9). |
| RN-09  | Los consecutivos son automáticos, únicos y no se reutilizan: C-0001 compras, V-0001 ventas, I-0001 instalaciones, COT-0001 cotizaciones, AJ-001 ajustes, II-001 inventario inicial.                                                                                    |
| RN-10  | Todas las garantías son de 3 meses: mano de obra desde la fecha de la instalación, y equipos desde la fecha de la venta o instalación.                                                                                                                                 |
| RN-11  | Las cotizaciones no apartan material; el inventario solo se descuenta al convertirlas.                                                                                                                                                                                 |
| RN-12  | El descuento es libre (cualquier porcentaje o valor), a decisión del usuario.                                                                                                                                                                                          |
| RN-13  | Una cotización vence automáticamente al pasar su fecha de validez si no fue aprobada ni rechazada.                                                                                                                                                                     |
| RN-14  | Una cotización convertida no se puede volver a convertir.                                                                                                                                                                                                              |

## 5. Modelo de datos (referencia para el programador)

Entidades principales que debe manejar el sistema y su relación. El programador puede ajustar el diseño técnico, siempre que se conserve esta información.

| **Entidad**         | **Información principal**                                                                                                                                                                | **Se relaciona con**                                              |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| Usuario             | Nombre, usuario, contraseña (cifrada), activo.                                                                                                                                           | Todos los documentos (quién registró).                            |
| Categoría           | Nombre.                                                                                                                                                                                  | Productos.                                                        |
| Producto            | Código, nombre, marca, modelo, unidad, controla serial, precios, moneda del precio, stock, costo actual (USD), stock mínimo, foto, activo.                                               | Categoría, seriales, líneas de documentos, kárdex.                |
| Serial              | Número, estado, fecha de entrada, vencimiento de garantía (3 meses desde la salida).                                                                                                     | Producto, compra de entrada, venta o instalación de salida.       |
| Proveedor           | Nombre, NIT, contacto, ciudad, moneda habitual.                                                                                                                                          | Compras.                                                          |
| Cliente             | Tipo, nombre, documento, WhatsApp, correo, dirección, ciudad.                                                                                                                            | Cotizaciones, ventas, instalaciones.                              |
| Compra              | Consecutivo, fecha, factura, moneda, tasas, total, adjunto, estado (activa/anulada), motivo de anulación.                                                                                | Proveedor, líneas de compra, usuario.                             |
| Cotización          | Consecutivo, tipo, fecha, validez, moneda, tasas, mano de obra, descripción, total, estado, motivo de rechazo.                                                                           | Cliente, líneas, venta o instalación generada.                    |
| Venta               | Consecutivo, fecha, moneda, tasas, descuento, total, costo, utilidad, estado (activa/anulada), motivo de anulación.                                                                      | Cliente, líneas, seriales, cotización de origen.                  |
| Instalación         | Consecutivo, fecha, dirección, descripción, moneda, tasas, material cobrado, mano de obra, total, costo, utilidad, garantías, condiciones, estado (activa/anulada), motivo de anulación. | Cliente, técnicos, líneas, seriales, fotos, cotización de origen. |
| Línea de documento  | Producto, cantidad, precio o costo unitario, costo en USD al momento de la salida, subtotal.                                                                                             | Compra, cotización, venta o instalación.                          |
| Foto                | Archivo, grupo (antes/durante/después), fecha.                                                                                                                                           | Instalación.                                                      |
| Ajuste              | Consecutivo, fecha, motivo, cantidad, observación.                                                                                                                                       | Producto, seriales, usuario.                                      |
| Inventario inicial  | Consecutivo, fecha, archivo cargado.                                                                                                                                                     | Productos, seriales, kárdex, usuario.                             |
| Movimiento (kárdex) | Fecha, tipo, documento, entrada, salida, saldo.                                                                                                                                          | Producto, usuario, documento de origen.                           |
| Historial de costo  | Fecha, moneda y tasa de la factura, costo anterior, costo nuevo (USD), regla aplicada (sube / promedio).                                                                                 | Producto, compra.                                                 |
| Tasa de cambio      | Fecha, par (USD/COP, USD/VES), valor, fuente, automática o manual, usuario, historial de correcciones (valor anterior, nuevo, usuario).                                                  | Documentos (se copia el valor).                                   |
| Reclamo de garantía | Fecha, descripción, solución.                                                                                                                                                            | Instalación o serial.                                             |
| Configuración       | Datos de la empresa, valores por defecto.                                                                                                                                                | PDF y formularios.                                                |

## 6. Requerimientos no funcionales

| **ID** | **Requerimiento**                                                                                                                                                                                                                              |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RNF-01 | Aplicación web que funcione en celular y en computador (diseño adaptable a cualquier pantalla), sin necesidad de instalar nada. Deseable que se pueda agregar a la pantalla de inicio del celular como una app.                                |
| RNF-02 | Acceso protegido con usuario y contraseña; las contraseñas se guardan cifradas y la conexión es segura (HTTPS).                                                                                                                                |
| RNF-03 | Interfaz en español, sencilla y rápida de usar desde el celular en plena instalación: botones grandes, pocos pasos y cálculos automáticos.                                                                                                     |
| RNF-04 | Formatos: costos internos en USD; pesos colombianos con separador de miles y sin decimales (\$ 1.250.000); dólares con dos decimales (US\$ 1.939,04); bolívares con dos decimales (Bs 1.234,56); fechas día/mes/año; hora de Colombia (UTC-5). |
| RNF-05 | Copias de seguridad automáticas diarias de la información y de las fotos, con posibilidad de restaurar.                                                                                                                                        |
| RNF-06 | Posibilidad de exportar la información a Excel (inventario, kárdex, compras, ventas, instalaciones, cotizaciones y reportes).                                                                                                                  |
| RNF-07 | El sistema funcionará siempre con conexión a internet. No se requiere modo sin conexión, ya que los usuarios siempre cuentan con internet en las obras.                                                                                        |
| RNF-08 | Las pantallas deben cargar en menos de 3 segundos con una conexión móvil normal.                                                                                                                                                               |
| RNF-09 | El diseño visual seguirá el estilo del prototipo de referencia (sección 1.5).                                                                                                                                                                  |
| RNF-10 | El código fuente estará en repositorios de GitHub de ITALARM; hosting, base de datos, almacenamiento y dominio en cuentas a nombre de la empresa. Se entregará un manual corto de uso.                                                         |

## 7. Fuera de alcance (por ahora)

- Facturación electrónica DIAN (se sigue haciendo por fuera del sistema).

- Ventas a crédito, cuentas por cobrar y abonos.

- Registro de pagos y medios de pago (efectivo, transferencia, Nequi, etc.).

- Funcionamiento sin conexión a internet.

- Múltiples bodegas o traslados entre ubicaciones.

- Kits o combos de productos (todo se vende por unidad).

- Conversión automática de bobinas a metros.

- Roles y permisos diferenciados entre usuarios.

- Contabilidad (el sistema es de control interno).

- Consulta automática de la tasa del bolívar (se ingresa manualmente).

- Edición de valores en documentos guardados (se corrigen mediante anulación).

- Gestión del equipo dañado en garantía (cambio, baja o devolución al proveedor): es un proceso manual de la empresa.

- Reserva o apartado de material desde cotizaciones.

## 8. Diferencias entre el prototipo y este documento

El prototipo se elaboró a partir de una versión anterior de este documento. Los siguientes puntos del prototipo NO deben implementarse tal como se ven; prevalece lo indicado en este documento:

| **Tema**              | **En el prototipo**                                                                       | **Lo que se debe construir**                                                                                           |
|-----------------------|-------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| Monedas               | Solo COP y USD.                                                                           | USD (principal), COP y VES.                                                                                            |
| Moneda de los precios | Precios instalador y cliente final en COP.                                                | Precios principalmente en USD, con opción de otra moneda.                                                              |
| Costo del inventario  | Solo promedio ponderado, llevado en COP ("Costo actualizado por promedio ponderado").     | Costo en USD. Si el precio sube, todo al precio mayor; si baja, promedio ponderado; sin stock, el de la nueva factura. |
| Tasa del bolívar      | No existe.                                                                                | Registro manual diario con doble confirmación y alerta de variación.                                                   |
| Kits                  | Botones "Kit 4 cámaras" en instalación y cotización.                                      | No hay kits. Todo se agrega por unidad.                                                                                |
| Cable                 | Muestra stock en rollos más metros ("1 rollo + 107 m").                                   | Solo metros, sin conversión.                                                                                           |
| Garantía mano de obra | Opciones de 3, 6 y 12 meses.                                                              | Máximo 3 meses.                                                                                                        |
| Garantía de equipos   | Opciones de 6, 12 y 24 meses; el PDF dice "equipos 12 meses y mano de obra 6 meses".      | 3 meses para todos los equipos, sin opción de elegir.                                                                  |
| Descuentos            | Solo 0 %, 5 % o 10 %.                                                                     | Cualquier porcentaje o valor.                                                                                          |
| Usuarios              | Andrés y Julián.                                                                          | Jose y Victor.                                                                                                         |
| Mano de obra          | Siempre en COP.                                                                           | En la moneda elegida para el documento.                                                                                |
| Cotizaciones          | Un solo formulario en borrador, sin listado.                                              | Listado con estados y seguimiento (sección 3.11).                                                                      |
| Seriales en compras   | No se piden al registrar la compra.                                                       | Se ingresan los seriales de cada unidad comprada.                                                                      |
| Stock insuficiente    | Permite guardar ("Puedes guardar igual; quedará registrado") y deja el stock en negativo. | No se permite guardar ninguna salida mayor al stock.                                                                   |
| Anulaciones           | No existen.                                                                               | Anulación con motivo según la sección 3.9.                                                                             |

## 9. Arquitectura y tecnología

Esta sección define la tecnología obligatoria del proyecto y cómo se organizan sus partes. El sistema se construye como dos aplicaciones separadas: un backend (API) y un frontend (pantallas), que se comunican por HTTPS mediante una API REST documentada.

### 9.1 Tecnología definida

| **Capa**                       | **Tecnología**                                                                      | **Notas**                                                                |
|--------------------------------|-------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| Backend                        | Java 21 (LTS) + Spring Boot 3.x                                                     | Maven como herramienta de construcción.                                  |
| Base de datos                  | PostgreSQL 16 o superior                                                            | Administrada por el proveedor de hosting, con copias diarias.            |
| Migraciones de BD              | Flyway                                                                              | Toda modificación del esquema se hace con un script versionado.          |
| Persistencia                   | Spring Data JPA (Hibernate)                                                         | Consultas de reportes pueden usar SQL nativo o JdbcTemplate.             |
| Seguridad                      | Spring Security                                                                     | Sesión con cookie segura (HttpOnly, Secure, SameSite) y protección CSRF. |
| Documentación de API           | springdoc-openapi (OpenAPI 3 / Swagger UI)                                          | Es el contrato entre backend y frontend.                                 |
| PDF                            | OpenPDF o JasperReports                                                             | Generados en el backend.                                                 |
| Excel                          | Apache POI                                                                          | Exportaciones y carga inicial.                                           |
| Archivos                       | Almacenamiento compatible con S3 (Cloudflare R2, AWS S3 o DigitalOcean Spaces)      | Fotos, facturas y PDF. Nunca dentro de la base de datos.                 |
| Frontend                       | React 18+ con TypeScript (modo estricto) + Vite                                     | Aplicación de una sola página (SPA) instalable como PWA.                 |
| Datos del servidor en el front | TanStack Query                                                                      | Caché, reintentos y estados de carga.                                    |
| Formularios y validación       | React Hook Form + Zod                                                               |                                                                          |
| Enrutamiento                   | React Router                                                                        |                                                                          |
| Estilos                        | Tailwind CSS con los colores y tipografías del prototipo                            | Componentes accesibles (por ejemplo shadcn/ui sobre Radix).              |
| Cliente de API                 | Tipos y cliente generados desde el OpenAPI del backend (orval u openapi-typescript) | El front nunca define a mano los tipos de la API.                        |
| Pruebas backend                | JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL real)                         |                                                                          |
| Pruebas frontend               | Vitest, React Testing Library, MSW, Playwright (extremo a extremo)                  |                                                                          |
| Control de versiones y CI/CD   | GitHub + GitHub Actions                                                             | Repositorios a nombre de ITALARM.                                        |

### 9.2 Componentes del sistema

| **Componente**                          | **Responsabilidad**                                                                                                                                                                                                                                                                    |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Frontend (app.italarm.com)              | Pantallas del prototipo, navegación, formularios, vista previa, captura de fotos y seriales con la cámara, instalación como PWA. No contiene reglas de negocio: solo muestra lo que calcula el backend (puede mostrar vistas previas, pero el valor oficial siempre lo da el backend). |
| Backend (api.italarm.com)               | API REST, reglas de negocio (costo, stock, seriales, anulaciones, garantías, estados de cotización), seguridad, auditoría, generación de PDF y Excel.                                                                                                                                  |
| Tareas programadas (dentro del backend) | Consulta diaria de la TRM, vencimiento automático de cotizaciones, recordatorio de tasa del bolívar sin registrar.                                                                                                                                                                     |
| PostgreSQL                              | Única fuente de verdad de los datos. Aplica restricciones (stock ≥ 0, seriales únicos, consecutivos).                                                                                                                                                                                  |
| Almacenamiento S3                       | Fotos de instalaciones, facturas de proveedores, PDF generados y logo de la empresa.                                                                                                                                                                                                   |
| Servicio de TRM                         | Fuente oficial de la TRM (por ejemplo, el conjunto de datos abiertos de la TRM en datos.gov.co publicado por la Superintendencia Financiera). El desarrollador debe verificar la fuente vigente.                                                                                       |

*Flujo: Celular/Computador → Frontend (HTTPS) → API del Backend → PostgreSQL y Almacenamiento S3. Las tareas programadas del backend consultan el servicio de TRM.*

### 9.3 Organización de los repositorios

Dos repositorios independientes en GitHub, propiedad de ITALARM:

- **italarm-api:** API en Spring Boot.

- **italarm-web:** aplicación web en React.

Cada repositorio tendrá: README con instrucciones para ejecutar localmente, archivo CLAUDE.md con comandos y convenciones para el agente de desarrollo, carpeta docs/ con una copia de este documento en Markdown, CHANGELOG.md y flujo de CI que compila y ejecuta todas las pruebas en cada cambio.

### 9.4 Contrato de la API

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                    |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RT-01  | La API se versiona en la ruta: /api/v1/...                                                                                                                                                                                                                                                           |
| RT-02  | Recursos REST en plural y en español, en minúsculas con guiones: /productos, /categorias, /proveedores, /clientes, /compras, /ventas, /instalaciones, /cotizaciones, /ajustes, /seriales, /tasas, /reportes, /configuracion, /usuarios.                                                              |
| RT-03  | Acciones de negocio que no son un CRUD se exponen como sub-recursos con POST: /compras/{id}/anular, /ventas/{id}/anular, /instalaciones/{id}/anular, /cotizaciones/{id}/enviar, /cotizaciones/{id}/aprobar, /cotizaciones/{id}/rechazar, /cotizaciones/{id}/convertir, /tasas/ves (registro manual). |
| RT-04  | Los listados son paginados (page, size, sort) y aceptan filtros por parámetros de consulta.                                                                                                                                                                                                          |
| RT-05  | Los errores se responden con el formato estándar Problem Details (RFC 9457) e incluyen un código de negocio estable (por ejemplo STOCK_INSUFICIENTE, SERIAL_NO_DISPONIBLE, COMPRA_NO_ANULABLE, TASA_NO_CONFIRMADA) y mensajes en español para mostrar al usuario.                                    |
| RT-06  | Los valores de dinero viajan como texto decimal (por ejemplo "19.5000") junto con su moneda, nunca como número de punto flotante.                                                                                                                                                                    |
| RT-07  | Las operaciones que crean documentos (compras, ventas, instalaciones, ajustes, conversiones) aceptan una cabecera Idempotency-Key para que un doble toque en el celular no cree dos documentos.                                                                                                      |
| RT-08  | El OpenAPI generado por el backend es la fuente única de los tipos del frontend.                                                                                                                                                                                                                     |

### 9.5 Despliegue e infraestructura

| **Elemento**  | **Recomendación**                                                                                                                                                                                            |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Frontend      | Archivos estáticos en Cloudflare Pages, Vercel o Netlify, con dominio app.italarm.com.                                                                                                                       |
| Backend       | Imagen Docker desplegada en Render, Railway, DigitalOcean App Platform o AWS, con al menos 1 GB de memoria. Dominio api.italarm.com.                                                                         |
| Base de datos | PostgreSQL administrado en el mismo proveedor y región que el backend (EE. UU. Este, por cercanía a Colombia), con copias de seguridad diarias y restauración a un punto en el tiempo si el plan lo permite. |
| Archivos      | Bucket S3 privado; las fotos y PDF se entregan con enlaces firmados de corta duración.                                                                                                                       |
| Ambientes     | Local (Docker Compose con PostgreSQL), Pruebas (staging) y Producción, cada uno con su propia base de datos y bucket.                                                                                        |
| CI/CD         | GitHub Actions: en cada Pull Request compila, ejecuta pruebas y análisis de estilo. Al integrar en la rama principal despliega a Pruebas; el paso a Producción es manual y aprobado por ITALARM.             |
| Secretos      | Variables de entorno del proveedor de hosting. Nunca en el repositorio.                                                                                                                                      |
| Monitoreo     | Endpoint de salud (Spring Actuator), registro de errores con Sentry (backend y frontend) y alerta si la tarea de TRM falla.                                                                                  |
| Propiedad     | Cuentas de GitHub, hosting, base de datos, almacenamiento, Sentry y dominio a nombre de ITALARM.                                                                                                             |

## 10. Buenas prácticas del backend (Java / Spring Boot)

Reglas obligatorias para todo el código del backend. El agente de desarrollo debe aplicarlas desde la Fase 0 y verificarlas en cada revisión.

### 10.1 Estructura del proyecto

Monolito modular organizado por módulo de negocio (no por tipo de clase). Cada módulo tiene sus capas internas y solo expone servicios y DTO a los demás módulos:

```text
co.italarm.api
├── shared/            (dinero, moneda, errores, auditoría, seguridad, utilidades)
├── usuarios/
├── catalogo/          (productos, categorías, unidades)
├── tasas/             (TRM, bolívar, historial)
├── terceros/          (clientes, proveedores)
├── inventario/        (stock, kárdex, costo, seriales, ajustes, carga inicial)
├── compras/
├── ventas/
├── instalaciones/     (incluye fotos y garantías)
├── cotizaciones/
├── documentos/        (PDF, Excel, almacenamiento S3)
├── reportes/
└── configuracion/

Dentro de cada módulo:
  api/          → controladores REST y DTO de entrada/salida (records)
  aplicacion/   → servicios de caso de uso (transacciones)
  dominio/      → entidades, reglas de negocio puras, enums, excepciones
  infraestructura/ → repositorios JPA, clientes externos
```


### 10.2 Reglas de código

| **ID** | **Requerimiento**                                                                                                                                                                                                                                                                                              |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BP-01  | Controladores delgados: reciben, validan y delegan. Toda la lógica está en servicios de aplicación y en el dominio.                                                                                                                                                                                            |
| BP-02  | Nunca exponer entidades JPA en la API: siempre DTO inmutables con Java records. Conversión con MapStruct o mapeadores explícitos.                                                                                                                                                                              |
| BP-03  | Inyección de dependencias por constructor (campos final). No usar @Autowired en campos.                                                                                                                                                                                                                        |
| BP-04  | Validación de entrada con Bean Validation (@NotNull, @Positive, @Size, etc.) en los DTO, y validaciones de negocio en el dominio.                                                                                                                                                                              |
| BP-05  | Las reglas críticas (cálculo de costo, validación de stock, estados de cotización, cálculo de garantías) se implementan como código de dominio puro, sin dependencias de Spring, para probarlas con pruebas unitarias rápidas.                                                                                 |
| BP-06  | Dinero: tipo BigDecimal siempre, con un objeto de valor Dinero (monto + moneda). Escalas: costos y tasas con 6 decimales en cálculo y 4 al guardar; totales en COP sin decimales al mostrar; USD y VES con 2. Modo de redondeo definido en un solo lugar (HALF_UP). Prohibido usar double o float para dinero. |
| BP-07  | En la base de datos, dinero y tasas con NUMERIC(19,4) o NUMERIC(19,6); nunca REAL o DOUBLE PRECISION.                                                                                                                                                                                                          |
| BP-08  | Transacciones con @Transactional solo en la capa de aplicación. Toda operación que mueve inventario (compra, venta, instalación, ajuste, anulación, conversión) bloquea las filas de los productos y seriales afectados (bloqueo pesimista, SELECT ... FOR UPDATE) en orden de id para evitar bloqueos mutuos. |
| BP-09  | Restricciones en la base de datos como segunda línea de defensa: CHECK (stock \>= 0), UNIQUE en número de serial por producto, UNIQUE en consecutivos, llaves foráneas en todas las relaciones.                                                                                                                |
| BP-10  | Kárdex solo de inserción: nunca se actualiza ni borra un movimiento. Una anulación genera movimientos contrarios. El stock de cada producto debe ser igual a la suma de su kárdex (prueba automática que lo verifique).                                                                                        |
| BP-11  | Consecutivos generados con secuencias de PostgreSQL por tipo de documento; el formato (V-0001) se aplica al mostrar.                                                                                                                                                                                           |
| BP-12  | Auditoría automática: created_at, created_by, updated_at, updated_by en todas las tablas (Spring Data Auditing). Datos maestros con control de versión optimista (@Version).                                                                                                                                   |
| BP-13  | Fechas y horas: instantes guardados como TIMESTAMPTZ en UTC; fechas de negocio (fecha de venta, vencimientos) como LocalDate en zona America/Bogota. La zona se configura en un solo lugar y se usa un Clock inyectable para poder probar vencimientos.                                                        |
| BP-14  | Integraciones externas (TRM, almacenamiento S3, generación de PDF) detrás de interfaces propias del módulo, con implementaciones reemplazables en pruebas.                                                                                                                                                     |
| BP-15  | La tarea de TRM debe ser idempotente (si corre dos veces el mismo día no duplica) y registrar su resultado.                                                                                                                                                                                                    |
| BP-16  | Excepciones de negocio propias (por ejemplo StockInsuficienteException) manejadas en un @RestControllerAdvice global que responde Problem Details con el código de negocio.                                                                                                                                    |
| BP-17  | Configuración con perfiles (local, test, staging, prod) y variables de entorno. Sin secretos en el código ni en application.yml.                                                                                                                                                                               |
| BP-18  | Hibernate con ddl-auto=validate: el esquema lo manda Flyway, nunca Hibernate. Los scripts de Flyway ya aplicados no se modifican; cada cambio es un script nuevo.                                                                                                                                              |
| BP-19  | Consultas: evitar el problema N+1 (usar fetch join o proyecciones); índices en llaves foráneas y columnas de filtro frecuentes (fecha, cliente, producto, estado).                                                                                                                                             |
| BP-20  | Seguridad: contraseñas con BCrypt, cookie de sesión HttpOnly + Secure + SameSite, protección CSRF activa, CORS limitado al dominio del frontend, límite de intentos de ingreso, cabeceras de seguridad, validación de tipo y tamaño de archivos subidos.                                                       |
| BP-21  | Logs con SLF4J, sin datos sensibles (contraseñas, tokens). Cada petición con un identificador de correlación.                                                                                                                                                                                                  |
| BP-22  | Estilo uniforme con Spotless (formato Google Java Format) y análisis estático (Checkstyle o SpotBugs) en la CI. Lombok no se usa en entidades; se prefieren records.                                                                                                                                           |
| BP-23  | Nombres de clases y métodos en inglés o en español, pero de forma consistente en todo el proyecto (se recomienda el dominio en español para que coincida con este documento: Producto, Compra, Cotizacion).                                                                                                    |

### 10.3 Pruebas del backend

| **ID** | **Requerimiento**                                                                                                                                                                                                            |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BP-24  | Pruebas unitarias del dominio para toda regla de negocio: motor de costo, conversión de monedas, validación de stock, estados de cotización, cálculo de garantías y de utilidad.                                             |
| BP-25  | Pruebas de integración con Testcontainers (PostgreSQL real) para cada caso de uso que mueve inventario, incluyendo concurrencia: dos ventas simultáneas de la última unidad deben terminar con una exitosa y otra rechazada. |
| BP-26  | Cada caso de prueba de aceptación de la sección 14 (CP-xx) tiene al menos una prueba automática con el identificador en su nombre, por ejemplo cp01_compraConPrecioMayor_subeElCosto().                                      |
| BP-27  | Pruebas de la API (MockMvc) para seguridad, validaciones y formato de errores.                                                                                                                                               |
| BP-28  | Cobertura mínima de 80 % en los paquetes de dominio y aplicación, medida con JaCoCo en la CI.                                                                                                                                |
| BP-29  | Ninguna fase se da por terminada con pruebas fallando o desactivadas.                                                                                                                                                        |

## 11. Buenas prácticas del frontend (React / TypeScript)

### 11.1 Estructura del proyecto

Organización por funcionalidad (feature), igual que los módulos del backend:

```text
src/
├── app/              (arranque, rutas, proveedores, layout con menú lateral y barra inferior)
├── api/              (cliente y tipos generados desde OpenAPI; no se editan a mano)
├── components/ui/    (componentes base del sistema de diseño: botón, campo, tabla, diálogo...)
├── features/
│   ├── auth/  inicio/  inventario/  compras/  ventas/
│   ├── instalaciones/  cotizaciones/  clientes/  reportes/  configuracion/
│   └── (cada una con components/, hooks/, pages/, schemas/ y sus pruebas)
├── lib/              (formato de dinero y fechas, utilidades)
└── styles/           (tokens de diseño del prototipo)
```


### 11.2 Reglas de código

| **ID** | **Requerimiento**                                                                                                                                                                                      |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BF-01  | TypeScript en modo estricto; prohibido any (salvo casos justificados y comentados).                                                                                                                    |
| BF-02  | Solo componentes de función y hooks. Componentes pequeños con una sola responsabilidad; la lógica reutilizable va en hooks personalizados.                                                             |
| BF-03  | El estado que viene del servidor se maneja solo con TanStack Query (no se copia a estados locales ni a un store global). Para estado de interfaz global mínimo, React Context o Zustand.               |
| BF-04  | Tipos y llamadas a la API generados desde el OpenAPI. Si el backend cambia, se regeneran; nunca se escriben a mano.                                                                                    |
| BF-05  | Formularios con React Hook Form y esquemas Zod. Los mensajes de validación en español y junto a cada campo. Los errores del backend (Problem Details) se muestran traducidos por su código de negocio. |
| BF-06  | El frontend no es la fuente de verdad de costos, totales ni utilidades: puede calcular vistas previas, pero muestra y guarda lo que responde el backend.                                               |
| BF-07  | Formato de dinero y fechas centralizado en lib/ con Intl.NumberFormat y la configuración es-CO: \$ 1.250.000 · US\$ 1.939,04 · Bs 1.234,56 · dd/mm/aaaa. Nunca formatear a mano en los componentes.    |
| BF-08  | Diseño primero para celular (mobile first) y adaptable a computador, siguiendo el prototipo: menú lateral en computador, barra inferior con botón Nuevo (+) en celular.                                |
| BF-09  | Cada pantalla contempla sus estados: cargando (esqueletos), vacío, error con opción de reintentar y éxito. Notificaciones breves (toasts) para confirmaciones.                                         |
| BF-10  | Botones de guardar deshabilitados mientras la petición está en curso, y envío de Idempotency-Key en creaciones, para evitar documentos duplicados.                                                     |
| BF-11  | Accesibilidad: etiquetas en todos los campos, navegación con teclado, contraste suficiente, áreas táctiles de al menos 44 px.                                                                          |
| BF-12  | Rutas cargadas de forma diferida (lazy) para que la primera carga sea rápida en celular.                                                                                                               |
| BF-13  | PWA con vite-plugin-pwa: instalable, con ícono y pantalla de inicio. No se requiere funcionamiento sin conexión; si no hay internet, mostrar un aviso claro.                                           |
| BF-14  | Cámara: fotos con input de archivo con captura (capture="environment"), compresión en el navegador antes de subir; lectura de códigos de barras de seriales con una librería como @zxing/browser.      |
| BF-15  | Variables de entorno con prefijo VITE\_ (por ejemplo VITE_API_URL). Sin secretos en el frontend.                                                                                                       |
| BF-16  | Estilo uniforme con ESLint (reglas de React y hooks) y Prettier, verificados en la CI y antes de cada commit.                                                                                          |
| BF-17  | Textos de la interfaz en español, centralizados por funcionalidad para facilitar cambios.                                                                                                              |

### 11.3 Pruebas del frontend

| **ID** | **Requerimiento**                                                                                                                                                                                                                            |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BF-18  | Pruebas de componentes y hooks con Vitest y React Testing Library, probando lo que ve y hace el usuario (no detalles internos).                                                                                                              |
| BF-19  | MSW para simular la API en pruebas, usando las respuestas del contrato OpenAPI.                                                                                                                                                              |
| BF-20  | Pruebas extremo a extremo con Playwright contra el ambiente de pruebas para los flujos principales: ingreso, registrar compra, venta, instalación con fotos, cotización y conversión, anulación. Se ejecutan en tamaño celular y computador. |
| BF-21  | Ninguna fase se da por terminada con pruebas fallando.                                                                                                                                                                                       |

## 12. Plan de desarrollo por fases

El proyecto se desarrolla en 7 fases, en este orden. Cada fase entrega backend y frontend funcionando juntos en el ambiente de pruebas, y solo se pasa a la siguiente cuando se cumplen sus criterios de aceptación y ITALARM la aprueba. Dentro de cada fase, primero se construye y prueba el backend (endpoints documentados en OpenAPI) y luego el frontend que los consume.

### 12.1 Definición de terminado (aplica a toda fase)

- Código revisado mediante Pull Request, con la CI en verde (compilación, estilo, pruebas).

- Pruebas automáticas de la fase escritas y pasando, incluidos los casos CP-xx asignados.

- Migraciones Flyway aplicadas en el ambiente de pruebas sin errores.

- OpenAPI actualizado y cliente del frontend regenerado.

- Desplegado en el ambiente de pruebas y probado en celular y computador.

- CHANGELOG.md actualizado y demostración a ITALARM, que aprueba la fase.

### 12.2 Fase 0 — Fundaciones

Objetivo: Dejar lista la base técnica sobre la que se construye todo, con un ingreso al sistema funcionando.

<table>
<colgroup>
<col style="width: 23%" />
<col style="width: 76%" />
</colgroup>
<tbody>
<tr class="odd">
<td><strong>Backend</strong></td>
<td><ul>
<li><p>Crear proyecto Spring Boot 3 / Java 21 con Maven y la estructura de módulos de la sección 10.1.</p></li>
<li><p>Docker Compose local con PostgreSQL; Flyway configurado con el primer script (usuarios, configuración).</p></li>
<li><p>Spring Security: ingreso, cierre de sesión, cambio de contraseña, CSRF, CORS. Usuarios iniciales Jose y Victor.</p></li>
<li><p>Manejo global de errores con Problem Details, auditoría automática, identificador de correlación en logs.</p></li>
<li><p>Objeto de valor Dinero y utilidades de moneda y fecha (zona America/Bogota, Clock inyectable).</p></li>
<li><p>springdoc-openapi, Actuator (salud), Spotless, Checkstyle, JaCoCo.</p></li>
<li><p>Dockerfile, CI en GitHub Actions y despliegue al ambiente de pruebas con su PostgreSQL.</p></li>
<li><p>Archivo CLAUDE.md y README.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><strong>Frontend</strong></td>
<td><ul>
<li><p>Crear proyecto React + TypeScript + Vite, ESLint, Prettier, Tailwind con los tokens del prototipo.</p></li>
<li><p>Generación del cliente de API desde el OpenAPI.</p></li>
<li><p>Layout principal: menú lateral (computador) y barra inferior con botón Nuevo (celular), según el prototipo.</p></li>
<li><p>Pantalla de ingreso, cierre de sesión, protección de rutas y cambio de contraseña.</p></li>
<li><p>Componentes base del sistema de diseño y utilidades de formato de dinero y fecha.</p></li>
<li><p>Configuración PWA, CI y despliegue al ambiente de pruebas.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><strong>Pruebas y casos de aceptación</strong></td>
<td>Prueba de ingreso correcto e incorrecto; prueba de formato de dinero (COP, USD, VES); CI en verde en ambos repositorios.</td>
</tr>
<tr class="even">
<td><strong>Entregable</strong></td>
<td>Jose y Victor pueden ingresar al sistema en el ambiente de pruebas desde celular y computador y ver el menú vacío.</td>
</tr>
</tbody>
</table>

### 12.3 Fase 1 — Catálogo, terceros, tasas y configuración

Objetivo: Registrar la información maestra y tener las tasas de cambio funcionando.

<table>
<colgroup>
<col style="width: 23%" />
<col style="width: 76%" />
</colgroup>
<tbody>
<tr class="odd">
<td><strong>Backend</strong></td>
<td><ul>
<li><p>CRUD de categorías y productos (con indicador de serial, unidad, precios en USD, stock mínimo, foto, activo).</p></li>
<li><p>CRUD de clientes (tipo instalador / cliente final) y proveedores (moneda habitual).</p></li>
<li><p>Módulo de tasas: tarea diaria de TRM (idempotente, con reintentos y alerta si falla), registro manual de tasa USD/VES con doble confirmación y alerta de variación, corrección con historial, consulta de tasa vigente e historial.</p></li>
<li><p>Configuración: datos de la empresa (ITALARM, logo en S3), valores por defecto (validez, garantías, condiciones, límite de variación).</p></li>
<li><p>Integración con almacenamiento S3 (subida y enlaces firmados).</p></li>
</ul></td>
</tr>
<tr class="even">
<td><strong>Frontend</strong></td>
<td><ul>
<li><p>Pantallas de productos (listado, nuevo, editar), clientes (listado con filtros, detalle, nuevo) y proveedores.</p></li>
<li><p>Recuadro de tasas en el menú y barra superior; diálogo de registro de tasa del bolívar con doble digitación y alerta de variación; aviso cuando falte la tasa del día.</p></li>
<li><p>Pantalla de configuración.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><strong>Pruebas y casos de aceptación</strong></td>
<td>CP-10, CP-11, CP-12. Pruebas de la tarea de TRM con el servicio simulado (éxito, falla, doble ejecución).</td>
</tr>
<tr class="even">
<td><strong>Entregable</strong></td>
<td>El catálogo, los clientes y proveedores están cargados y las tasas del día se ven en todo el sistema.</td>
</tr>
</tbody>
</table>

### 12.4 Fase 2 — Compras, inventario, costo y carga inicial

Objetivo: El corazón del sistema: que el stock, los seriales y el costo en USD cuadren siempre.

<table>
<colgroup>
<col style="width: 23%" />
<col style="width: 76%" />
</colgroup>
<tbody>
<tr class="odd">
<td><strong>Backend</strong></td>
<td><ul>
<li><p>Motor de costo en el dominio: conversión a USD, regla sube / promedio, caso sin stock, historial de costo.</p></li>
<li><p>Compras con seriales, adjunto de factura, consecutivo C-0001, bloqueo de filas y kárdex.</p></li>
<li><p>Seriales: estados, búsqueda e historial.</p></li>
<li><p>Ajustes de inventario (entrada al costo vigente, salida sin dejar negativo, seriales), consecutivo AJ-001.</p></li>
<li><p>Anulación de compras con sus condiciones (última compra de cada producto, unidades en bodega) y reversión del costo.</p></li>
<li><p>Carga inicial de datos desde plantilla Excel (sección 3.18), con validación completa antes de guardar.</p></li>
<li><p>Consultas: inventario valorizado, detalle de producto, kárdex, historial de costo.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><strong>Frontend</strong></td>
<td><ul>
<li><p>Pantallas de inventario (listado con filtros y buscador), detalle de producto (indicadores, seriales, kárdex, historial de costo) y ajuste.</p></li>
<li><p>Registro de compra con vista previa del cambio de costo, captura de seriales (digitados o con cámara) y foto de factura.</p></li>
<li><p>Listado y detalle de compras; anulación con motivo.</p></li>
<li><p>Pantalla de carga inicial: descargar plantilla, subir archivo, ver errores por fila, confirmar.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><strong>Pruebas y casos de aceptación</strong></td>
<td>CP-01 a CP-07, CP-13, CP-16, CP-17, CP-19, CP-28, CP-29. Prueba de concurrencia y prueba de que el stock es igual a la suma del kárdex.</td>
</tr>
<tr class="even">
<td><strong>Entregable</strong></td>
<td>ITALARM carga su inventario real y registra compras; el costo en USD se calcula correctamente.</td>
</tr>
</tbody>
</table>

### 12.5 Fase 3 — Ventas, comprobantes y anulaciones

Objetivo: Vender material con precio según el tipo de cliente, controlando stock y seriales.

<table>
<colgroup>
<col style="width: 23%" />
<col style="width: 76%" />
</colgroup>
<tbody>
<tr class="odd">
<td><strong>Backend</strong></td>
<td><ul>
<li><p>Ventas con precio por tipo de cliente, descuento libre, costo y utilidad guardados, bloqueo de stock, seriales, consecutivo V-0001, garantía de equipos de 3 meses por serial.</p></li>
<li><p>Costo mostrado con tasa de hoy y de la última compra.</p></li>
<li><p>Anulación de ventas (devuelve material y seriales al costo vigente).</p></li>
<li><p>Generación del PDF del comprobante de venta y enlace firmado para compartir.</p></li>
<li><p>Idempotency-Key en la creación.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><strong>Frontend</strong></td>
<td><ul>
<li><p>Formulario de nueva venta (cliente, moneda, productos, seriales, resumen, descuento), con bloqueo de guardar si no hay stock.</p></li>
<li><p>Pantalla de confirmación con Enviar por WhatsApp (compartir nativo en celular, enlace wa.me en computador) y Descargar PDF.</p></li>
<li><p>Listado y detalle de ventas; anulación.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><strong>Pruebas y casos de aceptación</strong></td>
<td>CP-08, CP-14, CP-18, CP-25, CP-27 y prueba de venta sin stock rechazada.</td>
</tr>
<tr class="even">
<td><strong>Entregable</strong></td>
<td>ITALARM registra ventas reales y envía comprobantes por WhatsApp.</td>
</tr>
</tbody>
</table>

### 12.6 Fase 4 — Instalaciones, fotos y garantías

Objetivo: Registrar el trabajo completo de cada instalación.

<table>
<colgroup>
<col style="width: 23%" />
<col style="width: 76%" />
</colgroup>
<tbody>
<tr class="odd">
<td><strong>Backend</strong></td>
<td><ul>
<li><p>Instalaciones: cliente, dirección, técnicos, descripción, material con seriales, mano de obra, cobro, costo y utilidad, consecutivo I-0001.</p></li>
<li><p>Fotos por grupo (antes, durante, después) en S3, con compresión y validación de tipo y tamaño.</p></li>
<li><p>Garantías de 3 meses (mano de obra y equipos), estados vigente / por vencer / vencida, reclamos como notas.</p></li>
<li><p>Anulación de instalaciones; edición solo de datos descriptivos.</p></li>
<li><p>PDF del comprobante de instalación con garantías y seriales.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><strong>Frontend</strong></td>
<td><ul>
<li><p>Formulario de instalación en 4 pasos con panel de cobro, según el prototipo.</p></li>
<li><p>Captura de fotos con la cámara del celular y galería por grupo.</p></li>
<li><p>Listado y detalle de instalaciones; consulta de garantías y búsqueda por serial o cliente; registro de reclamos.</p></li>
<li><p>Pantalla de confirmación con WhatsApp y PDF.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><strong>Pruebas y casos de aceptación</strong></td>
<td>CP-15, CP-20 y prueba de subida de fotos (tipos permitidos, tamaño máximo).</td>
</tr>
<tr class="even">
<td><strong>Entregable</strong></td>
<td>ITALARM registra instalaciones reales con fotos y consulta garantías.</td>
</tr>
</tbody>
</table>

### 12.7 Fase 5 — Cotizaciones

Objetivo: Cotizar, hacer seguimiento y convertir en venta o instalación.

<table>
<colgroup>
<col style="width: 23%" />
<col style="width: 76%" />
</colgroup>
<tbody>
<tr class="odd">
<td><strong>Backend</strong></td>
<td><ul>
<li><p>Cotizaciones de instalación y de venta, con validez, descuento, mano de obra, consecutivo COT-0001.</p></li>
<li><p>Máquina de estados (Borrador, En evaluación, Aprobada, Convertida, Rechazada, Vencida) en el dominio, con transiciones válidas.</p></li>
<li><p>Tarea diaria que vence cotizaciones.</p></li>
<li><p>Conversión a venta o instalación reutilizando los casos de uso de las fases 3 y 4; enlace entre documentos; regreso a Aprobada si se anula el documento generado.</p></li>
<li><p>PDF de cotización.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><strong>Frontend</strong></td>
<td><ul>
<li><p>Formulario de cotización con vista previa del PDF en vivo (computador).</p></li>
<li><p>Listado con filtros por estado, días para vencer y resaltado de próximas a vencer.</p></li>
<li><p>Acciones: enviar por WhatsApp, aprobar, rechazar con motivo, duplicar, convertir (abre el formulario precargado).</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><strong>Pruebas y casos de aceptación</strong></td>
<td>CP-09, CP-21, CP-22, CP-23, CP-24, CP-26 y pruebas de transiciones de estado no permitidas.</td>
</tr>
<tr class="even">
<td><strong>Entregable</strong></td>
<td>ITALARM gestiona todas sus cotizaciones en el sistema.</td>
</tr>
</tbody>
</table>

### 12.8 Fase 6 — Inicio, reportes, Excel y salida a producción

Objetivo: Completar la visión del negocio y poner el sistema en producción.

<table>
<colgroup>
<col style="width: 23%" />
<col style="width: 76%" />
</colgroup>
<tbody>
<tr class="odd">
<td><strong>Backend</strong></td>
<td><ul>
<li><p>Endpoints de la pantalla de inicio (indicadores del mes, stock bajo, garantías y cotizaciones por vencer, últimos movimientos).</p></li>
<li><p>Reportes por período y exportación a Excel de inventario, kárdex, compras, ventas, instalaciones, cotizaciones y reportes.</p></li>
<li><p>Revisión de rendimiento (índices, consultas), seguridad y copias de seguridad; prueba de restauración.</p></li>
<li><p>Ambiente de producción, dominio, monitoreo y alertas.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><strong>Frontend</strong></td>
<td><ul>
<li><p>Pantalla de inicio según el prototipo (computador y celular).</p></li>
<li><p>Pantalla de reportes con selector de período, gráficos y botón Excel.</p></li>
<li><p>Revisión final de accesibilidad, estados vacíos y de error, y pruebas en celulares reales.</p></li>
<li><p>Pruebas extremo a extremo completas.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><strong>Pruebas y casos de aceptación</strong></td>
<td>Todos los casos CP-01 a CP-29 pasando en el ambiente de pruebas; pruebas extremo a extremo en celular y computador.</td>
</tr>
<tr class="even">
<td><strong>Entregable</strong></td>
<td>Sistema en producción con los datos reales de ITALARM, manual corto de uso y restauración de copia de seguridad probada.</td>
</tr>
</tbody>
</table>

## 13. Guía de trabajo para el agente de desarrollo (Claude Code)

El desarrollo lo realizará un agente de Claude Code bajo la supervisión de ITALARM. Estas instrucciones son parte obligatoria del alcance:

| **ID** | **Requerimiento**                                                                                                                                                                                                            |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AG-01  | Antes de escribir código, leer este documento completo. En caso de conflicto, prevalece este documento sobre el prototipo (sección 8) y las secciones 10 y 11 sobre las preferencias propias.                                |
| AG-02  | Trabajar una fase a la vez, en el orden de la sección 12. Al iniciar cada fase, escribir en docs/plan-fase-N.md la lista de tareas, los endpoints y las migraciones que se harán, y esperar aprobación antes de implementar. |
| AG-03  | Dentro de una fase: primero el backend (migración, dominio con pruebas, casos de uso, endpoints, OpenAPI), luego el frontend (regenerar cliente, pantallas, pruebas).                                                        |
| AG-04  | Desarrollo guiado por pruebas para las reglas de negocio: escribir primero la prueba del caso CP-xx, verla fallar y luego implementar.                                                                                       |
| AG-05  | No inventar reglas de negocio. Si algo no está definido o es ambiguo, registrarlo en docs/preguntas.md y preguntar a ITALARM antes de decidir.                                                                               |
| AG-06  | Cambios pequeños y frecuentes: una rama por funcionalidad, commits con Conventional Commits (feat:, fix:, test:, refactor:, docs:), Pull Request con descripción de qué se hizo y cómo probarlo.                             |
| AG-07  | Mantener CLAUDE.md actualizado en cada repositorio con: comandos para compilar, probar y ejecutar; convenciones; y decisiones técnicas tomadas.                                                                              |
| AG-08  | No desactivar, omitir ni borrar pruebas para que la CI pase. No bajar la cobertura mínima.                                                                                                                                   |
| AG-09  | Nunca subir secretos, contraseñas ni datos reales de clientes al repositorio.                                                                                                                                                |
| AG-10  | No modificar migraciones Flyway ya aplicadas; crear siempre una nueva.                                                                                                                                                       |
| AG-11  | Al terminar cada fase: verificar la definición de terminado (12.1), actualizar CHANGELOG.md y preparar una lista de qué debe probar ITALARM en el ambiente de pruebas.                                                       |
| AG-12  | Ningún despliegue a producción sin aprobación explícita de ITALARM.                                                                                                                                                          |

Rol de ITALARM durante el desarrollo: aprobar el plan de cada fase, responder las preguntas del agente, probar cada entrega en el ambiente de pruebas con los casos de la sección 14 y aprobar el paso a la siguiente fase.

## 14. Casos de prueba de aceptación

Estos casos sirven para verificar que el sistema entregado cumple las reglas de negocio. Cada caso parte de la situación descrita y debe producir exactamente el resultado esperado. La columna Req. indica el requerimiento o sección que se prueba.

| **ID** | **Req.** | **Situación**                                                                                  | **Resultado esperado**                                                                                                             |
|--------|----------|------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| CP-01  | RF-66    | Hay 10 cámaras a US\$20. Se compran 10 a US\$25.                                               | Stock 20; costo US\$25.                                                                                                            |
| CP-02  | RF-66    | Hay 10 cámaras a US\$20. Se compran 10 a US\$15.                                               | Stock 20; costo US\$17,50.                                                                                                         |
| CP-03  | RF-66    | Hay 10 cámaras a US\$20. Se compran 10 a US\$20.                                               | Stock 20; costo US\$20 (se aplica promedio).                                                                                       |
| CP-04  | 3.8      | Hay 10 cámaras a US\$20. Se compran 10 en COP a \$76.000 con TRM \$4.000.                      | Stock 20; costo US\$19,50. La factura guarda COP y TRM \$4.000.                                                                    |
| CP-05  | 3.8      | Hay 10 cámaras a US\$20. Se compran 10 en COP a \$88.000 con TRM \$4.000.                      | Stock 20; costo US\$22.                                                                                                            |
| CP-06  | 3.8      | Hay 10 cámaras a US\$20. Se compran 10 en VES a Bs 950 con tasa registrada de Bs 50 por dólar. | Factura = US\$19; costo US\$19,50.                                                                                                 |
| CP-07  | 3.8      | Un producto con stock 0 y costo anterior US\$20. Se compran 5 a US\$18.                        | Stock 5; costo US\$18.                                                                                                             |
| CP-08  | RF-68    | Con costo US\$17,50 se vende 1 cámara. Luego se compra a US\$25 y el costo sube.               | La utilidad de la venta anterior sigue calculada con US\$17,50.                                                                    |
| CP-09  | RF-69    | Costo US\$19,50; TRM de hoy \$4.200; TRM de la última compra \$4.000. Se abre una cotización.  | Se muestra \$81.900 a la tasa de hoy y \$78.000 a la tasa de compra.                                                               |
| CP-10  | RF-35    | Se digita la tasa del bolívar dos veces con valores distintos.                                 | No se guarda; se pide digitarla de nuevo.                                                                                          |
| CP-11  | RF-35    | La tasa anterior es 50 y se digita 500 dos veces.                                              | Alerta de variación mayor al 5 %; solo se guarda si el usuario la acepta expresamente.                                             |
| CP-12  | RF-33    | No hay tasa del bolívar registrada hoy y se abre una venta.                                    | Se usa la última tasa registrada y se muestra el aviso para actualizarla.                                                          |
| CP-13  | RF-20    | Se compran 3 cámaras y se ingresan solo 2 seriales.                                            | No se permite guardar la compra.                                                                                                   |
| CP-14  | RF-22    | Se vende una cámara cuyo serial ya fue usado en una instalación.                               | Ese serial no aparece como disponible.                                                                                             |
| CP-15  | RF-65    | Hay 50 m de cable y se intenta registrar una instalación con 60 m.                             | No se permite guardar la instalación.                                                                                              |
| CP-16  | RF-71    | Se anula la última compra de un producto, sin salidas posteriores.                             | Stock y costo vuelven a los valores anteriores a esa compra.                                                                       |
| CP-17  | RF-71    | Se intenta anular una compra de la que ya se vendió una unidad.                                | No se permite; el sistema indica que se corrija con un ajuste.                                                                     |
| CP-18  | RF-72    | Se anula una venta de 2 cámaras con serial.                                                    | Las 2 unidades regresan al stock y sus seriales quedan disponibles.                                                                |
| CP-19  | RF-61    | Un conteo físico encuentra 2 conectores de más. Costo vigente US\$0,50.                        | Stock +2 al costo de US\$0,50; el costo no cambia.                                                                                 |
| CP-20  | RF-113   | Instalación del 1 de octubre con garantía por defecto.                                         | Garantía vigente hasta el 1 de enero siguiente; aparece en próximas a vencer durante sus últimos 30 días.                          |
| CP-21  | 3.11     | Cotización enviada con validez de 15 días; pasan 16 días sin respuesta.                        | La cotización pasa automáticamente a Vencida.                                                                                      |
| CP-22  | 3.11     | Se convierte en instalación una cotización aprobada de 4 cámaras y 120 m de cable.             | La instalación se abre con cliente, ítems, precios y mano de obra cargados; al guardar, la cotización queda Convertida y enlazada. |
| CP-23  | 3.11     | Se intenta convertir una cotización de 4 cámaras cuando solo hay 3 en bodega.                  | No se permite guardar la instalación hasta ajustar la cantidad.                                                                    |
| CP-24  | 3.9      | Se anula una venta que provenía de una cotización.                                             | La cotización vuelve al estado Aprobada.                                                                                           |
| CP-25  | RF-23    | Se vende una cámara con serial el 15 de octubre.                                               | El serial queda con garantía hasta el 15 de enero siguiente.                                                                       |
| CP-26  | 3.11     | Hay 5 cámaras. Se crea una cotización en evaluación por 4 cámaras.                             | El stock sigue en 5 y las 5 cámaras se pueden vender a otro cliente.                                                               |
| CP-27  | 3.12     | Venta de US\$100 con descuento de US\$7.                                                       | Total US\$93; la utilidad se calcula sobre US\$93.                                                                                 |
| CP-28  | RF-151   | Carga inicial de 10 cámaras con sus 10 seriales a US\$20.                                      | Stock 10; costo US\$20; kárdex con movimiento Inventario inicial II-001; seriales En bodega.                                       |
| CP-29  | RF-150   | Archivo de carga inicial con un serial repetido en la fila 8.                                  | No se guarda nada; se muestra el error en la hoja de inventario, fila 8.                                                           |

## 15. Puntos pendientes por definir

Estado de los puntos de decisión a la fecha de esta versión. Queda abierto un solo punto, resaltado en amarillo, que no bloquea el desarrollo.

| **\#** | **Tema**                | **Pregunta**                                                                           | **Estado**                                                                                              |
|--------|-------------------------|----------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| 1      | Tasa del bolívar        | ¿De dónde se toma la tasa USD/VES?                                                     | Cerrado: ingreso manual diario con doble confirmación (sección 3.5).                                    |
| 2      | Moneda de los costos    | ¿Se comparan los precios en USD aunque la factura venga en COP?                        | Cerrado: el costo se lleva en USD; cada factura se convierte con la tasa de su día (sección 3.8).       |
| 3      | Productos con serial    | ¿Qué productos llevan serial?                                                          | Cerrado: todos los equipos con serial de fábrica; cable y consumibles solo por cantidad (sección 3.4).  |
| 4      | Stock insuficiente      | ¿Se permite guardar una salida sin stock suficiente?                                   | Cerrado: no se permite; el stock nunca queda en negativo (sección 3.7).                                 |
| 5      | Garantía de equipos     | ¿Se controla la garantía de los equipos? ¿Con qué plazos?                              | Cerrado: 3 meses para todos los equipos, desde la venta o instalación (sección 3.4).                    |
| 6      | Datos de la empresa     | Logo, NIT, ciudad, teléfono y correo que aparecerán en los PDF.                        | Pendiente. El nombre ya está confirmado: ITALARM. No bloquea el desarrollo: se cargan en Configuración. |
| 7      | Usuarios                | Nombres de los dos usuarios del sistema.                                               | Cerrado: Jose y Victor.                                                                                 |
| 8      | Reserva en cotizaciones | ¿Una cotización en evaluación debe apartar el material para ese cliente?               | Cerrado: no aparta material; solo se descuenta al convertirla (sección 3.11).                           |
| 9      | Descuentos              | ¿Los descuentos son solo 0 %, 5 % y 10 %, o se permite cualquier valor?                | Cerrado: cualquier valor, a decisión del usuario.                                                       |
| 10     | Equipos defectuosos     | En un reclamo de garantía, ¿el equipo dañado se da de baja o se devuelve al proveedor? | Cerrado: proceso manual de la empresa, fuera del sistema (sección 3.14).                                |

## 16. Historial de versiones

| **Versión** | **Fecha**  | **Cambios**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|-------------|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0.1         | 29/09/2026 | Primer borrador: negocio, usuarios, módulos principales, monedas y requerimientos generales.                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 0.2         | 29/09/2026 | Se define desarrollo a la medida. No se registran pagos ni medios de pago. Uso siempre con internet.                                                                                                                                                                                                                                                                                                                                                                                                                |
| 0.3         | 29/09/2026 | Control por cantidad y serial. Cable en metros. Tres monedas (USD principal, COP, VES). Regla de costo de inventario. Sin kits. Garantía de instalación de 3 meses.                                                                                                                                                                                                                                                                                                                                                 |
| 0.4 (a)     | 29/09/2026 | Decisiones sobre la v0.3: tasa del bolívar manual con doble confirmación; costo en USD con conversión por factura y costo a la tasa del día al cotizar o vender; serial en todos los equipos con serial de fábrica; se impiden salidas mayores al stock; ajustes de entrada al costo vigente; anulación de documentos; casos de prueba de aceptación.                                                                                                                                                               |
| 0.4 (b)     | 30/09/2026 | Funcionalidades del prototipo: navegación, pantalla de inicio, detalle de producto, kárdex, ajustes, formularios paso a paso, pantallas de confirmación y PDF; ciclo de vida de cotizaciones y conversión a venta o instalación; glosario, garantías, configuración, reglas de negocio, modelo de datos, diferencias con el prototipo y anexo de capturas.                                                                                                                                                          |
| 0.5         | 30/09/2026 | Unificación de las dos versiones 0.4 en un solo documento. Se integran las decisiones de la 0.4 (a) en todos los módulos de la 0.4 (b): tasas, costo en USD, bloqueo de stock, anulaciones (nueva sección 3.9) y casos de prueba (sección 9, ampliados con cotizaciones). Pendientes con estado: 4 cerrados y 6 abiertos.                                                                                                                                                                                           |
| 0.6         | 30/09/2026 | Garantía de equipos de 3 meses para todos. Nombre de la empresa: ITALARM. Usuarios: Jose y Victor. Las cotizaciones no apartan material. Descuento libre. Equipos dañados en garantía: proceso manual (nuevo motivo de ajuste Garantía). Nuevos casos de prueba CP-25 a CP-27. Queda pendiente solo los datos de contacto de la empresa.                                                                                                                                                                            |
| 0.7         | 30/09/2026 | Arquitectura y tecnología: backend Java 21 + Spring Boot 3, frontend React + TypeScript separados, PostgreSQL, almacenamiento S3 y despliegue (sección 9). Buenas prácticas obligatorias de backend y frontend (secciones 10 y 11). Plan de desarrollo en 7 fases con tareas de backend y frontend, casos de aceptación y definición de terminado (sección 12). Guía de trabajo para el agente de Claude Code (sección 13). Nuevo requerimiento de carga inicial de datos desde Excel (3.18) y casos CP-28 y CP-29. |

## Anexo A. Capturas del prototipo

*Las capturas son de referencia visual. Los datos son de ejemplo. Donde haya diferencias con este documento, aplica la sección 8.*

*Figura 1. Pantalla de inicio (computador)*

*Figura 2. Pantalla de inicio (celular) con barra de navegación inferior*

*Figura 3. Listado de inventario*

*Figura 4. Detalle de producto*

*Figura 5. Detalle de producto: números de serie y kárdex*

*Figura 6. Registro de compra*

*Figura 7. Nueva venta*

*Figura 8. Nueva instalación: cliente, trabajo y cobro*

*Figura 9. Nueva instalación: material, fotos y garantía*

*Figura 10. Cotización con vista previa del PDF*

*Figura 11. Clientes*

*Figura 12. Reportes*
