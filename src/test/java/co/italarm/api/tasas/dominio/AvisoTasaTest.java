package co.italarm.api.tasas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** RF-07, RF-33: aviso cuando falta la tasa del día. */
class AvisoTasaTest {

  private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

  @Test
  void cp12_sinTasaDelBolivarDeHoy_avisaQueSeUsaLaUltima() {
    assertThat(AvisoTasa.para(ParMoneda.USD_VES, LocalDate.of(2026, 9, 29), HOY, false))
        .isEqualTo(
            "No se ha registrado la tasa del bolívar de hoy. Se está usando la del 29/09/2026.");
  }

  @Test
  void conLaTasaDeHoyNoHayAviso() {
    assertThat(AvisoTasa.para(ParMoneda.USD_VES, HOY, HOY, false)).isNull();
    assertThat(AvisoTasa.para(ParMoneda.USD_COP, HOY, HOY, true)).isNull();
  }

  @Test
  void sinNingunaTasaPideRegistrarla() {
    assertThat(AvisoTasa.para(ParMoneda.USD_VES, null, HOY, false))
        .isEqualTo("No hay tasa del bolívar registrada. Regístrala para convertir a bolívares.");
    assertThat(AvisoTasa.para(ParMoneda.USD_COP, null, HOY, false))
        .isEqualTo("No hay TRM registrada. Regístrala para convertir a pesos.");
  }

  @Test
  void laTrmQueNoSeHaObtenidoSeExplicaDistintoSiFalloLaConsulta() {
    assertThat(AvisoTasa.para(ParMoneda.USD_COP, LocalDate.of(2026, 9, 30), HOY, false))
        .isEqualTo("Aún no se ha obtenido la TRM de hoy. Se está usando la del 30/09/2026.");
    assertThat(AvisoTasa.para(ParMoneda.USD_COP, LocalDate.of(2026, 9, 30), HOY, true))
        .isEqualTo(
            "La consulta automática de la TRM falló. Se está usando la del 30/09/2026; puedes"
                + " registrarla manualmente.");
  }
}
