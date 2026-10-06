package co.italarm.api.instalaciones.infraestructura;

import co.italarm.api.instalaciones.dominio.InstalacionInvalidaException;
import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.DescuentoInvalidoException;
import co.italarm.api.shared.dominio.TipoError;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos de las instalaciones y su error de negocio (BP-09). */
@Component
public class RestriccionesInstalaciones implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_linea_instalacion_producto",
            TipoError.VALIDACION,
            InstalacionInvalidaException.PRODUCTO_REPETIDO,
            "Un producto aparece dos veces en la instalación."),
        new RestriccionConocida(
            "ck_instalacion_descuento",
            TipoError.VALIDACION,
            DescuentoInvalidoException.CODIGO,
            "El descuento no puede ser mayor que el subtotal."),
        new RestriccionConocida(
            "fk_tecnico_instalacion_usuario",
            TipoError.VALIDACION,
            "TECNICO_NO_EXISTE",
            "Uno de los técnicos no existe."));
  }
}
