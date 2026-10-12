package co.italarm.api.reportes.aplicacion;

import co.italarm.api.cotizaciones.aplicacion.CotizacionResumenVista;
import co.italarm.api.garantias.aplicacion.GarantiaVista;
import co.italarm.api.tasas.aplicacion.TasasVigentesVista;
import java.time.LocalDate;
import java.util.List;

/**
 * Pantalla de Inicio (sección 3.2) en una sola respuesta (RNF-08). Cada bloque trae como máximo 10
 * filas y su total, para el enlace "Ver todo".
 *
 * @param usuario nombre para el saludo (RF-06)
 * @param avisosTasas falta la tasa del bolívar de hoy o la TRM automática no llegó (RF-07): se
 *     muestran antes que cualquier otro bloque, con el botón Registrar tasa
 * @param indicadores del mes en curso (RF-08)
 * @param garantiasPorVencer las que vencen en los próximos 30 días (RF-11)
 * @param cotizacionesEnEvaluacion primero las que vencen antes (RF-12, RF-91)
 */
public record InicioVista(
    String usuario,
    LocalDate hoy,
    List<AvisoTasa> avisosTasas,
    TasasVigentesVista tasas,
    IndicadoresVista indicadores,
    List<StockBajoVista> stockBajo,
    long totalStockBajo,
    List<GarantiaVista> garantiasPorVencer,
    long totalGarantiasPorVencer,
    List<CotizacionResumenVista> cotizacionesEnEvaluacion,
    long totalCotizacionesEnEvaluacion,
    List<MovimientoRecienteVista> ultimosMovimientos) {

  /**
   * @param par USD_COP o USD_VES
   */
  public record AvisoTasa(String par, String mensaje) {}
}
