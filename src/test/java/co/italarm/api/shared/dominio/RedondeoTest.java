package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class RedondeoTest {

  @Test
  void usaHalfUpEnUnSoloLugar() {
    assertThat(Redondeo.MODO).isEqualTo(RoundingMode.HALF_UP);
  }

  @Test
  void escalaDeCalculoEsDeSeisDecimales() {
    assertThat(Redondeo.paraCalculo(new BigDecimal("1.23456750"))).isEqualTo("1.234568");
  }

  @Test
  void escalaDeAlmacenamientoEsDeCuatroDecimales() {
    assertThat(Redondeo.paraAlmacenar(new BigDecimal("17.49995"))).isEqualTo("17.5000");
  }

  @Test
  void escalaParaMostrarDependeDeLaMoneda() {
    BigDecimal valor = new BigDecimal("1250000.5");

    assertThat(Redondeo.paraMostrar(valor, Moneda.COP)).isEqualTo("1250001");
    assertThat(Redondeo.paraMostrar(valor, Moneda.USD)).isEqualTo("1250000.50");
    assertThat(Redondeo.paraMostrar(valor, Moneda.VES)).isEqualTo("1250000.50");
  }

  @Test
  void divideConLaEscalaDeCalculo() {
    BigDecimal promedio = Redondeo.dividir(new BigDecimal("390"), new BigDecimal("20"));

    assertThat(promedio).isEqualTo("19.500000");
  }
}
