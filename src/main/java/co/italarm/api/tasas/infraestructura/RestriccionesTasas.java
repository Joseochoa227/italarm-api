package co.italarm.api.tasas.infraestructura;

import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.TipoError;
import co.italarm.api.tasas.dominio.TasaYaRegistradaException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos de tasas y su error de negocio (BP-09). */
@Component
public class RestriccionesTasas implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_tasa_cambio_par_fecha",
            TipoError.CONFLICTO,
            TasaYaRegistradaException.CODIGO,
            "La tasa de hoy ya fue registrada. Si está errada, corrígela."));
  }
}
