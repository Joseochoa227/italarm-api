package co.italarm.api.tasas.aplicacion;

import co.italarm.api.tasas.dominio.FuenteTasa;
import co.italarm.api.tasas.dominio.ParMoneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Tasa vigente de un par (RF-30): la de hoy o, si falta, la última disponible con su aviso (RF-33).
 * Si no hay ninguna tasa, {@code valor} y {@code fecha} vienen vacíos.
 *
 * @param aviso texto para mostrar en Inicio y en el recuadro de tasas; null si la tasa es de hoy
 */
public record TasaVigenteVista(
    ParMoneda par,
    Long id,
    BigDecimal valor,
    LocalDate fecha,
    FuenteTasa fuente,
    Instant registradaEn,
    String registradaPor,
    boolean esDeHoy,
    String aviso) {}
