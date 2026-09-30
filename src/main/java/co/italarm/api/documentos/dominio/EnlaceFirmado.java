package co.italarm.api.documentos.dominio;

/**
 * Firma de un enlace de descarga.
 *
 * @param expira segundos desde 1970 (UTC) en que el enlace deja de servir
 */
public record EnlaceFirmado(String clave, long expira, String firma) {}
