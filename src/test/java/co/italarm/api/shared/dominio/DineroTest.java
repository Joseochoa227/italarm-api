package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DineroTest {

  @Test
  void sumaYRestaConLaMismaMoneda() {
    Dinero a = Dinero.de("19.50", Moneda.USD);
    Dinero b = Dinero.de("0.50", Moneda.USD);

    assertThat(a.sumar(b)).isEqualTo(Dinero.de("20", Moneda.USD));
    assertThat(a.restar(b)).isEqualTo(Dinero.de("19", Moneda.USD));
  }

  @Test
  void conservaLaExactitudDecimal() {
    Dinero resultado = Dinero.de("0.1", Moneda.USD).sumar(Dinero.de("0.2", Moneda.USD));

    assertThat(resultado.monto()).isEqualByComparingTo("0.3");
  }

  @Test
  void multiplicaPorUnaCantidad() {
    Dinero unitario = Dinero.de("17.50", Moneda.USD);

    assertThat(unitario.multiplicar(new BigDecimal("20"))).isEqualTo(Dinero.de("350", Moneda.USD));
  }

  @Test
  void calculaUnPorcentaje() {
    Dinero total = Dinero.de("100", Moneda.USD);

    assertThat(total.porcentaje(new BigDecimal("7"))).isEqualTo(Dinero.de("7", Moneda.USD));
  }

  @Test
  void noMezclaMonedas() {
    Dinero dolares = Dinero.de("1", Moneda.USD);
    Dinero pesos = Dinero.de("4000", Moneda.COP);

    assertThatThrownBy(() -> dolares.sumar(pesos))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("USD")
        .hasMessageContaining("COP");
    assertThatThrownBy(() -> dolares.restar(pesos)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> dolares.compareTo(pesos)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void laIgualdadNoDependeDeLaEscala() {
    assertThat(Dinero.de("1.0", Moneda.USD)).isEqualTo(Dinero.de("1.0000", Moneda.USD));
    assertThat(Dinero.de("1.0", Moneda.USD)).hasSameHashCodeAs(Dinero.de("1.0000", Moneda.USD));
    assertThat(Dinero.de("1", Moneda.USD)).isNotEqualTo(Dinero.de("1", Moneda.COP));
    assertThat(Dinero.de("1", Moneda.USD)).isNotEqualTo(Dinero.de("2", Moneda.USD));
  }

  @Test
  void comparaYConsultaElSigno() {
    Dinero cero = Dinero.cero(Moneda.VES);

    assertThat(cero.esCero()).isTrue();
    assertThat(Dinero.de("-1", Moneda.VES).esNegativo()).isTrue();
    assertThat(Dinero.de("2", Moneda.VES).compareTo(Dinero.de("1", Moneda.VES))).isPositive();
    assertThat(Dinero.de("2", Moneda.VES).esMayorQue(Dinero.de("1", Moneda.VES))).isTrue();
  }

  @Test
  void redondeaParaAlmacenarYMostrar() {
    Dinero costo = Dinero.de("19.123456", Moneda.USD);

    assertThat(costo.paraAlmacenar().monto()).isEqualByComparingTo("19.1235");
    assertThat(costo.paraMostrar().monto()).isEqualByComparingTo("19.12");
  }

  @Test
  void exigeMontoYMoneda() {
    assertThatThrownBy(() -> new Dinero(null, Moneda.USD)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Dinero(BigDecimal.ONE, null))
        .isInstanceOf(NullPointerException.class);
  }
}
