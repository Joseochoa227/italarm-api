package co.italarm.api.ventas.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Venta en el listado (RF-105).
 *
 * @param productos resumen, por ejemplo "Cámara domo × 2 und, Cable UTP × 30 m y 1 más"
 */
public record VentaResumenVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    Long clienteId,
    String cliente,
    Moneda moneda,
    Dinero total,
    Dinero totalUsd,
    Dinero utilidad,
    String estado,
    String productos,
    String registradaPor,
    Instant registradaEn) {}
