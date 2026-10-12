package co.italarm.api.reportes.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Utilidad de una venta o instalación (RF-143, P-63), en su moneda y en USD.
 *
 * @param tipo VENTA o INSTALACION
 */
public record UtilidadDocumentoVista(
    String tipo,
    Long id,
    String consecutivo,
    LocalDate fecha,
    Long clienteId,
    String cliente,
    Dinero total,
    Dinero costo,
    Dinero utilidad,
    BigDecimal porcentajeUtilidad,
    Dinero totalUsd,
    Dinero costoUsd,
    Dinero utilidadUsd) {}
