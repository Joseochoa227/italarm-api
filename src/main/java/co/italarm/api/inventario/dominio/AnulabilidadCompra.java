package co.italarm.api.inventario.dominio;

import java.util.List;
import java.util.Optional;

/**
 * Cuándo se puede anular una compra (RF-71, P-23): para cada producto, la compra debe ser su último
 * movimiento en el kárdex y sus seriales deben seguir en bodega.
 */
public final class AnulabilidadCompra {

  /**
   * Situación de un producto de la compra.
   *
   * @param ultimoMovimiento si la compra es el último movimiento del producto en el kárdex
   * @param serialesEnBodega si todos los seriales que entraron con la compra siguen en bodega
   */
  public record Producto(String nombre, boolean ultimoMovimiento, boolean serialesEnBodega) {}

  private AnulabilidadCompra() {}

  /** El motivo por el que no se puede anular, o vacío si se puede. */
  public static Optional<String> motivoNoAnulable(List<Producto> productos) {
    for (Producto producto : productos) {
      if (!producto.ultimoMovimiento()) {
        return Optional.of(
            producto.nombre()
                + " tuvo movimientos después de esta compra. Corrige con un ajuste de"
                + " inventario.");
      }
      if (!producto.serialesEnBodega()) {
        return Optional.of(
            "Algunos seriales de "
                + producto.nombre()
                + " ya no están en bodega. Corrige con un ajuste de inventario.");
      }
    }
    return Optional.empty();
  }
}
