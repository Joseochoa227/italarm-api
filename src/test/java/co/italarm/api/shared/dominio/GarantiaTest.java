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

  @Test
  void cp20_vigenteHastaElUno_porVencerSusUltimos30Dias_vencidaDespues() {
    LocalDate vence = Garantia.vencimiento(LocalDate.of(2026, 10, 1), 3);

    assertThat(vence).isEqualTo(LocalDate.of(2027, 1, 1));
    assertThat(Garantia.estado(vence, LocalDate.of(2026, 12, 1))).isEqualTo(EstadoGarantia.VIGENTE);
    assertThat(Garantia.estado(vence, LocalDate.of(2026, 12, 2)))
        .isEqualTo(EstadoGarantia.POR_VENCER);
    assertThat(Garantia.estado(vence, LocalDate.of(2027, 1, 1)))
        .isEqualTo(EstadoGarantia.POR_VENCER);
    assertThat(Garantia.estado(vence, LocalDate.of(2027, 1, 2))).isEqualTo(EstadoGarantia.VENCIDA);
    assertThat(Garantia.diasRestantes(vence, LocalDate.of(2026, 12, 2))).isEqualTo(30);
    assertThat(Garantia.diasRestantes(vence, LocalDate.of(2027, 1, 3))).isEqualTo(-2);
  }
}
