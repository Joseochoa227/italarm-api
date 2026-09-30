package co.italarm.api.catalogo.infraestructura;

import co.italarm.api.catalogo.aplicacion.MovimientosProducto;
import org.springframework.stereotype.Component;

/**
 * Fase 1: aún no existe el inventario, así que ningún producto tiene movimientos. En la Fase 2 se
 * reemplaza por la consulta al kárdex.
 */
@Component
public class SinMovimientosTodavia implements MovimientosProducto {

  @Override
  public boolean tieneMovimientos(Long productoId) {
    return false;
  }
}
