package co.italarm.api.inventario.aplicacion;

import java.math.BigDecimal;

/** Producto y cantidad que entraron con un documento que se anula. */
public record LineaAnulacion(Long productoId, BigDecimal cantidad) {}
