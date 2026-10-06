package co.italarm.api.instalaciones.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.CopiaCliente;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InstalacionTest {

  private static final LocalDate FECHA = LocalDate.of(2026, 10, 1);
  private static final CopiaCliente CLIENTE =
      new CopiaCliente(5L, "CLIENTE_FINAL", "Ana", "CC 1", "+573001", "Calle 1", "Cúcuta");

  private static Instalacion instalacion(
      List<LineaInstalacion> lineas, String manoDeObra, int meses, String direccion) {
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            lineas.stream()
                .map(
                    l ->
                        new CalculoDocumento.Linea(
                            l.getCantidad(), l.getPrecioUnitario(), l.getCostoUnitarioUsd()))
                .toList(),
            new BigDecimal(manoDeObra),
            Descuento.ninguno(),
            Moneda.USD,
            new Tasas(null, null));
    return Instalacion.registrar(
        3,
        FECHA,
        CLIENTE,
        new Instalacion.Descripcion(
            direccion, " Cámaras en bodega ", List.of(1L, 2L), null, null, Set.of(Moneda.COP)),
        Moneda.USD,
        new Instalacion.TasasInstalacion(null, null, null, null),
        Descuento.ninguno(),
        resumen,
        new Instalacion.Garantias(meses, 3, "No cubre descargas eléctricas."),
        lineas);
  }

  private static LineaInstalacion camara() {
    return new LineaInstalacion(
        1L,
        "CAM-1",
        "Cámara",
        "und",
        new BigDecimal("2"),
        new BigDecimal("50"),
        new BigDecimal("50"),
        new BigDecimal("30"));
  }

  @Test
  void registrar_calculaGarantiasYTomaLaDireccionDelCliente() {
    Instalacion instalacion = instalacion(List.of(camara()), "40", 2, null);

    assertThat(instalacion.consecutivo()).isEqualTo("I-0003");
    assertThat(instalacion.getDireccion()).isEqualTo("Calle 1");
    assertThat(instalacion.getDescripcion()).isEqualTo("Cámaras en bodega");
    assertThat(instalacion.getVenceManoObra()).isEqualTo(LocalDate.of(2026, 12, 1));
    assertThat(instalacion.getVenceEquipos()).isEqualTo(LocalDate.of(2027, 1, 1));
    assertThat(instalacion.getCondicionesGarantia()).isEqualTo("No cubre descargas eléctricas.");
    assertThat(instalacion.getResumen().total()).isEqualByComparingTo("140");
    assertThat(instalacion.getTecnicos()).containsExactlyInAnyOrder(1L, 2L);
    assertThat(instalacion.getMonedasComprobante()).containsExactly(Moneda.COP);
    assertThat(instalacion.getCliente().nombre()).isEqualTo("Ana");
    assertThat(instalacion.getLineas()).hasSize(1);
  }

  @Test
  void soloManoDeObra_seAcepta_yVacia_noSeAcepta() {
    assertThat(instalacion(List.of(), "80", 3, "Calle 2").getResumen().total())
        .isEqualByComparingTo("80");
    assertThatThrownBy(() -> instalacion(List.of(), "0", 3, "Calle 2"))
        .isInstanceOf(InstalacionInvalidaException.class)
        .extracting("codigo")
        .isEqualTo(InstalacionInvalidaException.VACIA);
  }

  @Test
  void garantiaFueraDe1a3Meses_seRechaza() {
    assertThatThrownBy(() -> instalacion(List.of(camara()), "0", 4, "Calle 2"))
        .extracting("codigo")
        .isEqualTo(InstalacionInvalidaException.GARANTIA_INVALIDA);
    assertThatThrownBy(() -> instalacion(List.of(camara()), "0", 0, "Calle 2"))
        .extracting("codigo")
        .isEqualTo(InstalacionInvalidaException.GARANTIA_INVALIDA);
  }

  @Test
  void reglas_deFechaYTecnicos() {
    ReglasInstalacion.validarFecha(FECHA, FECHA);
    assertThatThrownBy(() -> ReglasInstalacion.validarFecha(FECHA.plusDays(1), FECHA))
        .extracting("codigo")
        .isEqualTo(InstalacionInvalidaException.FECHA_FUTURA);
    assertThatThrownBy(() -> ReglasInstalacion.validarTecnicos(List.of()))
        .extracting("codigo")
        .isEqualTo(InstalacionInvalidaException.SIN_TECNICOS);
  }

  @Test
  void editarDescripcion_conservaLoQueNoSeEnvia() {
    Instalacion instalacion = instalacion(List.of(camara()), "40", 3, "Calle 9");

    instalacion.cambiarDescripcion(
        new Instalacion.Descripcion(
            null, null, List.of(2L), "Sin cobertura por agua.", "Ok", null));

    assertThat(instalacion.getDireccion()).isEqualTo("Calle 9");
    assertThat(instalacion.getDescripcion()).isEqualTo("Cámaras en bodega");
    assertThat(instalacion.getTecnicos()).containsExactly(2L);
    assertThat(instalacion.getCondicionesGarantia()).isEqualTo("Sin cobertura por agua.");
    assertThat(instalacion.getObservaciones()).isEqualTo("Ok");
    assertThat(instalacion.getMonedasComprobante()).isEmpty();
    assertThatThrownBy(
            () ->
                instalacion.cambiarDescripcion(
                    new Instalacion.Descripcion(null, null, List.of(), null, null, null)))
        .extracting("codigo")
        .isEqualTo(InstalacionInvalidaException.SIN_TECNICOS);
  }

  @Test
  void anular_unaSolaVez() {
    Instalacion instalacion = instalacion(List.of(camara()), "40", 3, "Calle 9");
    Instant ahora = Instant.parse("2026-10-02T15:00:00Z");

    instalacion.anular(" Error ", 1L, ahora);

    assertThat(instalacion.estaAnulada()).isTrue();
    assertThat(instalacion.getMotivoAnulacion()).isEqualTo("Error");
    assertThat(instalacion.getAnuladaEn()).isEqualTo(ahora);
    assertThatThrownBy(() -> instalacion.anular("x", 1L, ahora))
        .isInstanceOf(InstalacionYaAnuladaException.class);
  }

  @Test
  void fotos_maximo30PorGrupo() {
    assertThat(FotoInstalacion.agregar(1L, GrupoFoto.ANTES, "a.jpg", 29).getGrupo())
        .isEqualTo(GrupoFoto.ANTES);
    assertThatThrownBy(() -> FotoInstalacion.agregar(1L, GrupoFoto.DESPUES, "b.jpg", 30))
        .isInstanceOf(FotosMaximasException.class)
        .hasMessageContaining("despues");
  }
}
