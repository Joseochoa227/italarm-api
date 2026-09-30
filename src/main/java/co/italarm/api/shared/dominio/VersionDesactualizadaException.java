package co.italarm.api.shared.dominio;

/** Otro usuario modificó el registro después de que este usuario lo consultó (BP-12). */
public class VersionDesactualizadaException extends NegocioException {

  public VersionDesactualizadaException() {
    super(
        TipoError.CONFLICTO,
        CodigoError.MODIFICADO_POR_OTRO_USUARIO,
        "Otro usuario modificó este registro. Recarga la información e intenta de nuevo.");
  }
}
