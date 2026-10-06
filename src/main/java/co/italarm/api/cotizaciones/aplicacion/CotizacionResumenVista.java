package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Fila del listado de cotizaciones (RF-90).
 *
 * @param diasParaVencer solo en Borrador o En evaluación; vacío en los demás estados
 * @param porVencer en evaluación con 3 días o menos: se resalta (RF-91)
 * @param documentoGenerado consecutivo de la venta o instalación generada, o vacío
 */
public record CotizacionResumenVista(
    Long id,
    String consecutivo,
    String tipo,
    LocalDate fecha,
    Long clienteId,
    String cliente,
    Moneda moneda,
    Dinero total,
    Dinero totalUsd,
    String estado,
    LocalDate vence,
    Long diasParaVencer,
    boolean porVencer,
    String documentoGenerado,
    String registradaPor,
    Instant registradaEn) {}
