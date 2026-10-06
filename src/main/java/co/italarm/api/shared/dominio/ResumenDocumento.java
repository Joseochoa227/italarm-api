package co.italarm.api.shared.dominio;

import java.math.BigDecimal;

/**
 * Totales de una venta o instalación (RF-100, RF-119, RN-03). Todo en la moneda del documento salvo
 * los campos en USD.
 *
 * @param material suma del material a precio de venta (RF-117)
 * @param manoDeObra valor digitado de la mano de obra (RF-118); 0 en las ventas
 * @param subtotal material + mano de obra
 * @param costo costo del material al momento de la salida (RF-68)
 * @param porcentajeUtilidad utilidad ÷ total × 100, con 2 decimales; vacío si el total es 0
 */
public record ResumenDocumento(
    BigDecimal material,
    BigDecimal manoDeObra,
    BigDecimal subtotal,
    BigDecimal descuento,
    BigDecimal total,
    BigDecimal costo,
    BigDecimal utilidad,
    BigDecimal porcentajeUtilidad,
    BigDecimal totalUsd,
    BigDecimal costoUsd,
    BigDecimal utilidadUsd) {}
