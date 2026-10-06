package co.italarm.api.comercial.aplicacion;

import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;

/**
 * Cobro en las tres monedas (RF-100, RF-119).
 *
 * @param material material a precio de venta (RF-117)
 * @param manoDeObra 0 en las ventas (RF-118)
 * @param subtotal material + mano de obra
 * @param porcentajeUtilidad utilidad ÷ total × 100; vacío si el total es 0
 */
public record ResumenCobroVista(
    MontoEnMonedas material,
    MontoEnMonedas manoDeObra,
    MontoEnMonedas subtotal,
    MontoEnMonedas descuento,
    MontoEnMonedas total,
    MontoEnMonedas costo,
    MontoEnMonedas utilidad,
    BigDecimal porcentajeUtilidad) {}
