package co.italarm.api.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tasas guardadas con la venta (RN-04): pesos y bolívares por dólar y su fecha. */
public record TasasVentaVista(
    BigDecimal trm, LocalDate fechaTrm, BigDecimal tasaVes, LocalDate fechaTasaVes) {}
