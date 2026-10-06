package co.italarm.api.comercial.aplicacion;

/**
 * Cliente de una venta o instalación.
 *
 * @param tipo INSTALADOR o CLIENTE_FINAL
 * @param precioAplicado "Se le aplicará el precio instalador" (RF-98); vacío en documentos
 *     guardados
 */
public record ClienteDocumentoVista(
    Long id,
    String tipo,
    String nombre,
    String documento,
    String telefono,
    String direccion,
    String ciudad,
    String precioAplicado) {}
