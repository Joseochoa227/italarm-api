package co.italarm.api.garantias.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReclamoGarantiaTest {

  private static final LocalDate HOY = LocalDate.of(2027, 1, 10);
  private static final LocalDate VENCE = LocalDate.of(2027, 1, 1);

  @Test
  void dentroYFueraDeGarantia() {
    ReclamoGarantia dentro =
        ReclamoGarantia.registrar(1L, null, 5L, VENCE, HOY, VENCE, " No graba ", null);
    ReclamoGarantia fuera =
        ReclamoGarantia.registrar(null, 2L, 5L, VENCE.plusDays(1), HOY, VENCE, "Sin video", null);

    assertThat(dentro.isEnGarantia()).isTrue();
    assertThat(dentro.getProblema()).isEqualTo("No graba");
    assertThat(fuera.isEnGarantia()).isFalse();
    assertThat(fuera.getSerialId()).isEqualTo(2L);

    fuera.cambiarSolucion(" Se cambió el adaptador ");
    assertThat(fuera.getSolucion()).isEqualTo("Se cambió el adaptador");
  }

  @Test
  void validaciones() {
    assertThatThrownBy(() -> ReclamoGarantia.registrar(1L, 2L, 5L, HOY, HOY, VENCE, "x", null))
        .isInstanceOf(ReclamoInvalidoException.class);
    assertThatThrownBy(() -> ReclamoGarantia.registrar(null, null, 5L, HOY, HOY, VENCE, "x", null))
        .isInstanceOf(ReclamoInvalidoException.class);
    assertThatThrownBy(
            () -> ReclamoGarantia.registrar(1L, null, 5L, HOY.plusDays(1), HOY, VENCE, "x", null))
        .hasMessageContaining("posterior a hoy");
    assertThatThrownBy(() -> ReclamoGarantia.registrar(1L, null, 5L, HOY, HOY, VENCE, " ", null))
        .hasMessageContaining("problema");
  }
}
