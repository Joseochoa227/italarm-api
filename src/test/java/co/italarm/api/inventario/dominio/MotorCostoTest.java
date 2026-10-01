package co.italarm.api.inventario.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Tasas;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Regla de costo de la sección 3.8 (RF-66, RN-02) con los casos de aceptación CP-01 a CP-07. */
class MotorCostoTest {

  private static final BigDecimal DIEZ = new BigDecimal("10");
  private static final BigDecimal VEINTE_USD = new BigDecimal("20");
  private static final Tasas TASAS = new Tasas(new BigDecimal("4000"), new BigDecimal("50"));

  private static ResultadoCosto comprar(
      BigDecimal stock, BigDecimal costo, String cantidad, String usd) {
    return MotorCosto.aplicarCompra(stock, costo, new BigDecimal(cantidad), new BigDecimal(usd));
  }

  @Test
  void cp01_compraConPrecioMayor_subeElCosto() {
    ResultadoCosto resultado = comprar(DIEZ, VEINTE_USD, "10", "25");

    assertThat(resultado.costoNuevo()).isEqualByComparingTo("25");
    assertThat(resultado.regla()).isEqualTo(ReglaCosto.SUBE);
    assertThat(resultado.costoAnterior()).isEqualByComparingTo("20");
  }

  @Test
  void cp02_compraConPrecioMenor_promedia() {
    ResultadoCosto resultado = comprar(DIEZ, VEINTE_USD, "10", "15");

    assertThat(resultado.costoNuevo()).isEqualByComparingTo("17.50");
    assertThat(resultado.regla()).isEqualTo(ReglaCosto.PROMEDIO);
  }

  @Test
  void cp03_compraConPrecioIgual_aplicaPromedio() {
    ResultadoCosto resultado = comprar(DIEZ, VEINTE_USD, "10", "20");

    assertThat(resultado.costoNuevo()).isEqualByComparingTo("20");
    assertThat(resultado.regla()).isEqualTo(ReglaCosto.PROMEDIO);
  }

  @Test
  void cp04_compraEnCopMasBarata_promediaEnUsd() {
    BigDecimal usd = TASAS.aUsd(new BigDecimal("76000"), Moneda.COP);

    ResultadoCosto resultado = MotorCosto.aplicarCompra(DIEZ, VEINTE_USD, DIEZ, usd);

    assertThat(usd).isEqualByComparingTo("19");
    assertThat(resultado.costoNuevo()).isEqualByComparingTo("19.50");
    assertThat(resultado.regla()).isEqualTo(ReglaCosto.PROMEDIO);
  }

  @Test
  void cp05_compraEnCopMasCara_sube() {
    BigDecimal usd = TASAS.aUsd(new BigDecimal("88000"), Moneda.COP);

    ResultadoCosto resultado = MotorCosto.aplicarCompra(DIEZ, VEINTE_USD, DIEZ, usd);

    assertThat(resultado.costoNuevo()).isEqualByComparingTo("22");
    assertThat(resultado.regla()).isEqualTo(ReglaCosto.SUBE);
  }

  @Test
  void cp06_compraEnVes_convierteConLaTasaDelBolivar() {
    BigDecimal usd = TASAS.aUsd(new BigDecimal("950"), Moneda.VES);

    ResultadoCosto resultado = MotorCosto.aplicarCompra(DIEZ, VEINTE_USD, DIEZ, usd);

    assertThat(usd).isEqualByComparingTo("19");
    assertThat(resultado.costoNuevo()).isEqualByComparingTo("19.50");
  }

  @Test
  void cp07_productoSinStock_tomaElCostoDeLaFactura() {
    ResultadoCosto resultado = comprar(BigDecimal.ZERO, VEINTE_USD, "5", "18");

    assertThat(resultado.costoNuevo()).isEqualByComparingTo("18");
    assertThat(resultado.regla()).isEqualTo(ReglaCosto.SIN_STOCK);
    assertThat(resultado.costoAnterior()).isEqualByComparingTo("20");
  }

  @Test
  void laPrimeraCompraNoTieneCostoAnterior() {
    ResultadoCosto resultado = MotorCosto.aplicarCompra(BigDecimal.ZERO, null, DIEZ, VEINTE_USD);

    assertThat(resultado.costoAnterior()).isNull();
    assertThat(resultado.costoNuevo()).isEqualByComparingTo("20");
    assertThat(resultado.regla()).isEqualTo(ReglaCosto.SIN_STOCK);
  }

  @Test
  void elPromedioSeGuardaConCuatroDecimales() {
    // (3 × 10 + 1 × 9) ÷ 4 = 9,75; (3 × 10 + 2 × 9.99) ÷ 5 = 9,996
    assertThat(comprar(new BigDecimal("3"), new BigDecimal("10"), "2", "9.99").costoNuevo())
        .isEqualTo("9.9960");
    assertThat(comprar(new BigDecimal("3"), new BigDecimal("10"), "3", "9.3333333").costoNuevo())
        .isEqualTo("9.6667");
  }

  @Test
  void funcionaConMetrosConDecimales() {
    ResultadoCosto resultado =
        comprar(new BigDecimal("107.5"), new BigDecimal("0.40"), "305", "0.35");

    // (107,5 × 0,40 + 305 × 0,35) ÷ 412,5 = 0,363030…
    assertThat(resultado.costoNuevo()).isEqualTo("0.3630");
  }
}
