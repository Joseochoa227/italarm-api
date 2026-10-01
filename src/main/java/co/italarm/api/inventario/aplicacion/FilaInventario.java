package co.italarm.api.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fila de la hoja Inventario inicial (RF-149): producto por código, cantidad, costo unitario en USD
 * y seriales separados por coma en la misma fila (P-26).
 */
public record FilaInventario(
    int fila,
    String codigo,
    BigDecimal cantidad,
    BigDecimal costoUnitarioUsd,
    List<String> seriales) {}
