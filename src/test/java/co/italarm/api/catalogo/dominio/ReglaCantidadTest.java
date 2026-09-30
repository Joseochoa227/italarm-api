package co.italarm.api.catalogo.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** P-09: Metro admite hasta 2 decimales; Unidad y Par, solo enteros. */
class ReglaCantidadTest {

  @ParameterizedTest
  @ValueSource(strings = {"0", "12", "12.5", "12.50", "107.25", "12.500"})
  void enMetrosAceptaHastaDosDecimales(String cantidad) {
    assertThat(ReglaCantidad.validar(new BigDecimal(cantidad), true, "m", "Stock mínimo"))
        .isEqualByComparingTo(cantidad);
  }

  @Test
  void enMetrosRechazaMasDeDosDecimales() {
    assertThatThrownBy(
            () -> ReglaCantidad.validar(new BigDecimal("12.555"), true, "m", "Stock mínimo"))
        .isInstanceOf(CantidadInvalidaException.class)
        .hasMessage("Stock mínimo: en m se admiten máximo 2 decimales.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "24", "24.0", "24.000"})
  void enUnidadesAceptaEnteros(String cantidad) {
    assertThat(ReglaCantidad.validar(new BigDecimal(cantidad), false, "und", "Stock mínimo"))
        .isEqualByComparingTo(cantidad);
  }

  @Test
  void enUnidadesRechazaDecimales() {
    assertThatThrownBy(
            () -> ReglaCantidad.validar(new BigDecimal("2.5"), false, "und", "Stock mínimo"))
        .isInstanceOf(CantidadInvalidaException.class)
        .hasMessage("Stock mínimo: en und la cantidad debe ser un número entero.");
  }

  @Test
  void rechazaNegativos() {
    assertThatThrownBy(() -> ReglaCantidad.validar(new BigDecimal("-1"), true, "m", "Stock mínimo"))
        .isInstanceOf(CantidadInvalidaException.class)
        .hasMessage("Stock mínimo: la cantidad no puede ser negativa.");
  }

  @Test
  void unaCantidadVaciaNoSeValida() {
    assertThat(ReglaCantidad.validar(null, false, "und", "Stock mínimo")).isNull();
  }
}
