package co.italarm.api.instalaciones.aplicacion;

import java.time.Instant;

/**
 * Enlace público del comprobante para enviarlo por WhatsApp (RF-134, P-33).
 *
 * @param whatsappUrl abre WhatsApp con el número del cliente y el mensaje; vacío si no tiene
 *     teléfono
 */
public record EnlaceComprobante(String url, Instant venceEn, String mensaje, String whatsappUrl) {}
