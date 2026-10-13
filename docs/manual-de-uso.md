# Manual de uso de ITALARM

Guía corta para el día a día (RNF-10). Sirve en el celular y en el computador. Para exportarla a PDF, ábrela en GitHub e imprímela como PDF desde el navegador.

## Entrar

- Abre la dirección de la app (la de Cloudflare Pages). En el celular, toca **Agregar a la pantalla de inicio** para tenerla como una app.
- Ingresa con tu **correo** y tu contraseña. La primera vez usa la contraseña inicial y cámbiala en tu perfil (mínimo 8 caracteres, con mayúscula, minúscula, número y signo).
- La sesión no se cierra sola. Si pierdes el celular, cambia tu contraseña desde otro equipo: eso cierra las demás sesiones.

## Todos los días: la tasa del bolívar

- La TRM llega sola cada mañana. La **tasa del bolívar** se registra a mano: si falta, Inicio muestra un aviso con el botón **Registrar tasa**.
- Se escribe dos veces para evitar errores. Si cambia mucho frente a la anterior, la app pide confirmar.
- Una tasa mal escrita se corrige desde **Tasas**. Los documentos ya guardados no cambian.

## Inicio

Muestra el mes en curso:
- ventas, instalaciones, utilidad e inventario, en dólares y con su equivalente en pesos y bolívares;
- productos con stock bajo;
- garantías y cotizaciones por vencer;
- los últimos movimientos. Toca uno para abrirlo.

## Compras

1. **Compras → Registrar compra:** proveedor, número y fecha de la factura, moneda y productos con cantidad y costo.
2. En los productos con serial, escanea o escribe **todos** los seriales.
3. La vista previa muestra cómo cambia el costo de cada producto. Guarda y, si quieres, adjunta la foto de la factura.
4. Una compra se puede anular solo si es el último movimiento de sus productos y sus seriales siguen en bodega.

## Ventas

1. **Nueva venta:** elige el cliente. El precio sugerido depende de si es instalador o cliente final, y se puede cambiar.
2. Agrega productos. En los que tienen serial, elige los seriales que salen.
3. Revisa el panel de cobro: total, costo y utilidad. Si falta stock, no deja guardar.
4. Al guardar: **Descargar PDF** o **Enviar por WhatsApp**.
5. Si hubo un error, **anúlala** con un motivo: el material vuelve a bodega.

## Instalaciones

1. **Nueva instalación:** cliente, dirección, fecha, técnicos y descripción del trabajo.
2. Agrega el material con sus seriales, la mano de obra y la garantía de mano de obra (1 a 3 meses).
3. Después de guardar, sube las **fotos** de antes, durante y después.
4. Comparte el comprobante por PDF o WhatsApp; incluye los seriales y las fechas de garantía.

## Cotizaciones

- **Ciclo:**
  1. **Nueva cotización:** venta o instalación, validez de 8, 15 o 30 días. Queda en **Borrador**.
  2. Envíala por WhatsApp o descarga el PDF: queda **En evaluación**.
  3. Si el cliente la acepta: **Cliente aprobó**.
  4. **Convertir:** se abre la venta o instalación ya llena; completa los seriales y guarda.
- **Si la editas estando en evaluación**, se guarda como nueva versión (COT-0001 v2).
- **Si el cliente no acepta**, márcala **Rechazada**. Las que pasan su fecha quedan **Vencidas** solas. Para retomarla, **duplícala**.
- **Botón de seguimiento:** abre WhatsApp con un mensaje listo para el cliente.

## Inventario y ajustes

- **Inventario:** busca por nombre, código o número de serie. En cada producto ves el kárdex, el historial de costo y los seriales.
- **Ajuste:** para pérdidas, daños, conteo físico o garantías. Los ajustes no se anulan: un error se corrige con otro ajuste.

## Garantías y reclamos

- **Garantías:** busca por cliente o por serial para ver hasta cuándo está cubierto un equipo o una instalación.
- **Reclamo:** anota el problema y, después, la solución. Si la garantía ya venció, queda marcado "fuera de garantía".

## Reportes y Excel

- **Reportes:** elige semana, mes, año o un rango, y muévete con las flechas. Muestra ventas, instalaciones, utilidad, los productos más vendidos, las compras por proveedor y las cotizaciones aprobadas.
- **Botón Excel:** está en cada listado y en cada reporte. Descarga lo mismo que ves, con los montos listos para sumar.

## Si algo no funciona

- **La app tarda en abrir (hasta un minuto):** el servidor estaba dormido; espera y vuelve a intentar. Si pasa seguido, revisen el monitor de UptimeRobot.
- **Sale un error al guardar:** lee el mensaje; casi siempre dice qué corregir (stock insuficiente, serial no disponible, falta la tasa…).
- **Recuperar información borrada o dañada:** hay copias de cada noche, y la guía es `docs/copias-produccion.md`.
