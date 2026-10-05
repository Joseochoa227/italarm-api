package co.italarm.api.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Producto que sale con una venta (y desde la Fase 4, con una instalación).
 *
 * @param seriales los que salen, si el producto controla serial (RF-21)
 */
public record LineaSalida(Long productoId, BigDecimal cantidad, List<String> seriales) {}
