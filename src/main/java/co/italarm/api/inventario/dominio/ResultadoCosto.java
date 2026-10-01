package co.italarm.api.inventario.dominio;

import java.math.BigDecimal;

/** Costo antes y después de una compra, y la regla aplicada. El anterior es null si no había. */
public record ResultadoCosto(BigDecimal costoAnterior, BigDecimal costoNuevo, ReglaCosto regla) {}
