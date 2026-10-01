package co.italarm.api.inventario.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * P-23: una compra se anula si es el último movimiento de cada producto y sus seriales siguen en
 * bodega.
 */
class AnulabilidadCompraTest {

  @Test
  void seAnulaSiEsElUltimoMovimientoDeCadaProducto() {
    assertThat(
            AnulabilidadCompra.motivoNoAnulable(
                List.of(
                    new AnulabilidadCompra.Producto("Cámara domo", true, true),
                    new AnulabilidadCompra.Producto("Cable UTP", true, true))))
        .isEmpty();
  }

  @Test
  void noSeAnulaSiHuboMovimientosPosteriores() {
    assertThat(
            AnulabilidadCompra.motivoNoAnulable(
                List.of(
                    new AnulabilidadCompra.Producto("Cámara domo", true, true),
                    new AnulabilidadCompra.Producto("Cable UTP", false, true))))
        .contains(
            "Cable UTP tuvo movimientos después de esta compra. Corrige con un ajuste de"
                + " inventario.");
  }

  @Test
  void noSeAnulaSiAlgunSerialYaNoEstaEnBodega() {
    assertThat(
            AnulabilidadCompra.motivoNoAnulable(
                List.of(new AnulabilidadCompra.Producto("Cámara domo", true, false))))
        .contains(
            "Algunos seriales de Cámara domo ya no están en bodega. Corrige con un ajuste de"
                + " inventario.");
  }
}
