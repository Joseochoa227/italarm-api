package co.italarm.api.cotizaciones.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReglasCotizacionTest {

  private static final LocalDate PRIMERO_OCTUBRE = LocalDate.of(2026, 10, 1);

  @Test
  void validezDe15Dias_venceElDia16() {
    assertThat(ReglasCotizacion.vencimiento(PRIMERO_OCTUBRE, 15))
        .isEqualTo(LocalDate.of(2026, 10, 16));
  }

  @Test
  void soloSeAdmitenValidecesDe8_15y30Dias() {
    ReglasCotizacion.validarValidez(8);
    ReglasCotizacion.validarValidez(15);
    ReglasCotizacion.validarValidez(30);
    assertThatThrownBy(() -> ReglasCotizacion.validarValidez(10))
        .isInstanceOf(CotizacionInvalidaException.class)
        .extracting("codigo")
        .isEqualTo(CotizacionInvalidaException.VALIDEZ_INVALIDA);
  }

  @Test
  void diasParaVencer_cuentaDesdeHoyHastaElVencimiento() {
    LocalDate vence = LocalDate.of(2026, 10, 16);
    assertThat(ReglasCotizacion.diasParaVencer(vence, PRIMERO_OCTUBRE)).isEqualTo(15);
    assertThat(ReglasCotizacion.diasParaVencer(vence, vence)).isZero();
    assertThat(ReglasCotizacion.diasParaVencer(vence, vence.plusDays(1))).isEqualTo(-1);
  }

  @Test
  void quedaVencidaDesdeElDiaSiguienteAlVencimiento() {
    LocalDate vence = LocalDate.of(2026, 10, 16);
    assertThat(ReglasCotizacion.vencida(vence, vence)).isFalse();
    assertThat(ReglasCotizacion.vencida(vence, vence.plusDays(1))).isTrue();
  }

  @Test
  void porVencer_soloEnEvaluacionYCon3DiasOMenos() {
    LocalDate vence = LocalDate.of(2026, 10, 16);
    assertThat(
            ReglasCotizacion.porVencer(
                EstadoCotizacion.EN_EVALUACION, vence, LocalDate.of(2026, 10, 13)))
        .isTrue();
    assertThat(ReglasCotizacion.porVencer(EstadoCotizacion.EN_EVALUACION, vence, vence)).isTrue();
    assertThat(
            ReglasCotizacion.porVencer(
                EstadoCotizacion.EN_EVALUACION, vence, LocalDate.of(2026, 10, 12)))
        .isFalse();
    assertThat(
            ReglasCotizacion.porVencer(
                EstadoCotizacion.BORRADOR, vence, LocalDate.of(2026, 10, 14)))
        .isFalse();
    assertThat(
            ReglasCotizacion.porVencer(
                EstadoCotizacion.EN_EVALUACION, vence, LocalDate.of(2026, 10, 17)))
        .isFalse();
  }

  @Test
  void ventaSinProductos_noSeAdmite() {
    assertThatThrownBy(() -> ReglasCotizacion.validarContenido(TipoCotizacion.VENTA, 0, null, null))
        .extracting("codigo")
        .isEqualTo(CotizacionInvalidaException.SIN_LINEAS);
  }

  @Test
  void ventaConManoDeObra_noSeAdmite() {
    assertThatThrownBy(
            () ->
                ReglasCotizacion.validarContenido(
                    TipoCotizacion.VENTA, 1, new BigDecimal("10"), null))
        .extracting("codigo")
        .isEqualTo(CotizacionInvalidaException.MANO_OBRA_EN_VENTA);
    ReglasCotizacion.validarContenido(TipoCotizacion.VENTA, 1, BigDecimal.ZERO, null);
  }

  @Test
  void instalacion_exigeMaterialOManoDeObraYDescripcion() {
    assertThatThrownBy(
            () ->
                ReglasCotizacion.validarContenido(
                    TipoCotizacion.INSTALACION, 0, BigDecimal.ZERO, "Cámaras"))
        .extracting("codigo")
        .isEqualTo(CotizacionInvalidaException.VACIA);
    assertThatThrownBy(
            () ->
                ReglasCotizacion.validarContenido(
                    TipoCotizacion.INSTALACION, 2, BigDecimal.ZERO, "  "))
        .extracting("codigo")
        .isEqualTo(CotizacionInvalidaException.SIN_DESCRIPCION);
    ReglasCotizacion.validarContenido(
        TipoCotizacion.INSTALACION, 0, new BigDecimal("50"), "Mantenimiento");
  }
}
