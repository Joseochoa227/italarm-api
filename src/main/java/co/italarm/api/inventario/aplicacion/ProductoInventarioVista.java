package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle de un producto en el inventario (RF-53 a RF-55): indicadores en las tres monedas y
 * seriales por estado.
 */
public record ProductoInventarioVista(
    Long id,
    String codigo,
    String nombre,
    String marca,
    String modelo,
    String categoria,
    String abreviatura,
    boolean controlaSerial,
    BigDecimal stock,
    BigDecimal stockMinimo,
    boolean bajoMinimo,
    MontoEnMonedas costoActual,
    MontoEnMonedas valorEnBodega,
    MontoEnMonedas precioInstalador,
    MontoEnMonedas precioClienteFinal,
    SerialesPorEstado seriales,
    String fotoUrl,
    boolean activo,
    List<String> avisos) {

  /** Cantidad de seriales del producto en cada estado (RF-55). */
  public record SerialesPorEstado(
      long enBodega, long vendidos, long instalados, long dadosDeBaja, long anulados) {}
}
