package co.italarm.api.garantias.aplicacion;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Reclamo de garantía (RF-125).
 *
 * @param tipo INSTALACION o SERIAL
 * @param enGarantia false si se registró después del vencimiento (P-45)
 */
public record ReclamoVista(
    Long id,
    String tipo,
    Long instalacionId,
    Long serialId,
    String serial,
    String documento,
    Long clienteId,
    String cliente,
    LocalDate fecha,
    String problema,
    String solucion,
    boolean enGarantia,
    LocalDate vencimiento,
    String registradoPor,
    Instant registradoEn,
    long version) {}
