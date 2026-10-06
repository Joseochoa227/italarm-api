package co.italarm.api.ventas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDescuento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VentaTest {

  private static final LocalDate HOY = LocalDate.of(2026, 10, 15);

  private static Venta venta(Set<Moneda> monedas) {
    LineaVenta linea =
        new LineaVenta(
            1L,
            "CAM-1",
            "Cámara domo",
            "und",
            new BigDecimal("2"),
            new BigDecimal("50"),
            new BigDecimal("55"),
            new BigDecimal("30"));
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            List.of(
                new CalculoDocumento.Linea(
                    new BigDecimal("2"), new BigDecimal("50"), new BigDecimal("30"))),
            Descuento.de(TipoDescuento.VALOR, new BigDecimal("7")),
            Moneda.USD,
            new Tasas(new BigDecimal("4000"), null));
    return Venta.registrar(
        12,
        HOY,
        new DatosClienteVenta(5L, "INSTALADOR", "Juan", "CC 123", "+573001234567", null, null),
        Moneda.USD,
        new Venta.TasasVenta(new BigDecimal("4000"), HOY, null, null),
        Descuento.de(TipoDescuento.VALOR, new BigDecimal("7")),
        resumen,
        "  Entrega en obra ",
        monedas,
        List.of(linea));
  }

  @Test
  void registrar_guardaTotalesClienteYLineas() {
    Venta venta = venta(Set.of(Moneda.COP, Moneda.USD));

    assertThat(venta.consecutivo()).isEqualTo("V-0012");
    assertThat(venta.getTotal()).isEqualByComparingTo("93");
    assertThat(venta.getUtilidad()).isEqualByComparingTo("33");
    assertThat(venta.getCliente().nombre()).isEqualTo("Juan");
    assertThat(venta.getObservaciones()).isEqualTo("Entrega en obra");
    assertThat(venta.getMonedasComprobante()).containsExactly(Moneda.COP);
    assertThat(venta.getLineas()).hasSize(1);
    assertThat(venta.getLineas().get(0).getSubtotal()).isEqualByComparingTo("100");
    assertThat(venta.estaAnulada()).isFalse();
  }

  @Test
  void cambiarDescripcion_aceptaQuitarLasMonedasAdicionales() {
    Venta venta = venta(Set.of(Moneda.VES));

    venta.cambiarDescripcion(null, null);

    assertThat(venta.getObservaciones()).isNull();
    assertThat(venta.getMonedasComprobante()).isEmpty();
  }

  @Test
  void anular_registraMotivoYNoSePuedeDosVeces() {
    Venta venta = venta(Set.of());
    Instant ahora = Instant.parse("2026-10-15T15:00:00Z");

    venta.anular(" Cliente devolvió ", 2L, ahora);

    assertThat(venta.estaAnulada()).isTrue();
    assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
    assertThat(venta.getMotivoAnulacion()).isEqualTo("Cliente devolvió");
    assertThat(venta.getAnuladaPor()).isEqualTo(2L);
    assertThat(venta.getAnuladaEn()).isEqualTo(ahora);
    assertThatThrownBy(() -> venta.anular("otra", 2L, ahora))
        .isInstanceOf(VentaYaAnuladaException.class);
  }

  @Test
  void sinLineasOConPrecioNegativo_seRechaza() {
    assertThatThrownBy(
            () ->
                Venta.registrar(
                    1,
                    HOY,
                    new DatosClienteVenta(1L, "CLIENTE_FINAL", "Ana", null, null, null, null),
                    Moneda.USD,
                    new Venta.TasasVenta(null, null, null, null),
                    Descuento.ninguno(),
                    null,
                    null,
                    null,
                    List.of()))
        .isInstanceOf(VentaInvalidaException.class);
    assertThatThrownBy(
            () ->
                new LineaVenta(
                    1L,
                    "X",
                    "X",
                    "und",
                    BigDecimal.ONE,
                    new BigDecimal("-1"),
                    BigDecimal.ONE,
                    null))
        .isInstanceOf(co.italarm.api.shared.dominio.PrecioInvalidoException.class);
  }
}
