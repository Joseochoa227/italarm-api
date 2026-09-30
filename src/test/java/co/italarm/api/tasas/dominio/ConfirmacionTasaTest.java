package co.italarm.api.tasas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RF-35a: la tasa manual se digita dos veces y ambas deben coincidir. */
class ConfirmacionTasaTest {

  @Test
  void cp10_tasaDigitadaDistintaDosVeces_noSeAcepta() {
    assertThatThrownBy(
            () -> ConfirmacionTasa.validar(new BigDecimal("36.50"), new BigDecimal("36.05")))
        .isInstanceOf(TasaNoConfirmadaException.class)
        .hasMessage("Las dos tasas digitadas no coinciden. Digítala de nuevo.");
  }

  @Test
  void aceptaLaMismaTasaAunqueCambienLosCerosFinales() {
    assertThat(ConfirmacionTasa.validar(new BigDecimal("36.5"), new BigDecimal("36.500")))
        .isEqualTo(new BigDecimal("36.500000"));
  }

  @Test
  void laTasaDebeSerMayorQueCero() {
    assertThatThrownBy(() -> ConfirmacionTasa.validar(BigDecimal.ZERO, BigDecimal.ZERO))
        .isInstanceOf(TasaInvalidaException.class)
        .hasMessage("La tasa debe ser mayor que 0.");
    assertThatThrownBy(() -> ConfirmacionTasa.validar(null, BigDecimal.ONE))
        .isInstanceOf(TasaInvalidaException.class);
  }

  @Test
  void admiteMaximoSeisDecimales() {
    assertThatThrownBy(
            () ->
                ConfirmacionTasa.validar(new BigDecimal("1.1234567"), new BigDecimal("1.1234567")))
        .isInstanceOf(TasaInvalidaException.class)
        .hasMessage("La tasa admite máximo 6 decimales.");
  }
}
