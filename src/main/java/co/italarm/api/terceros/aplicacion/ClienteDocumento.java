package co.italarm.api.terceros.aplicacion;

/**
 * Datos de un cliente para un documento (venta, instalación, cotización).
 *
 * @param tipo INSTALADOR o CLIENTE_FINAL
 * @param precioInstalador true si se le aplica el precio instalador (RN-01)
 * @param documento por ejemplo "CC 1020304050", o vacío
 */
public record ClienteDocumento(
    Long id,
    String tipo,
    boolean precioInstalador,
    String precioAplicadoDescripcion,
    String nombre,
    String documento,
    String telefono,
    String direccion,
    String ciudad) {}
