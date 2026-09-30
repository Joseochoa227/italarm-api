package co.italarm.api.documentos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ValidadorImagenTest {

  static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
  static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
  static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

  @Test
  void reconoceElTipoPorElContenidoReal() {
    assertThat(ValidadorImagen.validar(JPEG)).isEqualTo(TipoImagen.JPEG);
    assertThat(ValidadorImagen.validar(PNG)).isEqualTo(TipoImagen.PNG);
    assertThat(ValidadorImagen.validar(WEBP)).isEqualTo(TipoImagen.WEBP);
  }

  @Test
  void rechazaUnEjecutableRenombradoComoImagen() {
    byte[] ejecutable = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xFF};

    assertThatThrownBy(() -> ValidadorImagen.validar(ejecutable))
        .isInstanceOf(ArchivoTipoNoPermitidoException.class)
        .hasMessage("El archivo debe ser una imagen JPEG, PNG o WebP.");
  }

  @Test
  void rechazaUnArchivoVacioOMuyCorto() {
    assertThatThrownBy(() -> ValidadorImagen.validar(new byte[0]))
        .isInstanceOf(ArchivoTipoNoPermitidoException.class);
    assertThatThrownBy(() -> ValidadorImagen.validar(new byte[] {(byte) 0xFF}))
        .isInstanceOf(ArchivoTipoNoPermitidoException.class);
    assertThatThrownBy(() -> ValidadorImagen.validar(null))
        .isInstanceOf(ArchivoTipoNoPermitidoException.class);
  }

  @Test
  void rechazaUnRiffQueNoEsWebp() {
    byte[] wav = "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1);

    assertThatThrownBy(() -> ValidadorImagen.validar(wav))
        .isInstanceOf(ArchivoTipoNoPermitidoException.class);
  }

  @Test
  void rechazaImagenesDeMasDeCincoMegas() {
    byte[] grande = new byte[ValidadorImagen.TAMANO_MAXIMO_BYTES + 1];
    System.arraycopy(JPEG, 0, grande, 0, JPEG.length);

    assertThatThrownBy(() -> ValidadorImagen.validar(grande))
        .isInstanceOf(ArchivoDemasiadoGrandeException.class)
        .hasMessage("La imagen supera el tamaño máximo de 5 MB.");
  }

  @Test
  void aceptaExactamenteCincoMegas() {
    byte[] limite = new byte[ValidadorImagen.TAMANO_MAXIMO_BYTES];
    System.arraycopy(PNG, 0, limite, 0, PNG.length);

    assertThat(ValidadorImagen.validar(limite)).isEqualTo(TipoImagen.PNG);
  }

  @Test
  void cadaTipoConoceSuExtensionYTipoDeContenido() {
    assertThat(TipoImagen.JPEG.extension()).isEqualTo("jpg");
    assertThat(TipoImagen.PNG.tipoContenido()).isEqualTo("image/png");
    assertThat(TipoImagen.deExtension("foto.webp")).contains(TipoImagen.WEBP);
    assertThat(TipoImagen.deExtension("foto.JPG")).contains(TipoImagen.JPEG);
    assertThat(TipoImagen.deExtension("archivo.pdf")).isEmpty();
    assertThat(TipoImagen.deExtension("sin-extension")).isEmpty();
  }
}
