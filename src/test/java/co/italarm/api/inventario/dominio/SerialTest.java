package co.italarm.api.inventario.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.TipoDocumento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SerialTest {

  private static final DocumentoRef COMPRA = new DocumentoRef(TipoDocumento.COMPRA, 1L, "C-0001");
  private static final DocumentoRef AJUSTE = new DocumentoRef(TipoDocumento.AJUSTE, 2L, "AJ-001");

  @Test
  void p22_normalizaElNumeroSinEspaciosYEnMayusculas() {
    assertThat(Serial.normalizar("  abc-123x ")).isEqualTo("ABC-123X");
  }

  @Test
  void rechazaUnSerialVacioODemasiadoLargo() {
    assertThatThrownBy(() -> Serial.normalizar("  "))
        .isInstanceOf(SerialInvalidoException.class)
        .hasMessage("Hay un número de serie vacío.");
    assertThatThrownBy(() -> Serial.normalizar("X".repeat(81)))
        .isInstanceOf(SerialInvalidoException.class);
  }

  @Test
  void entraEnBodegaYSeDaDeBaja() {
    Serial serial = Serial.entrar(5L, "abc1", COMPRA, LocalDate.of(2026, 10, 1));

    assertThat(serial.getNumero()).isEqualTo("ABC1");
    assertThat(serial.getEstado()).isEqualTo(EstadoSerial.EN_BODEGA);
    assertThat(serial.getDocumentoEntrada()).isEqualTo(COMPRA);
    assertThat(serial.estaDisponible()).isTrue();

    serial.darDeBaja(AJUSTE);

    assertThat(serial.getEstado()).isEqualTo(EstadoSerial.DADO_DE_BAJA);
    assertThat(serial.getDocumentoSalida()).isEqualTo(AJUSTE);
    assertThat(serial.estaDisponible()).isFalse();
  }

  @Test
  void unSerialQueNoEstaEnBodegaNoPuedeSalirNiAnularse() {
    Serial serial = Serial.entrar(5L, "abc1", COMPRA, LocalDate.of(2026, 10, 1));
    serial.darDeBaja(AJUSTE);

    assertThatThrownBy(() -> serial.darDeBaja(AJUSTE))
        .isInstanceOf(SerialNoDisponibleException.class)
        .hasMessage("El serial ABC1 no está en bodega.");
    assertThatThrownBy(serial::anular).isInstanceOf(SerialNoDisponibleException.class);
  }

  @Test
  void alAnularLaCompraElSerialQuedaAnulado() {
    Serial serial = Serial.entrar(5L, "abc1", COMPRA, LocalDate.of(2026, 10, 1));

    serial.anular();

    assertThat(serial.getEstado()).isEqualTo(EstadoSerial.ANULADO);
  }

  @Test
  void rf20_laCantidadDeSerialesDebeCoincidir() {
    assertThatThrownBy(
            () -> Serial.validarLista(List.of("A1", "A2"), new BigDecimal("3"), "Cámara domo"))
        .isInstanceOf(SerialesNoCoincidenException.class)
        .hasMessage("Cámara domo: se compraron 3 unidades y se ingresaron 2 seriales.");
    assertThatThrownBy(() -> Serial.validarLista(null, new BigDecimal("1"), "Cámara domo"))
        .isInstanceOf(SerialesNoCoincidenException.class);
  }

  @Test
  void rf20_noPermiteSerialesRepetidos() {
    assertThatThrownBy(
            () -> Serial.validarLista(List.of("a1", " A1 "), new BigDecimal("2"), "Cámara domo"))
        .isInstanceOf(SerialDuplicadoException.class)
        .hasMessage("Cámara domo: el serial A1 está repetido.");
  }

  @Test
  void devuelveLosSerialesNormalizados() {
    assertThat(Serial.validarLista(List.of(" a1", "b2 "), new BigDecimal("2"), "X"))
        .containsExactly("A1", "B2");
  }

  @Test
  void vender_dejaElSerialVendidoConGarantia_yAnularLaVentaLoDevuelve() {
    DocumentoRef venta = new DocumentoRef(TipoDocumento.VENTA, 9L, "V-0001");
    Serial serial = Serial.entrar(1L, "abc", COMPRA, java.time.LocalDate.of(2026, 10, 1));

    serial.vender(venta, java.time.LocalDate.of(2027, 1, 15));

    org.assertj.core.api.Assertions.assertThat(serial.getEstado()).isEqualTo(EstadoSerial.VENDIDO);
    org.assertj.core.api.Assertions.assertThat(serial.getDocumentoSalida()).isEqualTo(venta);
    org.assertj.core.api.Assertions.assertThat(serial.getVencimientoGarantia())
        .isEqualTo(java.time.LocalDate.of(2027, 1, 15));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> serial.vender(venta, java.time.LocalDate.of(2027, 1, 15)))
        .isInstanceOf(SerialNoDisponibleException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> serial.devolver(AJUSTE))
        .isInstanceOf(SerialNoDisponibleException.class);

    serial.devolver(venta);

    org.assertj.core.api.Assertions.assertThat(serial.estaDisponible()).isTrue();
    org.assertj.core.api.Assertions.assertThat(serial.getDocumentoSalida()).isNull();
    org.assertj.core.api.Assertions.assertThat(serial.getVencimientoGarantia()).isNull();
  }
}
