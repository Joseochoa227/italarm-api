package co.italarm.api.tasas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** TRM publicada por la Superintendencia Financiera y su vigencia. */
public record TrmPublicada(BigDecimal valor, LocalDate vigenciaDesde, LocalDate vigenciaHasta) {}
