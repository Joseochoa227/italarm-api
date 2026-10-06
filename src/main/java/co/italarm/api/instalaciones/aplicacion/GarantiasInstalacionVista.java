package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.shared.dominio.EstadoGarantia;
import java.time.LocalDate;

/**
 * Garantías de una instalación (RF-113 a RF-115).
 *
 * @param estadoManoObra VIGENTE, POR_VENCER o VENCIDA; vacío en la vista previa
 * @param venceEquipos vencimiento de los equipos con serial instalados (RF-114)
 */
public record GarantiasInstalacionVista(
    int manoObraMeses,
    LocalDate venceManoObra,
    EstadoGarantia estadoManoObra,
    LocalDate venceEquipos,
    EstadoGarantia estadoEquipos,
    String condiciones) {}
