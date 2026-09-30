package co.italarm.api.tasas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TasaCambioTest {

  private static final LocalDate HOY = LocalDate.of(2026, 10, 1);
  private static final Instant AHORA = Instant.parse("2026-10-01T13:00:00Z");

  @Test
  void laManualGuardaSuUsuarioYLaOficialNo() {
    TasaCambio manual =
        TasaCambio.registrarManual(ParMoneda.USD_VES, HOY, new BigDecimal("36.5"), 1L, AHORA);
    TasaCambio oficial = TasaCambio.registrarOficial(HOY, new BigDecimal("3912.45"), AHORA);

    assertThat(manual.getFuente()).isEqualTo(FuenteTasa.MANUAL);
    assertThat(manual.getRegistradaPor()).isEqualTo(1L);
    assertThat(manual.getValor()).isEqualByComparingTo("36.5");
    assertThat(oficial.getPar()).isEqualTo(ParMoneda.USD_COP);
    assertThat(oficial.getFuente()).isEqualTo(FuenteTasa.SUPERFINANCIERA);
    assertThat(oficial.getRegistradaPor()).isNull();
    assertThat(oficial.getFecha()).isEqualTo(HOY);
    assertThat(oficial.getRegistradaEn()).isEqualTo(AHORA);
  }

  @Test
  void laCorreccionGuardaElValorAnteriorYElNuevo() {
    TasaCambio tasa =
        TasaCambio.registrarManual(ParMoneda.USD_VES, HOY, new BigDecimal("36.5"), 1L, AHORA);

    CorreccionTasa correccion =
        tasa.corregir(new BigDecimal("36.05"), 2L, "Error de digitación", AHORA.plusSeconds(60));

    assertThat(tasa.getValor()).isEqualByComparingTo("36.05");
    assertThat(correccion.getValorAnterior()).isEqualByComparingTo("36.5");
    assertThat(correccion.getValorNuevo()).isEqualByComparingTo("36.05");
    assertThat(correccion.getCorregidaPor()).isEqualTo(2L);
    assertThat(correccion.getMotivo()).isEqualTo("Error de digitación");
    assertThat(correccion.isAutomatica()).isFalse();
    assertThat(correccion.getCorregidaEn()).isEqualTo(AHORA.plusSeconds(60));
  }

  @Test
  void p13_laTrmOficialReemplazaALaManualDelMismoDia() {
    TasaCambio manual =
        TasaCambio.registrarManual(ParMoneda.USD_COP, HOY, new BigDecimal("3900"), 1L, AHORA);

    CorreccionTasa correccion =
        manual.reemplazarPorOficial(new BigDecimal("3912.45"), AHORA.plusSeconds(3600));

    assertThat(manual.getFuente()).isEqualTo(FuenteTasa.SUPERFINANCIERA);
    assertThat(manual.getValor()).isEqualByComparingTo("3912.45");
    assertThat(manual.getRegistradaPor()).isNull();
    assertThat(correccion.isAutomatica()).isTrue();
    assertThat(correccion.getCorregidaPor()).isNull();
    assertThat(correccion.getValorAnterior()).isEqualByComparingTo("3900");
    assertThat(correccion.getMotivo()).isEqualTo("Reemplazada por la TRM oficial");
  }
}
