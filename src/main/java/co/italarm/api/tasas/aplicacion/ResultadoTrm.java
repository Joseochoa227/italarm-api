package co.italarm.api.tasas.aplicacion;

import co.italarm.api.tasas.dominio.ResultadoEjecucion;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado de consultar la TRM de un día. */
public record ResultadoTrm(
    LocalDate fecha, ResultadoEjecucion resultado, BigDecimal valor, String detalle) {}
