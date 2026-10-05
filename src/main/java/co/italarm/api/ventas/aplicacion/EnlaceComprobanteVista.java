package co.italarm.api.ventas.aplicacion;

import java.time.Instant;

/**
 * Enlace público del comprobante para enviarlo por WhatsApp (RF-134, P-33).
 *
 * @param mensaje texto listo para enviar, con el enlace
 * @param whatsappUrl abre WhatsApp con el número del cliente y el mensaje; vacío si el cliente no
 *     tiene teléfono
 */
public record EnlaceComprobanteVista(
    String url, Instant venceEn, String mensaje, String whatsappUrl) {}
