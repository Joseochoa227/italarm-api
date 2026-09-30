package co.italarm.api.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfiguracionTest {

  private static DatosConfiguracion datos(
      int validez, int garantiaManoObra, int garantiaEquipos, String limite) {
    return new DatosConfiguracion(
        " ITALARM ",
        "Instalación de cámaras de seguridad",
        "900.123.456-7",
        "Cúcuta",
        "+57 300 123 4567",
        "contacto@italarm.com",
        new BigDecimal(limite),
        validez,
        garantiaManoObra,
        garantiaEquipos,
        "No cubre daños por descargas eléctricas.",
        "Documento no válido como factura.");
  }

  @Test
  void aplicaLosDatosDeLaEmpresaYLosValoresPorDefecto() {
    Configuracion configuracion = new Configuracion();

    configuracion.actualizar(datos(30, 2, 3, "7.5"));

    assertThat(configuracion.getEmpresaNombre()).isEqualTo("ITALARM");
    assertThat(configuracion.getEmpresaNit()).isEqualTo("900.123.456-7");
    assertThat(configuracion.getValidezCotizacionDias()).isEqualTo(30);
    assertThat(configuracion.getGarantiaManoObraMeses()).isEqualTo(2);
    assertThat(configuracion.getLimiteVariacionTasa()).isEqualByComparingTo("7.5");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 7, 10, 31})
  void laValidezSoloPuedeSer8Quince0Treinta(int validez) {
    assertThatThrownBy(() -> new Configuracion().actualizar(datos(validez, 3, 3, "5")))
        .isInstanceOf(ConfiguracionInvalidaException.class)
        .hasMessage("La validez de las cotizaciones debe ser de 8, 15 o 30 días.");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 4, 6, 12})
  void lasGarantiasSonDeUnoATresMeses(int meses) {
    assertThatThrownBy(() -> new Configuracion().actualizar(datos(15, meses, 3, "5")))
        .isInstanceOf(ConfiguracionInvalidaException.class)
        .hasMessage("La garantía de mano de obra debe ser de 1 a 3 meses.");
    assertThatThrownBy(() -> new Configuracion().actualizar(datos(15, 3, meses, "5")))
        .isInstanceOf(ConfiguracionInvalidaException.class)
        .hasMessage("La garantía de equipos debe ser de 1 a 3 meses.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "100.01"})
  void elLimiteDeVariacionEsMayorQueCeroYHasta100(String limite) {
    assertThatThrownBy(() -> new Configuracion().actualizar(datos(15, 3, 3, limite)))
        .isInstanceOf(ConfiguracionInvalidaException.class)
        .hasMessage("El límite de variación de tasas debe ser mayor que 0 % y máximo 100 %.");
  }

  @Test
  void alCambiarElLogoDevuelveElAnterior() {
    Configuracion configuracion = new Configuracion();

    assertThat(configuracion.cambiarLogo("configuracion/logo-a.png")).isNull();
    assertThat(configuracion.cambiarLogo("configuracion/logo-b.png"))
        .isEqualTo("configuracion/logo-a.png");
    assertThat(configuracion.quitarLogo()).isEqualTo("configuracion/logo-b.png");
    assertThat(configuracion.getEmpresaLogoClave()).isNull();
  }
}
