package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** RN-09: formato de los consecutivos. */
class TipoDocumentoTest {

  @Test
  void formateaCadaConsecutivo() {
    assertThat(TipoDocumento.COMPRA.consecutivo(1)).isEqualTo("C-0001");
    assertThat(TipoDocumento.AJUSTE.consecutivo(1)).isEqualTo("AJ-001");
    assertThat(TipoDocumento.INVENTARIO_INICIAL.consecutivo(1)).isEqualTo("II-001");
    assertThat(TipoDocumento.VENTA.consecutivo(319)).isEqualTo("V-0319");
    assertThat(TipoDocumento.INSTALACION.consecutivo(88)).isEqualTo("I-0088");
    assertThat(TipoDocumento.COTIZACION.consecutivo(45)).isEqualTo("COT-0045");
    assertThat(TipoDocumento.AJUSTE.consecutivo(1234)).isEqualTo("AJ-1234");
  }
}
