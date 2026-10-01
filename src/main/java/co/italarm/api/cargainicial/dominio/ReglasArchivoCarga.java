package co.italarm.api.cargainicial.dominio;

/** El archivo de la carga inicial es un .xlsx de máximo 5 MB (sección 3.18). */
public final class ReglasArchivoCarga {

  public static final int TAMANO_MAXIMO = 5 * 1024 * 1024;

  private ReglasArchivoCarga() {}

  public static void validar(byte[] contenido) {
    if (contenido == null || contenido.length == 0) {
      throw new ArchivoCargaInvalidoException("El archivo está vacío.");
    }
    if (contenido.length > TAMANO_MAXIMO) {
      throw new ArchivoCargaDemasiadoGrandeException();
    }
    // Un .xlsx es un ZIP: empieza por "PK" 03 04.
    if (contenido.length < 4
        || contenido[0] != 'P'
        || contenido[1] != 'K'
        || contenido[2] != 3
        || contenido[3] != 4) {
      throw new ArchivoCargaInvalidoException(
          "El archivo debe ser un Excel (.xlsx). Usa la plantilla de carga inicial.");
    }
  }
}
