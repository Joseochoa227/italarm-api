package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GarantiaTest {

  @Test
  void cp25_ventaDel15DeOctubre_garantiaHasta15DeEnero() {
    assertThat(Garantia.vencimiento(LocalDate.of(2026, 10, 15), 3))
        .isEqualTo(LocalDate.of(2027, 1, 15));
  }

  @Test
  void finDeMes_quedaEnElUltimoDiaDelMesDeVencimiento() {
    assertThat(Garantia.vencimiento(LocalDate.of(2026, 11, 30), 3))
        .isEqualTo(LocalDate.of(2027, 2, 28));
  }
}
