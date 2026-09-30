package co.italarm.api.tasas.aplicacion;

import java.time.LocalDate;

/**
 * Tasas del día para el menú lateral y la barra superior (RF-30).
 *
 * @param trmAutomaticaFallo la consulta automática de hoy falló y aún no hay TRM oficial
 */
public record TasasVigentesVista(
    LocalDate hoy, TasaVigenteVista trm, TasaVigenteVista bolivar, boolean trmAutomaticaFallo) {}
