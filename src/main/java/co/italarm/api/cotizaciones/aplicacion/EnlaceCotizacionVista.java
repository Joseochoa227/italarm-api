package co.italarm.api.cotizaciones.aplicacion;

import java.time.Instant;

/**
 * Enlace público del PDF de la cotización para WhatsApp (RF-134).
 *
 * @param whatsappUrl enlace wa.me con el número del cliente y el mensaje, o vacío si no tiene
 *     número
 * @param estado de la cotización después de crear el enlace (queda En evaluación, P-50)
 */
public record EnlaceCotizacionVista(
    String url, Instant venceEn, String mensaje, String whatsappUrl, String estado) {}
