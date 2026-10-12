package co.italarm.api.reportes.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte del período (sección 3.16). Todo en USD con los valores guardados en cada documento
 * (RN-04, P-57); sin anulados.
 *
 * @param tipo SEMANA, MES, ANIO o RANGO
 * @param anterior y {@code siguiente}: períodos para las flechas (RF-135); vacíos en un rango libre
 * @param agrupacion SEMANA o MES: cómo se agrupan las barras (P-60)
 * @param serie barras de ventas e instalaciones (RF-137)
 * @param masVendidos y {@code masInstalados}: los 10 primeros por valor (RF-138, P-59)
 * @param stockBajo productos bajo el mínimo hoy (RF-140)
 */
public record ReportePeriodoVista(
    String tipo,
    LocalDate desde,
    LocalDate hasta,
    PeriodoVista anterior,
    PeriodoVista siguiente,
    IndicadoresVista indicadores,
    String agrupacion,
    List<Barra> serie,
    List<ProductoTop> masVendidos,
    List<ProductoTop> masInstalados,
    List<CompraProveedor> comprasPorProveedor,
    Dinero totalCompras,
    List<StockBajoVista> stockBajo,
    Cotizaciones cotizaciones) {

  public record PeriodoVista(LocalDate desde, LocalDate hasta) {}

  /**
   * @param etiqueta "05/10 al 11/10" o "Octubre 2026"
   */
  public record Barra(
      String etiqueta,
      LocalDate desde,
      LocalDate hasta,
      long ventas,
      Dinero totalVentas,
      long instalaciones,
      Dinero totalInstalaciones) {}

  /**
   * @param valor subtotal de las líneas en USD, con las tasas de cada documento
   */
  public record ProductoTop(
      Long productoId,
      String codigo,
      String nombre,
      BigDecimal cantidad,
      String unidad,
      Dinero valor) {}

  public record CompraProveedor(Long proveedorId, String proveedor, long compras, Dinero total) {}

  /**
   * Cotizaciones del período por estado (RF-142).
   *
   * @param porcentajeAprobadas aprobadas y convertidas sobre las que ya tuvieron respuesta
   *     (aprobadas, convertidas y rechazadas); vacío si ninguna tuvo respuesta
   */
  public record Cotizaciones(
      List<PorEstado> porEstado, long total, BigDecimal porcentajeAprobadas) {}

  /**
   * @param estado BORRADOR, EN_EVALUACION, APROBADA, CONVERTIDA, RECHAZADA o VENCIDA
   */
  public record PorEstado(String estado, long cantidad) {}
}
