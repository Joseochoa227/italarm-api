package co.italarm.api.reportes.aplicacion;

import co.italarm.api.shared.api.Pagina;
import java.time.LocalDate;
import java.util.List;

/**
 * Seriales disponibles, vendidos e instalados (RF-141).
 *
 * @param porEstado cantidad de seriales por estado (del producto, si se filtró)
 */
public record ReporteSerialesVista(List<PorEstado> porEstado, Pagina<Serial> seriales) {

  /**
   * @param estado EN_BODEGA, VENDIDO, INSTALADO, DADO_DE_BAJA o ANULADO
   */
  public record PorEstado(String estado, long cantidad) {}

  /**
   * @param entrada consecutivo del documento de entrada
   * @param salidaTipo VENTA, INSTALACION o AJUSTE, o vacío si sigue en bodega
   */
  public record Serial(
      Long id,
      String numero,
      String estado,
      Long productoId,
      String codigoProducto,
      String producto,
      LocalDate fechaEntrada,
      String entrada,
      String salidaTipo,
      Long salidaId,
      String salida,
      LocalDate vencimientoGarantia) {}
}
