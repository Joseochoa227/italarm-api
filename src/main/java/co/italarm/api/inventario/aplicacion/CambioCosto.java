package co.italarm.api.inventario.aplicacion;

import java.math.BigDecimal;

/**
 * Cómo cambia (o cambiaría) el costo en USD de un producto con una compra (RF-41).
 *
 * @param regla SUBE, PROMEDIO o SIN_STOCK
 */
public record CambioCosto(
    Long productoId,
    BigDecimal stockActual,
    BigDecimal costoAnterior,
    BigDecimal costoNuevo,
    String regla) {}
