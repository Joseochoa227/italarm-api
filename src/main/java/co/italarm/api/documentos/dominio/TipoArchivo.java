package co.italarm.api.documentos.dominio;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatos de archivo permitidos: imágenes para fotos y logo (P-16); además PDF para facturas
 * (RF-44).
 */
public enum TipoArchivo {
  JPEG("image/jpeg", "jpg"),
  PNG("image/png", "png"),
  WEBP("image/webp", "webp"),
  PDF("application/pdf", "pdf");

  private final String tipoContenido;
  private final String extension;

  TipoArchivo(String tipoContenido, String extension) {
    this.tipoContenido = tipoContenido;
    this.extension = extension;
  }

  public boolean esImagen() {
    return this != PDF;
  }

  public String tipoContenido() {
    return tipoContenido;
  }

  public String extension() {
    return extension;
  }

  /** Tipo de imagen según la extensión de una clave o nombre de archivo. */
  public static Optional<TipoArchivo> deExtension(String nombre) {
    int punto = nombre.lastIndexOf('.');
    if (punto < 0) {
      return Optional.empty();
    }
    String ext = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    String normalizada = "jpeg".equals(ext) ? "jpg" : ext;
    return Arrays.stream(values()).filter(t -> t.extension.equals(normalizada)).findFirst();
  }
}
