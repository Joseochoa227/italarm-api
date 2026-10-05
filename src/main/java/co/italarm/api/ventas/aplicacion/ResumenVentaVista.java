package co.italarm.api.ventas.aplicacion;

import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;

/**
 * Resumen de la venta en las tres monedas (RF-100).
 *
 * @param porcentajeUtilidad utilidad ÷ total × 100; vacío si el total es 0
 */
public record ResumenVentaVista(
    MontoEnMonedas subtotal,
    MontoEnMonedas descuento,
    MontoEnMonedas total,
    MontoEnMonedas costo,
    MontoEnMonedas utilidad,
    BigDecimal porcentajeUtilidad) {}
