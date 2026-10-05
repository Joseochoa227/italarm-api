package co.italarm.api.ventas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Tasas;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PrecioSugeridoTest {

  private static final Tasas TASAS = new Tasas(new BigDecimal("4000"), new BigDecimal("36.5"));

  @Test
  void enLaMismaMoneda_esElPrecioDeLista() {
    assertThat(PrecioSugerido.en(Dinero.de("25.5", Moneda.USD), Moneda.USD, TASAS))
        .isEqualByComparingTo("25.5");
  }

  @Test
  void deUsdACop_seRedondeaAPesosEnteros() {
    assertThat(PrecioSugerido.en(Dinero.de("25.555", Moneda.USD), Moneda.COP, TASAS))
        .isEqualByComparingTo("102220");
    assertThat(PrecioSugerido.en(Dinero.de("0.3333", Moneda.USD), Moneda.COP, TASAS))
        .isEqualByComparingTo("1333");
  }

  @Test
  void aUsdOVes_seRedondeaADosDecimales() {
    assertThat(PrecioSugerido.en(Dinero.de("100000", Moneda.COP), Moneda.USD, TASAS))
        .isEqualByComparingTo("25.00");
    assertThat(PrecioSugerido.en(Dinero.de("10.01", Moneda.USD), Moneda.VES, TASAS))
        .isEqualByComparingTo("365.37");
  }
}
