package co.italarm.api.documentos.aplicacion;

import java.time.Instant;

/** Enlace público recién creado: la dirección completa y su vencimiento. */
public record EnlaceCreado(String url, Instant venceEn) {}
