package co.italarm.api.compras.aplicacion;

import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import java.time.LocalDate;
import java.util.List;

/**
 * Compras de un período con sus totales sin las anuladas (RF-46, RF-47, RF-73).
 *
 * @param totalesPorMoneda total de cada moneda de factura y su equivalente en USD
 * @param totalUsd suma en USD de todas las compras activas del período
 */
public record ListadoComprasVista(
    LocalDate desde,
    LocalDate hasta,
    Pagina<CompraResumenVista> compras,
    List<TotalMoneda> totalesPorMoneda,
    Dinero totalUsd) {

  public record TotalMoneda(Dinero total, Dinero totalUsd, long compras) {}
}
