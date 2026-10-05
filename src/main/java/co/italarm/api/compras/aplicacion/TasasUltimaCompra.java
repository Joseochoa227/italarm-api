package co.italarm.api.compras.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tasas guardadas en la última compra no anulada de un producto (RF-69, P-32).
 *
 * @param consecutivo la compra, por ejemplo C-0012
 */
public record TasasUltimaCompra(
    String consecutivo, LocalDate fecha, BigDecimal trm, BigDecimal tasaVes) {}
