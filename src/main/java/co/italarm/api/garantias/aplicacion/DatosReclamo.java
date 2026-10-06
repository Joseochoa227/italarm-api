package co.italarm.api.garantias.aplicacion;

import java.time.LocalDate;

/**
 * Reclamo a registrar: sobre una instalación o un serial, no ambos (RF-125).
 *
 * @param fecha vacía es hoy
 */
public record DatosReclamo(
    Long instalacionId, Long serialId, LocalDate fecha, String problema, String solucion) {}
