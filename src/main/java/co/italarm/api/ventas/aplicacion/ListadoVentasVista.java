package co.italarm.api.ventas.aplicacion;

import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import java.time.LocalDate;
import java.util.List;

/**
 * Ventas de un período con sus totales sin las anuladas (RF-105, RF-73).
 *
 * @param totalUsd suma en USD de lo vendido en el período
 * @param utilidadUsd suma en USD de la utilidad del período
 */
public record ListadoVentasVista(
    LocalDate desde,
    LocalDate hasta,
    Pagina<VentaResumenVista> ventas,
    List<TotalMoneda> totalesPorMoneda,
    Dinero totalUsd,
    Dinero utilidadUsd) {

  public record TotalMoneda(Dinero total, Dinero costo, Dinero utilidad, long ventas) {}
}
