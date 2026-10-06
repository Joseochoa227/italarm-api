package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.EstadoGarantia;
import co.italarm.api.shared.dominio.Moneda;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Instalación en el listado (RF-121).
 *
 * @param tecnicos nombres separados por coma
 * @param estadoGarantia de la mano de obra; vacío si la instalación está anulada
 */
public record InstalacionResumenVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    Long clienteId,
    String cliente,
    String direccion,
    String tecnicos,
    Moneda moneda,
    Dinero total,
    Dinero totalUsd,
    Dinero utilidad,
    LocalDate venceManoObra,
    EstadoGarantia estadoGarantia,
    String estado,
    String registradaPor,
    Instant registradaEn) {}
