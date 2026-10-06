package co.italarm.api.cotizaciones.aplicacion;

/**
 * Mensaje de seguimiento por WhatsApp (RF-92, P-53).
 *
 * @param whatsappUrl enlace wa.me con el número del cliente y el mensaje, o vacío si no tiene
 *     número
 */
public record SeguimientoCotizacionVista(String mensaje, String whatsappUrl) {}
