package co.italarm.api.inventario.aplicacion;

import co.italarm.api.catalogo.aplicacion.MovimientosProducto;
import co.italarm.api.inventario.infraestructura.MovimientoInventarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un producto tiene movimientos si aparece en el kárdex. Con esto el catálogo impide eliminarlo
 * (RF-14) y cambiar su serial o su unidad (P-17).
 */
@Service
public class MovimientosProductoKardex implements MovimientosProducto {

  private final MovimientoInventarioRepositorio movimientos;

  public MovimientosProductoKardex(MovimientoInventarioRepositorio movimientos) {
    this.movimientos = movimientos;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean tieneMovimientos(Long productoId) {
    return movimientos.existsByProductoId(productoId);
  }
}
