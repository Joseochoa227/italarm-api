package co.italarm.api.ventas.aplicacion;

/**
 * Cliente de la venta.
 *
 * @param tipo INSTALADOR o CLIENTE_FINAL
 * @param precioAplicado "Se le aplicará el precio instalador" (RF-98); vacío en ventas guardadas
 */
public record ClienteVentaVista(
    Long id,
    String tipo,
    String nombre,
    String documento,
    String telefono,
    String direccion,
    String ciudad,
    String precioAplicado) {}
