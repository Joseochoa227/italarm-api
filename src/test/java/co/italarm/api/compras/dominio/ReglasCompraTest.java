package co.italarm.api.compras.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReglasCompraTest {

  private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

  @Test
  void fechaAnteriorAHoy_seAcepta() {
    assertThatCode(
            () ->
                ReglasCompra.validar(
                    HOY.minusDays(10),
                    HOY,
                    List.of(new ReglasCompra.Linea(1L, BigDecimal.ONE, BigDecimal.TEN))))
        .doesNotThrowAnyException();
  }

  @Test
  void fechaFutura_seRechaza() {
    assertThatThrownBy(
            () ->
                ReglasCompra.validar(
                    HOY.plusDays(1),
                    HOY,
                    List.of(new ReglasCompra.Linea(1L, BigDecimal.ONE, BigDecimal.TEN))))
        .isInstanceOf(CompraInvalidaException.class)
        .extracting("codigo")
        .isEqualTo(ReglasCompra.FECHA_FUTURA);
  }

  @Test
  void sinLineas_seRechaza() {
    assertThatThrownBy(() -> ReglasCompra.validar(HOY, HOY, List.of()))
        .extracting("codigo")
        .isEqualTo(ReglasCompra.SIN_LINEAS);
  }

  @Test
  void productoRepetido_seRechaza() {
    assertThatThrownBy(
            () ->
                ReglasCompra.validar(
                    HOY,
                    HOY,
                    List.of(
                        new ReglasCompra.Linea(1L, BigDecimal.ONE, BigDecimal.TEN),
                        new ReglasCompra.Linea(1L, BigDecimal.ONE, BigDecimal.ONE))))
        .extracting("codigo")
        .isEqualTo(ReglasCompra.PRODUCTO_REPETIDO);
  }

  @Test
  void costoCero_seRechaza() {
    assertThatThrownBy(
            () ->
                ReglasCompra.validar(
                    HOY, HOY, List.of(new ReglasCompra.Linea(1L, BigDecimal.ONE, BigDecimal.ZERO))))
        .extracting("codigo")
        .isEqualTo(ReglasCompra.COSTO_INVALIDO);
  }

  @Test
  void cantidadCero_seRechaza() {
    assertThatThrownBy(
            () ->
                ReglasCompra.validar(
                    HOY, HOY, List.of(new ReglasCompra.Linea(1L, BigDecimal.ZERO, BigDecimal.TEN))))
        .extracting("codigo")
        .isEqualTo("CANTIDAD_INVALIDA");
  }
}
