package co.italarm.api.cotizaciones.dominio;

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
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CotizacionTest {

  private static final LocalDate HOY = LocalDate.of(2026, 10, 1);
  private static final Instant AHORA = Instant.parse("2026-10-01T15:00:00Z");
  private static final CopiaCliente CLIENTE =
      new CopiaCliente(5L, "CLIENTE_FINAL", "Ana", "CC 1", "+573001", "Calle 1", "Cúcuta");

  private static LineaCotizacion linea(long productoId, String cantidad, String precio) {
    return new LineaCotizacion(
        productoId,
        "P-" + productoId,
        "Producto " + productoId,
        "und",
        new BigDecimal(cantidad),
        new BigDecimal(precio),
        new BigDecimal(precio),
        new BigDecimal("20"));
  }

  private static Cotizacion.Contenido contenido(
      TipoCotizacion tipo, LocalDate fecha, String manoDeObra, List<LineaCotizacion> lineas) {
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
    return new Cotizacion.Contenido(
        tipo,
        fecha,
        15,
        CLIENTE,
        Moneda.USD,
        new Cotizacion.TasasCotizacion(null, null, null, null),
        "  Instalar 4 cámaras  ",
        Descuento.ninguno(),
        resumen,
        null,
        Set.of(Moneda.USD, Moneda.COP),
        lineas);
  }

  private static Cotizacion instalacion() {
    return Cotizacion.registrar(
        1, contenido(TipoCotizacion.INSTALACION, HOY, "50", List.of(linea(1, "4", "40"))));
  }

  private static Cotizacion en(EstadoCotizacion estado) {
    Cotizacion cotizacion = instalacion();
    switch (estado) {
      case BORRADOR -> {}
      case EN_EVALUACION -> cotizacion.enviar(AHORA);
      case APROBADA -> cotizacion.aprobar(AHORA);
      case CONVERTIDA -> {
        cotizacion.aprobar(AHORA);
        cotizacion.convertir(TipoCotizacion.INSTALACION, 5L, 9L, "I-0009", AHORA);
      }
      case RECHAZADA -> cotizacion.rechazar(null, null, AHORA);
      case VENCIDA -> cotizacion.vencerSiCorresponde(HOY.plusDays(16));
      default -> throw new IllegalArgumentException();
    }
    assertThat(cotizacion.getEstado()).isEqualTo(estado);
    return cotizacion;
  }

  private static void noPermitida(EstadoCotizacion estado, Consumer<Cotizacion> accion) {
    Cotizacion cotizacion = en(estado);
    assertThatThrownBy(() -> accion.accept(cotizacion))
        .isInstanceOf(TransicionNoPermitidaException.class)
        .extracting("codigo")
        .isEqualTo(TransicionNoPermitidaException.CODIGO);
    assertThat(cotizacion.getEstado()).isEqualTo(estado);
  }

  @Test
  void nuevaCotizacion_quedaEnBorradorConVencimientoCalculado() {
    Cotizacion cotizacion = instalacion();

    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.BORRADOR);
    assertThat(cotizacion.consecutivo()).isEqualTo("COT-0001");
    assertThat(cotizacion.getVence()).isEqualTo(LocalDate.of(2026, 10, 16));
    assertThat(cotizacion.getDescripcion()).isEqualTo("Instalar 4 cámaras");
    assertThat(cotizacion.getMonedasComprobante()).containsExactly(Moneda.COP);
    assertThat(cotizacion.getResumen().total()).isEqualByComparingTo("210");
    assertThat(cotizacion.getResumen().utilidad()).isEqualByComparingTo("130");
  }

  @Test
  void cotizacionDeVenta_noGuardaDescripcion() {
    Cotizacion venta =
        Cotizacion.registrar(
            2, contenido(TipoCotizacion.VENTA, HOY, "0", List.of(linea(1, "2", "40"))));
    assertThat(venta.getDescripcion()).isNull();
  }

  @Test
  void flujoCompleto_borradorEvaluacionAprobadaConvertida() {
    Cotizacion cotizacion = instalacion();
    cotizacion.enviar(AHORA);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.EN_EVALUACION);
    assertThat(cotizacion.getEnviadaEn()).isEqualTo(AHORA);

    cotizacion.aprobar(AHORA);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.APROBADA);

    cotizacion.convertir(TipoCotizacion.INSTALACION, 5L, 9L, "I-0009", AHORA);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.CONVERTIDA);
    assertThat(cotizacion.getDocumentoId()).isEqualTo(9L);
    assertThat(cotizacion.getDocumentoNumero()).isEqualTo("I-0009");
    assertThat(cotizacion.getConvertidaEn()).isEqualTo(AHORA);
  }

  @Test
  void reenviarUnaEnEvaluacion_soloActualizaLaFechaDeEnvio() {
    Cotizacion cotizacion = en(EstadoCotizacion.EN_EVALUACION);
    Instant despues = AHORA.plusSeconds(3600);
    cotizacion.enviar(despues);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.EN_EVALUACION);
    assertThat(cotizacion.getEnviadaEn()).isEqualTo(despues);
  }

  @Test
  void seApruebaSinHaberlaEnviado() {
    Cotizacion cotizacion = en(EstadoCotizacion.BORRADOR);
    cotizacion.aprobar(AHORA);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.APROBADA);
  }

  @Test
  void rechazarConMotivoYDetalle() {
    Cotizacion cotizacion = en(EstadoCotizacion.APROBADA);
    cotizacion.rechazar(MotivoRechazo.PRECIO, "  Consiguió otro más barato ", AHORA);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.RECHAZADA);
    assertThat(cotizacion.getMotivoRechazo()).isEqualTo(MotivoRechazo.PRECIO);
    assertThat(cotizacion.getDetalleRechazo()).isEqualTo("Consiguió otro más barato");
    assertThat(cotizacion.getRechazadaEn()).isEqualTo(AHORA);
  }

  @Test
  void transicionesNoPermitidas() {
    for (EstadoCotizacion estado :
        List.of(
            EstadoCotizacion.APROBADA,
            EstadoCotizacion.CONVERTIDA,
            EstadoCotizacion.RECHAZADA,
            EstadoCotizacion.VENCIDA)) {
      noPermitida(estado, c -> c.enviar(AHORA));
      noPermitida(estado, c -> c.aprobar(AHORA));
      noPermitida(
          estado, c -> c.editar(contenido(TipoCotizacion.INSTALACION, HOY, "10", List.of())));
    }
    for (EstadoCotizacion estado :
        List.of(
            EstadoCotizacion.CONVERTIDA, EstadoCotizacion.RECHAZADA, EstadoCotizacion.VENCIDA)) {
      noPermitida(estado, c -> c.rechazar(MotivoRechazo.OTRO, null, AHORA));
    }
  }

  @Test
  void cp21_validezDe15Dias_venceDesdeElDia17() {
    Cotizacion cotizacion = en(EstadoCotizacion.EN_EVALUACION);

    assertThat(cotizacion.vencerSiCorresponde(LocalDate.of(2026, 10, 16))).isFalse();
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.EN_EVALUACION);

    assertThat(cotizacion.vencerSiCorresponde(LocalDate.of(2026, 10, 17))).isTrue();
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.VENCIDA);
    assertThat(cotizacion.getVencidaEl()).isEqualTo(LocalDate.of(2026, 10, 17));
  }

  @Test
  void borradorTambienVence_peroAprobadaYRechazadaNo() {
    LocalDate despues = HOY.plusDays(40);
    assertThat(en(EstadoCotizacion.BORRADOR).vencerSiCorresponde(despues)).isTrue();
    for (EstadoCotizacion estado :
        List.of(
            EstadoCotizacion.APROBADA,
            EstadoCotizacion.CONVERTIDA,
            EstadoCotizacion.RECHAZADA,
            EstadoCotizacion.VENCIDA)) {
      Cotizacion cotizacion = en(estado);
      assertThat(cotizacion.vencerSiCorresponde(despues)).isFalse();
      assertThat(cotizacion.getEstado()).isEqualTo(estado);
    }
  }

  @Test
  void editarEnBorrador_reemplazaSinNuevaVersion() {
    Cotizacion cotizacion = en(EstadoCotizacion.BORRADOR);
    LocalDate despues = HOY.plusDays(2);

    VersionCotizacion anterior =
        cotizacion.editar(
            contenido(
                TipoCotizacion.INSTALACION,
                despues,
                "50",
                List.of(linea(1, "6", "40"), linea(2, "100", "1"))));

    assertThat(anterior).isNull();
    assertThat(cotizacion.getNumeroVersion()).isEqualTo(1);
    assertThat(cotizacion.getFecha()).isEqualTo(despues);
    assertThat(cotizacion.getVence()).isEqualTo(despues.plusDays(15));
    assertThat(cotizacion.getLineas())
        .extracting(LineaCotizacion::getProductoId, l -> l.getCantidad().intValue())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(1L, 6),
            org.assertj.core.groups.Tuple.tuple(2L, 100));
  }

  @Test
  void editarEnEvaluacion_guardaNuevaVersionYCopiaDeLaAnterior() {
    Cotizacion cotizacion = en(EstadoCotizacion.EN_EVALUACION);

    VersionCotizacion anterior =
        cotizacion.editar(
            contenido(
                TipoCotizacion.INSTALACION, HOY.plusDays(3), "60", List.of(linea(2, "1", "5"))));

    assertThat(cotizacion.getNumeroVersion()).isEqualTo(2);
    assertThat(cotizacion.consecutivo()).isEqualTo("COT-0001 v2");
    assertThat(cotizacion.consecutivoBase()).isEqualTo("COT-0001");
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.EN_EVALUACION);
    assertThat(cotizacion.getLineas())
        .extracting(LineaCotizacion::getProductoId)
        .containsExactly(2L);
    assertThat(anterior.getNumeroVersion()).isEqualTo(1);
    assertThat(anterior.getContenido().total()).isEqualByComparingTo("210");
    assertThat(anterior.getContenido().vence()).isEqualTo(LocalDate.of(2026, 10, 16));
    assertThat(anterior.getContenido().lineas())
        .extracting(ContenidoVersion.Linea::productoId)
        .containsExactly(1L);
  }

  @Test
  void convertir_exigeAprobadaMismoTipoYMismoCliente() {
    assertThatThrownBy(
            () ->
                en(EstadoCotizacion.EN_EVALUACION)
                    .validarConversion(TipoCotizacion.INSTALACION, 5L))
        .isInstanceOf(CotizacionNoConvertibleException.class)
        .hasMessageContaining("márcala como aprobada");
    assertThatThrownBy(
            () -> en(EstadoCotizacion.APROBADA).validarConversion(TipoCotizacion.VENTA, 5L))
        .isInstanceOf(CotizacionNoConvertibleException.class)
        .hasMessageContaining("es de instalación");
    assertThatThrownBy(
            () -> en(EstadoCotizacion.APROBADA).validarConversion(TipoCotizacion.INSTALACION, 6L))
        .isInstanceOf(CotizacionNoConvertibleException.class)
        .hasMessageContaining("otro cliente");
  }

  @Test
  void convertidaNoSeVuelveAConvertir() {
    Cotizacion cotizacion = en(EstadoCotizacion.CONVERTIDA);
    assertThatThrownBy(
            () -> cotizacion.convertir(TipoCotizacion.INSTALACION, 5L, 10L, "I-0010", AHORA))
        .isInstanceOf(CotizacionNoConvertibleException.class)
        .hasMessageContaining("ya fue convertida");
    assertThat(cotizacion.getDocumentoId()).isEqualTo(9L);
  }

  @Test
  void anularElDocumentoGenerado_vuelveAAprobada() {
    Cotizacion cotizacion = en(EstadoCotizacion.CONVERTIDA);

    cotizacion.revertirConversion(8L);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.CONVERTIDA);

    cotizacion.revertirConversion(9L);
    assertThat(cotizacion.getEstado()).isEqualTo(EstadoCotizacion.APROBADA);
    assertThat(cotizacion.getDocumentoId()).isNull();
    assertThat(cotizacion.getDocumentoNumero()).isNull();
    assertThat(cotizacion.getConvertidaEn()).isNull();

    cotizacion.convertir(TipoCotizacion.INSTALACION, 5L, 11L, "I-0011", AHORA);
    assertThat(cotizacion.getDocumentoId()).isEqualTo(11L);
  }

  @Test
  void contenidoInvalido_seRechazaAlCrear() {
    assertThatThrownBy(
            () -> Cotizacion.registrar(1, contenido(TipoCotizacion.VENTA, HOY, "0", List.of())))
        .extracting("codigo")
        .isEqualTo(CotizacionInvalidaException.SIN_LINEAS);
  }
}
