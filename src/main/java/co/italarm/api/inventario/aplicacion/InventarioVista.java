package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;
import java.util.List;

/**
 * Listado valorizado del inventario (RF-49 a RF-52). Los valores llegan en USD y convertidos con
 * las tasas vigentes; si falta una tasa, ese equivalente queda vacío y hay un aviso.
 *
 * @param totalProductos productos que cumplen el filtro (todas las páginas)
 * @param valorTotal valor en bodega de todos esos productos
 */
public record InventarioVista(
    Pagina<Producto> productos,
    long totalProductos,
    MontoEnMonedas valorTotal,
    List<String> avisos) {

  /**
   * Producto del listado.
   *
   * @param bajoMinimo para la etiqueta "Bajo"
   * @param valorEnBodega stock × costo actual
   */
  public record Producto(
      Long id,
      String codigo,
      String nombre,
      String marca,
      String categoria,
      String abreviatura,
      boolean controlaSerial,
      BigDecimal stock,
      BigDecimal stockMinimo,
      boolean bajoMinimo,
      Dinero costoActualUsd,
      MontoEnMonedas valorEnBodega,
      String fotoUrl,
      boolean activo) {}
}
