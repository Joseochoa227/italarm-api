package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Un usuario no puede desactivar su propia cuenta. */
public class NoPuedeDesactivarseException extends NegocioException {

  public static final String CODIGO = "NO_PUEDE_DESACTIVARSE_A_SI_MISMO";

  public NoPuedeDesactivarseException() {
    super(TipoError.REGLA_NEGOCIO, CODIGO, "No puedes desactivar tu propio usuario.");
  }
}
