package co.italarm.api.reportes.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores de Inicio (RF-08) y de los reportes (RF-136). La moneda principal es USD, con los
 * totales guardados en cada documento; los equivalentes en COP y VES usan las tasas de hoy (P-57).
 * Sin documentos anulados.
 */
public record IndicadoresVista(
    Documentos ventas,
    Documentos instalaciones,
    Utilidad utilidad,
    InventarioValorizado inventario) {

  /**
   * @param total en USD y sus equivalentes
   * @param porMoneda lo cobrado en cada moneda, como en los listados
   */
  public record Documentos(long cantidad, MontoEnMonedas total, List<Dinero> porMoneda) {}

  /**
   * @param porcentaje utilidad ÷ lo cobrado (ventas + instalaciones) × 100; vacío si no hay cobros
   */
  public record Utilidad(MontoEnMonedas total, BigDecimal porcentaje) {}

  /**
   * Inventario de hoy (P-58), aunque el período sea anterior.
   *
   * @param productos productos con stock
   * @param fecha la fecha del cálculo: hoy
   */
  public record InventarioValorizado(long productos, MontoEnMonedas valor, LocalDate fecha) {}
}
