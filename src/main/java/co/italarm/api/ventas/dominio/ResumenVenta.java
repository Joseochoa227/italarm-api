package co.italarm.api.ventas.dominio;

import java.math.BigDecimal;

/**
 * Totales de una venta (RF-100, RN-03). Todo en la moneda de la venta salvo los campos en USD.
 *
 * @param costo costo del material al momento de la salida (RF-68)
 * @param porcentajeUtilidad utilidad ÷ total × 100, con 2 decimales; vacío si el total es 0
 */
public record ResumenVenta(
    BigDecimal subtotal,
    BigDecimal descuento,
    BigDecimal total,
    BigDecimal costo,
    BigDecimal utilidad,
    BigDecimal porcentajeUtilidad,
    BigDecimal totalUsd,
    BigDecimal costoUsd,
    BigDecimal utilidadUsd) {}
