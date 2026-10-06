package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculoDocumentoTest {

  private static final Tasas TASAS = new Tasas(new BigDecimal("4000"), new BigDecimal("50"));

  private static CalculoDocumento.Linea linea(String cantidad, String precio, String costoUsd) {
    return new CalculoDocumento.Linea(
        new BigDecimal(cantidad), new BigDecimal(precio), new BigDecimal(costoUsd));
  }

  @Test
  void cp27_ventaDe100ConDescuentoDe7_total93YUtilidadSobre93() {
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
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
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
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
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
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
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            List.of(linea("1", "0", "5")), Descuento.ninguno(), Moneda.USD, TASAS);

    assertThat(resumen.total()).isEqualByComparingTo("0");
    assertThat(resumen.porcentajeUtilidad()).isNull();
  }

  @Test
  void descuentoMayorQueElSubtotal_seRechaza() {
    assertThatThrownBy(
            () ->
                CalculoDocumento.calcular(
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
                CalculoDocumento.calcular(
                    List.of(linea("1", "-1", "5")), Descuento.ninguno(), Moneda.USD, TASAS))
        .isInstanceOf(PrecioInvalidoException.class)
        .extracting("codigo")
        .isEqualTo("PRECIO_INVALIDO");
  }

  @Test
  void instalacion_sumaLaManoDeObraALaUtilidad() {
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            List.of(linea("2", "50", "30")),
            new BigDecimal("50"),
            Descuento.de(TipoDescuento.VALOR, new BigDecimal("10")),
            Moneda.USD,
            TASAS);

    assertThat(resumen.material()).isEqualByComparingTo("100");
    assertThat(resumen.manoDeObra()).isEqualByComparingTo("50");
    assertThat(resumen.subtotal()).isEqualByComparingTo("150");
    assertThat(resumen.total()).isEqualByComparingTo("140");
    assertThat(resumen.costo()).isEqualByComparingTo("60");
    assertThat(resumen.utilidad()).isEqualByComparingTo("80");
  }

  @Test
  void soloManoDeObra_yManoDeObraNegativaSeRechaza() {
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            List.of(), new BigDecimal("80000"), Descuento.ninguno(), Moneda.COP, TASAS);
    assertThat(resumen.total()).isEqualByComparingTo("80000");
    assertThat(resumen.costo()).isEqualByComparingTo("0");
    assertThat(resumen.totalUsd()).isEqualByComparingTo("20");

    assertThatThrownBy(
            () ->
                CalculoDocumento.calcular(
                    List.of(), new BigDecimal("-1"), Descuento.ninguno(), Moneda.USD, TASAS))
        .isInstanceOf(PrecioInvalidoException.class);
  }
}
