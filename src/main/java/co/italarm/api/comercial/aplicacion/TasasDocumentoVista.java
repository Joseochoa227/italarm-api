package co.italarm.api.comercial.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tasas guardadas con el documento (RN-04): pesos y bolívares por dólar y su fecha. */
public record TasasDocumentoVista(
    BigDecimal trm, LocalDate fechaTrm, BigDecimal tasaVes, LocalDate fechaTasaVes) {}
