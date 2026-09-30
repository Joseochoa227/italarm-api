package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FormatoDineroTest {

  @ParameterizedTest(name = "{0} {1} -> {2}")
  @CsvSource(
      delimiter = '|',
      value = {
        "1250000    | COP | $ 1.250.000",
        "1250000.49 | COP | $ 1.250.000",
        "1250000.5  | COP | $ 1.250.001",
        "1939.04    | USD | US$ 1.939,04",
        "19.5       | USD | US$ 19,50",
        "1234.56    | VES | Bs 1.234,56",
        "0          | USD | US$ 0,00",
        "0          | COP | $ 0",
        "-3900      | COP | -$ 3.900",
        "-7         | USD | -US$ 7,00",
        "-0.001     | USD | US$ 0,00",
        "1000000000 | VES | Bs 1.000.000.000,00"
      })
  void formateaSegunLaMoneda(String monto, Moneda moneda, String esperado) {
    assertThat(FormatoDinero.formatear(Dinero.de(monto, moneda))).isEqualTo(esperado);
  }
}
