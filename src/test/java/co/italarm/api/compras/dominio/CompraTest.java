package co.italarm.api.compras.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompraTest {

  private static Compra compraEnCop() {
    return Compra.registrar(
        7,
        LocalDate.of(2026, 10, 1),
        1L,
        "  FAC-1 ",
        Moneda.COP,
        new BigDecimal("4000"),
        LocalDate.of(2026, 10, 1),
        null,
        null,
        List.of(
            new LineaCompra(
                1L, new BigDecimal("10"), new BigDecimal("76000"), new BigDecimal("19")),
            new LineaCompra(
                2L, new BigDecimal("2.5"), new BigDecimal("1000"), new BigDecimal("0.25"))));
  }

  @Test
  void calculaElTotalEnLaMonedaDeLaFacturaYEnUsd() {
    Compra compra = compraEnCop();

    assertThat(compra.getTotal()).isEqualByComparingTo("762500");
    assertThat(compra.getTotalUsd()).isEqualByComparingTo("190.625");
    assertThat(compra.getNumeroFactura()).isEqualTo("FAC-1");
    assertThat(compra.consecutivo()).isEqualTo("C-0007");
    assertThat(compra.getLineas()).hasSize(2);
    assertThat(compra.estaAnulada()).isFalse();
  }

  @Test
  void anular_guardaMotivoUsuarioYFecha_yNoSePuedeDosVeces() {
    Compra compra = compraEnCop();
    Instant ahora = Instant.parse("2026-10-01T15:00:00Z");

    compra.anular(" Factura errada ", 1L, ahora);

    assertThat(compra.estaAnulada()).isTrue();
    assertThat(compra.getEstado()).isEqualTo(EstadoCompra.ANULADA);
    assertThat(compra.getMotivoAnulacion()).isEqualTo("Factura errada");
    assertThat(compra.getAnuladaPor()).isEqualTo(1L);
    assertThat(compra.getAnuladaEn()).isEqualTo(ahora);
    assertThatThrownBy(() -> compra.anular("otra", 1L, ahora))
        .isInstanceOf(CompraYaAnuladaException.class);
  }

  @Test
  void cambiarFactura_devuelveLaAnterior() {
    Compra compra = compraEnCop();

    assertThat(compra.cambiarFactura("compras/1/a.pdf")).isNull();
    assertThat(compra.cambiarFactura("compras/1/b.jpg")).isEqualTo("compras/1/a.pdf");
    assertThat(compra.getFacturaClave()).isEqualTo("compras/1/b.jpg");
  }

  @Test
  void elCambioDeCostoDeUnaLineaSeRegistraUnaSolaVez() {
    LineaCompra linea =
        new LineaCompra(1L, BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("10.1234567"));
    assertThat(linea.getCostoUnitarioUsd()).isEqualByComparingTo("10.123457");

    linea.registrarCambioCosto(new BigDecimal("9"), new BigDecimal("10"), "SUBE");

    assertThat(linea.getRegla()).isEqualTo("SUBE");
    assertThatThrownBy(() -> linea.registrarCambioCosto(null, BigDecimal.TEN, "SIN_STOCK"))
        .isInstanceOf(IllegalStateException.class);
  }
}
