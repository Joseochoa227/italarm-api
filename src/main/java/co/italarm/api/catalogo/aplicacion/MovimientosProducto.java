package co.italarm.api.catalogo.aplicacion;

/**
 * Indica si un producto ya tiene movimientos de inventario (RF-14, P-17). Lo implementa el módulo
 * de inventario a partir de la Fase 2.
 */
public interface MovimientosProducto {

  boolean tieneMovimientos(Long productoId);
}
