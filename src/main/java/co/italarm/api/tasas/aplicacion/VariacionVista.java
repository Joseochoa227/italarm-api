package co.italarm.api.tasas.aplicacion;

import java.math.BigDecimal;

/**
 * Vista previa antes de guardar una tasa manual (RF-35b): la anterior, la nueva y el porcentaje. Si
 * {@code superaLimite}, el frontend muestra la alerta y el usuario debe aceptarla (RF-35c).
 */
public record VariacionVista(
    BigDecimal anterior,
    BigDecimal nueva,
    BigDecimal porcentaje,
    BigDecimal limite,
    boolean superaLimite) {}
