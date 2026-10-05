package co.italarm.api.ventas.infraestructura;

import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.TipoError;
import co.italarm.api.ventas.dominio.DescuentoInvalidoException;
import co.italarm.api.ventas.dominio.VentaInvalidaException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos de las ventas y su error de negocio (BP-09). */
@Component
public class RestriccionesVentas implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_linea_venta_producto",
            TipoError.VALIDACION,
            VentaInvalidaException.PRODUCTO_REPETIDO,
            "Un producto aparece dos veces en la venta."),
        new RestriccionConocida(
            "ck_venta_descuento",
            TipoError.VALIDACION,
            DescuentoInvalidoException.CODIGO,
            "El descuento no puede ser mayor que el subtotal."));
  }
}
