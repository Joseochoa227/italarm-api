package co.italarm.api.garantias.aplicacion;

import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.EstadoGarantia;
import java.time.LocalDate;

/**
 * Una garantía (RF-123, RF-124).
 *
 * @param tipo INSTALACION o VENTA: el documento en que salió
 * @param clase MANO_OBRA (de una instalación) o EQUIPO (un serial)
 * @param serial número de serie, o vacío en la mano de obra
 * @param diasRestantes días hasta el vencimiento; negativos si ya venció
 */
public record GarantiaVista(
    String tipo,
    String clase,
    DocumentoRef documento,
    LocalDate fecha,
    Long clienteId,
    String cliente,
    Long serialId,
    String serial,
    String producto,
    LocalDate vencimiento,
    EstadoGarantia estado,
    long diasRestantes) {}
