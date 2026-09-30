package co.italarm.api.tasas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RF-35b/c: variación frente a la tasa anterior y alerta si supera el límite. */
class VariacionTasaTest {

  private static final BigDecimal LIMITE = new BigDecimal("5");

  @Test
  void cp11_tasaAnterior50YNueva500_superaElLimiteYExigeAceptacion() {
    VariacionTasa variacion =
        VariacionTasa.calcular(new BigDecimal("50"), new BigDecimal("500"), LIMITE);

    assertThat(variacion.porcentaje()).isEqualByComparingTo("900.00");
    assertThat(variacion.superaLimite()).isTrue();
    assertThatThrownBy(() -> variacion.exigirAceptacion(false))
        .isInstanceOf(TasaVariacionNoAceptadaException.class)
        .hasMessage(
            "La nueva tasa varía 900,00 % frente a la anterior (límite 5 %). Confírmala"
                + " expresamente para guardarla.");
    assertThatNoException().isThrownBy(() -> variacion.exigirAceptacion(true));
  }

  @Test
  void dentroDelLimiteNoExigeAceptacion() {
    VariacionTasa variacion =
        VariacionTasa.calcular(new BigDecimal("4000"), new BigDecimal("4200"), LIMITE);

    assertThat(variacion.porcentaje()).isEqualByComparingTo("5.00");
    assertThat(variacion.superaLimite()).isFalse();
    assertThatNoException().isThrownBy(() -> variacion.exigirAceptacion(false));
  }

  @Test
  void unaBajaTambienCuentaComoVariacion() {
    VariacionTasa variacion =
        VariacionTasa.calcular(new BigDecimal("40"), new BigDecimal("36"), LIMITE);

    assertThat(variacion.porcentaje()).isEqualByComparingTo("-10.00");
    assertThat(variacion.superaLimite()).isTrue();
  }

  @Test
  void justoPorEncimaDelLimiteLoSupera() {
    VariacionTasa variacion =
        VariacionTasa.calcular(new BigDecimal("100"), new BigDecimal("105.000001"), LIMITE);

    assertThat(variacion.superaLimite()).isTrue();
  }

  @Test
  void sinTasaAnteriorNoHayVariacion() {
    VariacionTasa variacion = VariacionTasa.calcular(null, new BigDecimal("36.5"), LIMITE);

    assertThat(variacion.anterior()).isNull();
    assertThat(variacion.porcentaje()).isNull();
    assertThat(variacion.superaLimite()).isFalse();
  }
}
