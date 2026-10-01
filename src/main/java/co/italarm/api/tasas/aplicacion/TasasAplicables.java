package co.italarm.api.tasas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tasas vigentes para una fecha: la registrada ese día o, si no hay, la última anterior (P-19).
 * Cualquiera puede faltar si nunca se ha registrado.
 */
public record TasasAplicables(
    BigDecimal trm, LocalDate fechaTrm, BigDecimal tasaVes, LocalDate fechaTasaVes) {}
