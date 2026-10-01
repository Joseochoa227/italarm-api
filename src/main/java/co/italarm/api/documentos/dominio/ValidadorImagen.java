package co.italarm.api.documentos.dominio;

/**
 * Valida una imagen subida por su contenido real (firma de los primeros bytes), nunca por la
 * extensión ni por el tipo que declara el navegador (BP-20).
 */
public final class ValidadorImagen {

  /** Tamaño máximo de una imagen: 5 MB (P-16). */
  public static final int TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;

  private static final int[] FIRMA_JPEG = {0xFF, 0xD8, 0xFF};
  private static final int[] FIRMA_PNG = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
  private static final int[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};
  private static final int[] FIRMA_WEBP = {'W', 'E', 'B', 'P'};
  private static final int[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};

  private ValidadorImagen() {}

  /** Imagen o PDF, para documentos como la factura de una compra (RF-44). Máximo 5 MB. */
  public static TipoArchivo validarImagenOPdf(byte[] contenido) {
    if (contenido != null
        && contenido.length > 0
        && contenido.length <= TAMANO_MAXIMO_BYTES
        && empiezaCon(contenido, 0, FIRMA_PDF)) {
      return TipoArchivo.PDF;
    }
    try {
      return validar(contenido);
    } catch (ArchivoTipoNoPermitidoException e) {
      throw new ArchivoTipoNoPermitidoException(
          "El archivo debe ser una imagen JPEG, PNG o WebP, o un PDF.");
    } catch (ArchivoDemasiadoGrandeException e) {
      throw new ArchivoDemasiadoGrandeException("El archivo supera el tamaño máximo de 5 MB.");
    }
  }

  public static TipoArchivo validar(byte[] contenido) {
    if (contenido == null || contenido.length == 0) {
      throw new ArchivoTipoNoPermitidoException();
    }
    if (contenido.length > TAMANO_MAXIMO_BYTES) {
      throw new ArchivoDemasiadoGrandeException("La imagen supera el tamaño máximo de 5 MB.");
    }
    if (empiezaCon(contenido, 0, FIRMA_JPEG)) {
      return TipoArchivo.JPEG;
    }
    if (empiezaCon(contenido, 0, FIRMA_PNG)) {
      return TipoArchivo.PNG;
    }
    if (empiezaCon(contenido, 0, FIRMA_RIFF) && empiezaCon(contenido, 8, FIRMA_WEBP)) {
      return TipoArchivo.WEBP;
    }
    throw new ArchivoTipoNoPermitidoException();
  }

  private static boolean empiezaCon(byte[] contenido, int desde, int[] firma) {
    if (contenido.length < desde + firma.length) {
      return false;
    }
    for (int i = 0; i < firma.length; i++) {
      if ((contenido[desde + i] & 0xFF) != firma[i]) {
        return false;
      }
    }
    return true;
  }
}
