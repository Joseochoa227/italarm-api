package co.italarm.api.inventario.dominio;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RF-62, RF-65, RN-05: el stock nunca queda en negativo. */
class ReglaStockTest {

  @Test
  void permiteSacarHastaElStockDisponible() {
    assertThatNoException()
        .isThrownBy(
            () -> ReglaStock.exigirDisponible(new BigDecimal("24"), new BigDecimal("24"), "und"));
  }

  @Test
  void rechazaUnaSalidaMayorAlStock() {
    assertThatThrownBy(
            () -> ReglaStock.exigirDisponible(new BigDecimal("50"), new BigDecimal("60"), "m"))
        .isInstanceOf(StockInsuficienteException.class)
        .hasMessage("Stock insuficiente · quedan 50 m");
    assertThatThrownBy(
            () -> ReglaStock.exigirDisponible(new BigDecimal("12.500"), new BigDecimal("13"), "m"))
        .hasMessage("Stock insuficiente · quedan 12.5 m");
  }
}
