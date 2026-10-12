package co.italarm.api.reportes.aplicacion;

import java.math.BigDecimal;

/**
 * Producto activo por debajo de su stock mínimo (RF-10, RF-140).
 *
 * @param nivel stock ÷ mínimo × 100, de 0 a 100, para la barra de nivel
 */
public record StockBajoVista(
    Long productoId,
    String codigo,
    String nombre,
    String unidad,
    BigDecimal stock,
    BigDecimal minimo,
    BigDecimal nivel) {}
