package co.italarm.api.cotizaciones.infraestructura;

import co.italarm.api.cotizaciones.dominio.CotizacionInvalidaException;
import co.italarm.api.cotizaciones.dominio.CotizacionNoConvertibleException;
import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.DescuentoInvalidoException;
import co.italarm.api.shared.dominio.TipoError;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos de las cotizaciones y su error de negocio (BP-09). */
@Component
public class RestriccionesCotizaciones implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_linea_cotizacion_producto",
            TipoError.VALIDACION,
            CotizacionInvalidaException.PRODUCTO_REPETIDO,
            "Un producto aparece dos veces en la cotización."),
        new RestriccionConocida(
            "ck_cotizacion_descuento",
            TipoError.VALIDACION,
            DescuentoInvalidoException.CODIGO,
            "El descuento no puede ser mayor que el subtotal."),
        new RestriccionConocida(
            "uq_venta_cotizacion_activa",
            TipoError.REGLA_NEGOCIO,
            CotizacionNoConvertibleException.CODIGO,
            "La cotización ya fue convertida."),
        new RestriccionConocida(
            "uq_instalacion_cotizacion_activa",
            TipoError.REGLA_NEGOCIO,
            CotizacionNoConvertibleException.CODIGO,
            "La cotización ya fue convertida."));
  }
}
