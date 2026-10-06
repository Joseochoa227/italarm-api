package co.italarm.api.comercial.aplicacion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Producto pedido en una venta o instalación (y desde la Fase 5, en una cotización).
 *
 * @param cantidad si el producto controla serial y viene vacía, es la cantidad de seriales (RF-21)
 * @param precioUnitario vacío = el precio sugerido según el tipo de cliente (RN-01)
 */
public record LineaMaterial(
    Long productoId, BigDecimal cantidad, List<String> seriales, BigDecimal precioUnitario) {}
