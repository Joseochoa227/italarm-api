package co.italarm.api.shared.dominio;

/**
 * Copia de los datos del cliente que salen en el comprobante (P-35): si después se cambian, la
 * venta o la instalación conserva los del momento en que se registró.
 *
 * @param tipo INSTALADOR o CLIENTE_FINAL
 * @param documento por ejemplo "CC 1020304050", o vacío
 */
public record CopiaCliente(
    Long id,
    String tipo,
    String nombre,
    String documento,
    String telefono,
    String direccion,
    String ciudad) {}
