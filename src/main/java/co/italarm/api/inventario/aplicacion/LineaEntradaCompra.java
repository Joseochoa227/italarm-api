package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.util.List;

/**
 * Producto que entra con una compra.
 *
 * @param costoUnitarioUsd costo de la factura ya convertido a USD
 * @param tasaFactura tasa con que se convirtió (null si la factura es en USD)
 * @param costoUnitarioFactura costo en la moneda de la factura
 */
public record LineaEntradaCompra(
    Long productoId,
    BigDecimal cantidad,
    BigDecimal costoUnitarioUsd,
    List<String> seriales,
    Moneda monedaFactura,
    BigDecimal tasaFactura,
    BigDecimal costoUnitarioFactura) {}
