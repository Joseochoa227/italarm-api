package co.italarm.api.terceros.infraestructura;

import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.TipoError;
import co.italarm.api.terceros.dominio.ClienteDocumentoDuplicadoException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos de terceros y su error de negocio (BP-09). */
@Component
public class RestriccionesTerceros implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_cliente_documento",
            TipoError.CONFLICTO,
            ClienteDocumentoDuplicadoException.CODIGO,
            "Ya existe un cliente con ese documento."));
  }
}
