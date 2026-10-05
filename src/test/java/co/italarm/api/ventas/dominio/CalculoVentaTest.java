package co.italarm.api.ventas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Tasas;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculoVentaTest {

  private static final Tasas TASAS = new Tasas(new BigDecimal("4000"), new BigDecimal("50"));

  private static CalculoVenta.Linea linea(String cantidad, String precio, String costoUsd) {
    return new CalculoVenta.Linea(
        new BigDecimal(cantidad), new BigDecimal(precio), new BigDecimal(costoUsd));
  }

  @Test
  void cp27_ventaDe100ConDescuentoDe7_total93YUtilidadSobre93() {
    ResumenVenta resumen =
        CalculoVenta.calcular(
            List.of(linea("2", "50", "30")),
            Descuento.de(TipoDescuento.VALOR, new BigDecimal("7")),
            Moneda.USD,
            TASAS);

    assertThat(resumen.subtotal()).isEqualByComparingTo("100");
    assertThat(resumen.descuento()).isEqualByComparingTo("7");
    assertThat(resumen.total()).isEqualByComparingTo("93");
    assertThat(resumen.costo()).isEqualByComparingTo("60");
    assertThat(resumen.utilidad()).isEqualByComparingTo("33");
    assertThat(resumen.porcentajeUtilidad()).isEqualByComparingTo("35.48");
    assertThat(resumen.totalUsd()).isEqualByComparingTo("93");
    assertThat(resumen.costoUsd()).isEqualByComparingTo("60");
  }

  @Test
  void ventaEnCop_conviertePrimeroElCostoEnUsdConLaTrm() {
    ResumenVenta resumen =
        CalculoVenta.calcular(
            List.of(linea("1", "100000", "19.50")),
            Descuento.de(TipoDescuento.PORCENTAJE, new BigDecimal("10")),
            Moneda.COP,
            TASAS);

    assertThat(resumen.descuento()).isEqualByComparingTo("10000");
    assertThat(resumen.total()).isEqualByComparingTo("90000");
    assertThat(resumen.costo()).isEqualByComparingTo("78000");
    assertThat(resumen.utilidad()).isEqualByComparingTo("12000");
    assertThat(resumen.totalUsd()).isEqualByComparingTo("22.5");
  }

  @Test
  void sinDescuento_yPrecioPorDebajoDelCosto_daUtilidadNegativa() {
    ResumenVenta resumen =
        CalculoVenta.calcular(
            List.of(linea("3", "10", "12"), linea("1.5", "2", "1")),
            Descuento.ninguno(),
            Moneda.USD,
            TASAS);

    assertThat(resumen.subtotal()).isEqualByComparingTo("33");
    assertThat(resumen.descuento()).isEqualByComparingTo("0");
    assertThat(resumen.costo()).isEqualByComparingTo("37.5");
    assertThat(resumen.utilidad()).isEqualByComparingTo("-4.5");
  }

  @Test
  void totalCero_noTienePorcentajeDeUtilidad() {
    ResumenVenta resumen =
        CalculoVenta.calcular(
            List.of(linea("1", "0", "5")), Descuento.ninguno(), Moneda.USD, TASAS);

    assertThat(resumen.total()).isEqualByComparingTo("0");
    assertThat(resumen.porcentajeUtilidad()).isNull();
  }

  @Test
  void descuentoMayorQueElSubtotal_seRechaza() {
    assertThatThrownBy(
            () ->
                CalculoVenta.calcular(
                    List.of(linea("1", "10", "5")),
                    Descuento.de(TipoDescuento.VALOR, new BigDecimal("11")),
                    Moneda.USD,
                    TASAS))
        .isInstanceOf(DescuentoInvalidoException.class);
  }

  @Test
  void descuentoNegativoOPorcentajeMayorA100_seRechaza() {
    assertThatThrownBy(() -> Descuento.de(TipoDescuento.VALOR, new BigDecimal("-1")))
        .isInstanceOf(DescuentoInvalidoException.class);
    assertThatThrownBy(() -> Descuento.de(TipoDescuento.PORCENTAJE, new BigDecimal("100.01")))
        .isInstanceOf(DescuentoInvalidoException.class);
    assertThat(Descuento.de(null, null)).isEqualTo(Descuento.ninguno());
  }

  @Test
  void precioNegativo_seRechaza() {
    assertThatThrownBy(
            () ->
                CalculoVenta.calcular(
                    List.of(linea("1", "-1", "5")), Descuento.ninguno(), Moneda.USD, TASAS))
        .isInstanceOf(VentaInvalidaException.class)
        .extracting("codigo")
        .isEqualTo(VentaInvalidaException.PRECIO_INVALIDO);
  }
}
