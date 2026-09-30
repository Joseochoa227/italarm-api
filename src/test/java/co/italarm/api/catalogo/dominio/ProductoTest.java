package co.italarm.api.catalogo.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductoTest {

  private final Categoria camaras = Categoria.crear("Cámaras");
  private final UnidadMedida unidad = UnidadMedida.crear("Unidad", "und", false);
  private final UnidadMedida metro = UnidadMedida.crear("Metro", "m", true);

  private static DatosProducto datos(String codigo, boolean serial, String stockMinimo) {
    return new DatosProducto(
        codigo,
        "  Cámara domo 2MP ",
        "Hikvision",
        "DS-2CE56D0T",
        serial,
        new BigDecimal("25.5"),
        new BigDecimal("32"),
        Moneda.USD,
        stockMinimo == null ? null : new BigDecimal(stockMinimo),
        "Cámara para interior");
  }

  @Test
  void alCrearseQuedaConStockCeroSinCostoYActivo() {
    Producto producto = Producto.crear(datos(" cam-d2 ", true, "5"), camaras, unidad);

    assertThat(producto.getCodigo()).isEqualTo("CAM-D2");
    assertThat(producto.getNombre()).isEqualTo("Cámara domo 2MP");
    assertThat(producto.getStock()).isEqualByComparingTo("0");
    assertThat(producto.getCostoActualUsd()).isNull();
    assertThat(producto.isActivo()).isTrue();
    assertThat(producto.isControlaSerial()).isTrue();
    assertThat(producto.precioInstalador()).isEqualTo(Dinero.de("25.5", Moneda.USD));
    assertThat(producto.precioClienteFinal()).isEqualTo(Dinero.de("32", Moneda.USD));
    assertThat(producto.getCategoria()).isSameAs(camaras);
    assertThat(producto.getUnidadMedida()).isSameAs(unidad);
  }

  @Test
  void quedaBajoElMinimoCuandoElStockEsMenor() {
    assertThat(Producto.crear(datos("A", false, "5"), camaras, unidad).estaBajoMinimo()).isTrue();
    assertThat(Producto.crear(datos("B", false, "0"), camaras, unidad).estaBajoMinimo()).isFalse();
    assertThat(Producto.crear(datos("C", false, null), camaras, unidad).estaBajoMinimo()).isFalse();
  }

  @Test
  void elStockMinimoRespetaLaUnidad() {
    assertThatThrownBy(() -> Producto.crear(datos("A", false, "2.5"), camaras, unidad))
        .isInstanceOf(CantidadInvalidaException.class);
    assertThat(Producto.crear(datos("A", false, "2.5"), camaras, metro).getStockMinimo())
        .isEqualByComparingTo("2.5");
  }

  @Test
  void sinMovimientosSePuedeCambiarElSerialYLaUnidad() {
    Producto producto = Producto.crear(datos("A", true, null), camaras, unidad);

    producto.actualizar(datos("A", false, null), camaras, metro, false);

    assertThat(producto.isControlaSerial()).isFalse();
    assertThat(producto.getUnidadMedida()).isSameAs(metro);
  }

  @Test
  void conMovimientosNoSePuedeCambiarElSerialNiLaUnidad() {
    Producto producto = Producto.crear(datos("A", true, null), camaras, unidad);

    assertThatThrownBy(() -> producto.actualizar(datos("A", false, null), camaras, unidad, true))
        .isInstanceOf(ProductoCambioNoPermitidoException.class)
        .hasMessageContaining("controla serial");
    assertThatThrownBy(() -> producto.actualizar(datos("A", true, null), camaras, metro, true))
        .isInstanceOf(ProductoCambioNoPermitidoException.class)
        .hasMessageContaining("unidad de medida");

    producto.actualizar(datos("B", true, "3"), camaras, unidad, true);
    assertThat(producto.getCodigo()).isEqualTo("B");
  }

  @Test
  void seActivaYDesactiva() {
    Producto producto = Producto.crear(datos("A", false, null), camaras, unidad);

    producto.desactivar();
    assertThat(producto.isActivo()).isFalse();

    producto.activar();
    assertThat(producto.isActivo()).isTrue();
  }

  @Test
  void alCambiarLaFotoDevuelveLaAnteriorParaEliminarla() {
    Producto producto = Producto.crear(datos("A", false, null), camaras, unidad);

    assertThat(producto.cambiarFoto("productos/1/foto-a.jpg")).isNull();
    assertThat(producto.cambiarFoto("productos/1/foto-b.jpg")).isEqualTo("productos/1/foto-a.jpg");
    assertThat(producto.getFotoClave()).isEqualTo("productos/1/foto-b.jpg");
    assertThat(producto.quitarFoto()).isEqualTo("productos/1/foto-b.jpg");
    assertThat(producto.getFotoClave()).isNull();
  }

  @Test
  void losTextosVaciosQuedanComoNulos() {
    DatosProducto sinOpcionales =
        new DatosProducto(
            "X",
            "Conector BNC",
            " ",
            "",
            false,
            BigDecimal.ONE,
            BigDecimal.TEN,
            Moneda.COP,
            null,
            "  ");

    Producto producto = Producto.crear(sinOpcionales, camaras, unidad);

    assertThat(producto.getMarca()).isNull();
    assertThat(producto.getModelo()).isNull();
    assertThat(producto.getDescripcion()).isNull();
    assertThat(producto.getMonedaPrecio()).isEqualTo(Moneda.COP);
  }
}
