package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TasasTest {

  private final Tasas tasas = new Tasas(new BigDecimal("4000"), new BigDecimal("50"));

  @Test
  void convierteAUsdConLaTasaDeCadaMoneda() {
    assertThat(tasas.aUsd(new BigDecimal("76000"), Moneda.COP)).isEqualByComparingTo("19");
    assertThat(tasas.aUsd(new BigDecimal("950"), Moneda.VES)).isEqualByComparingTo("19");
    assertThat(tasas.aUsd(new BigDecimal("19.5"), Moneda.USD)).isEqualByComparingTo("19.5");
  }

  @Test
  void convierteDesdeUsd() {
    assertThat(tasas.desdeUsd(new BigDecimal("19.5"), Moneda.COP)).isEqualByComparingTo("78000");
    assertThat(tasas.desdeUsd(new BigDecimal("19.5"), Moneda.VES)).isEqualByComparingTo("975");
    assertThat(tasas.desdeUsd(new BigDecimal("19.5"), Moneda.USD)).isEqualByComparingTo("19.5");
  }

  @Test
  void daLosEquivalentesEnLasTresMonedas() {
    MontoEnMonedas equivalentes = tasas.equivalentes(Dinero.de("81900", Moneda.COP));

    assertThat(equivalentes.usd()).isEqualTo(Dinero.de("20.475", Moneda.USD));
    assertThat(equivalentes.cop()).isEqualTo(Dinero.de("81900", Moneda.COP));
    assertThat(equivalentes.ves()).isEqualTo(Dinero.de("1023.75", Moneda.VES));
  }

  @Test
  void sinUnaTasaElEquivalenteQuedaVacio() {
    Tasas soloTrm = new Tasas(new BigDecimal("4000"), null);

    MontoEnMonedas equivalentes = soloTrm.equivalentes(Dinero.de("10", Moneda.USD));

    assertThat(equivalentes.cop()).isEqualTo(Dinero.de("40000", Moneda.COP));
    assertThat(equivalentes.ves()).isNull();
    assertThat(new Tasas(null, null).equivalentes(Dinero.de("1", Moneda.COP)).usd()).isNull();
  }

  @Test
  void convertirSinLaTasaNecesariaEsUnError() {
    Tasas soloTrm = new Tasas(new BigDecimal("4000"), null);

    assertThatThrownBy(() -> soloTrm.aUsd(BigDecimal.TEN, Moneda.VES))
        .isInstanceOf(TasaNoDisponibleException.class)
        .hasMessage(
            "No hay tasa del bolívar registrada para convertir a dólares. Regístrala antes de continuar.");
    assertThatThrownBy(() -> new Tasas(null, null).desdeUsd(BigDecimal.TEN, Moneda.COP))
        .isInstanceOf(TasaNoDisponibleException.class)
        .hasMessageContaining("TRM");
  }
}
