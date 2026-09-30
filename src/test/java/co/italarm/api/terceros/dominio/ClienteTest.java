package co.italarm.api.terceros.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClienteTest {

  private static DatosCliente datos(TipoCliente tipo, TipoDocumento tipoDoc, String numero) {
    return new DatosCliente(
        tipo,
        "  Seguridad  Total SAS ",
        tipoDoc,
        numero,
        "300 123 4567",
        " ventas@st.co ",
        "Cra 10 # 20-30",
        "Cúcuta");
  }

  @Test
  void normalizaLosDatos() {
    Cliente cliente =
        Cliente.crear(datos(TipoCliente.INSTALADOR, TipoDocumento.NIT, " 900.123.456-7 "));

    assertThat(cliente.getNombre()).isEqualTo("Seguridad Total SAS");
    assertThat(cliente.getNumeroDocumento()).isEqualTo("900123456-7");
    assertThat(cliente.getTelefono()).isEqualTo("+573001234567");
    assertThat(cliente.getCorreo()).isEqualTo("ventas@st.co");
    assertThat(cliente.getCiudad()).isEqualTo("Cúcuta");
  }

  @Test
  void elTipoDefineElPrecioQueSeLeAplica() {
    assertThat(Cliente.crear(datos(TipoCliente.INSTALADOR, null, null)).precioAplicado())
        .isEqualTo(PrecioAplicado.INSTALADOR);
    assertThat(Cliente.crear(datos(TipoCliente.CLIENTE_FINAL, null, null)).precioAplicado())
        .isEqualTo(PrecioAplicado.CLIENTE_FINAL);
    assertThat(PrecioAplicado.INSTALADOR.descripcion())
        .isEqualTo("Se le aplicará el precio instalador");
    assertThat(PrecioAplicado.CLIENTE_FINAL.descripcion())
        .isEqualTo("Se le aplicará el precio cliente final");
  }

  @Test
  void elDocumentoVaCompletoOVacio() {
    assertThat(Cliente.crear(datos(TipoCliente.INSTALADOR, null, " ")).getNumeroDocumento())
        .isNull();
    assertThatThrownBy(() -> Cliente.crear(datos(TipoCliente.INSTALADOR, TipoDocumento.CC, null)))
        .isInstanceOf(DocumentoIncompletoException.class);
    assertThatThrownBy(() -> Cliente.crear(datos(TipoCliente.INSTALADOR, null, "123")))
        .isInstanceOf(DocumentoIncompletoException.class);
  }

  @Test
  void seActualiza() {
    Cliente cliente = Cliente.crear(datos(TipoCliente.INSTALADOR, null, null));

    cliente.actualizar(
        new DatosCliente(
            TipoCliente.CLIENTE_FINAL,
            "Ana",
            TipoDocumento.CC,
            "1.090.123",
            "+58 4121234567",
            null,
            null,
            null));

    assertThat(cliente.getTipo()).isEqualTo(TipoCliente.CLIENTE_FINAL);
    assertThat(cliente.getTipoDocumento()).isEqualTo(TipoDocumento.CC);
    assertThat(cliente.getNumeroDocumento()).isEqualTo("1090123");
    assertThat(cliente.getTelefono()).isEqualTo("+584121234567");
    assertThat(cliente.getDireccion()).isNull();
  }
}
