package co.italarm.api.compras.infraestructura;

import co.italarm.api.compras.dominio.ReglasCompra;
import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.TipoError;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos de las compras y su error de negocio (BP-09). */
@Component
public class RestriccionesCompras implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_linea_compra_producto",
            TipoError.VALIDACION,
            ReglasCompra.PRODUCTO_REPETIDO,
            "Un producto aparece dos veces en la compra."),
        new RestriccionConocida(
            "ck_linea_compra_costo",
            TipoError.VALIDACION,
            ReglasCompra.COSTO_INVALIDO,
            "El costo unitario debe ser mayor que 0."));
  }
}
