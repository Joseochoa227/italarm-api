package co.italarm.api.cargainicial.dominio;

import co.italarm.api.shared.dominio.ErrorCarga;
import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;
import java.util.List;

/** El archivo tiene errores; no se guardó nada (RF-150). Lleva la lista por hoja y fila. */
public class CargaInicialConErroresException extends NegocioException {

  public static final String CODIGO = "CARGA_INICIAL_CON_ERRORES";

  private final transient List<ErrorCarga> errores;

  public CargaInicialConErroresException(List<ErrorCarga> errores) {
    super(
        TipoError.VALIDACION,
        CODIGO,
        "El archivo tiene "
            + errores.size()
            + (errores.size() == 1 ? " error" : " errores")
            + "; no se guardó nada. Corrígelos y vuelve a subirlo.");
    this.errores = List.copyOf(errores);
  }

  @Override
  public List<ErrorCarga> detalles() {
    return errores;
  }
}
