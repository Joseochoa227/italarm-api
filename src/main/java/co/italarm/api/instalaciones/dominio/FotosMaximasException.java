package co.italarm.api.instalaciones.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;
import java.util.Locale;

/** El grupo ya tiene el máximo de fotos (P-42). */
public class FotosMaximasException extends NegocioException {

  public static final String CODIGO = "FOTOS_MAXIMAS";

  public FotosMaximasException(GrupoFoto grupo) {
    super(
        TipoError.REGLA_NEGOCIO,
        CODIGO,
        "El grupo "
            + grupo.name().toLowerCase(Locale.ROOT)
            + " ya tiene "
            + ReglasInstalacion.FOTOS_POR_GRUPO
            + " fotos. Quita alguna antes de agregar otra.");
  }
}
