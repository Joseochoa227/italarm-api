package co.italarm.api.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import co.italarm.api.configuracion.dominio.Configuracion;
import co.italarm.api.configuracion.infraestructura.ConfiguracionRepositorio;
import co.italarm.api.soporte.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConfiguracionIntegracionTest extends PruebaIntegracion {

  @Autowired ConfiguracionRepositorio repositorio;

  @Test
  void traeLosValoresPorDefectoDelDocumento() {
    Configuracion configuracion = repositorio.findById(Configuracion.ID_UNICO).orElseThrow();

    assertThat(configuracion.getId()).isEqualTo(1);
    assertThat(configuracion.getEmpresaNombre()).isEqualTo("ITALARM");
    assertThat(configuracion.getEmpresaLema()).isEqualTo("Instalación de cámaras de seguridad");
    assertThat(configuracion.getLimiteVariacionTasa()).isEqualByComparingTo("5");
    assertThat(configuracion.getValidezCotizacionDias()).isEqualTo(15);
    assertThat(configuracion.getGarantiaManoObraMeses()).isEqualTo(3);
    assertThat(configuracion.getGarantiaEquiposMeses()).isEqualTo(3);
    assertThat(configuracion.getCondicionesGarantia())
        .isEqualTo("No cubre daños por descargas eléctricas, humedad o manipulación de terceros.");
    assertThat(configuracion.getPiePdf()).contains("Documento no válido como factura");
    assertThat(configuracion.getVersion()).isZero();
    assertThat(configuracion.getCreatedAt()).isNotNull();
    assertThat(configuracion.getUpdatedAt()).isNotNull();
    assertThat(configuracion.getCreatedBy()).isNull();
    assertThat(configuracion.getUpdatedBy()).isNull();
  }

  @Test
  void losDatosDeContactoQuedanPendientes() {
    Configuracion configuracion = repositorio.findById(Configuracion.ID_UNICO).orElseThrow();

    assertThat(configuracion.getEmpresaNit()).isNull();
    assertThat(configuracion.getEmpresaCiudad()).isNull();
    assertThat(configuracion.getEmpresaTelefono()).isNull();
    assertThat(configuracion.getEmpresaCorreo()).isNull();
    assertThat(configuracion.getEmpresaLogoClave()).isNull();
  }

  @Test
  void laBaseDeDatosImpideUnaSegundaFila() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into configuracion (id, empresa_nombre, limite_variacion_tasa,"
                        + " validez_cotizacion_dias, garantia_mano_obra_meses,"
                        + " garantia_equipos_meses, condiciones_garantia, pie_pdf)"
                        + " values (2, 'X', 5, 15, 3, 3, 'c', 'p')"))
        .hasMessageContaining("ck_configuracion_fila_unica");
  }

  @Test
  void laBaseDeDatosLimitaLaGarantiaATresMeses() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> jdbc.update("update configuracion set garantia_mano_obra_meses = 6"))
        .hasMessageContaining("ck_configuracion_garantia_mano_obra");
  }
}
