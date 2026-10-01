package co.italarm.api.compras.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tasas guardadas con la compra (RF-32, RN-04): pesos y bolívares por dólar y su fecha. */
public record TasasCompraVista(
    BigDecimal trm, LocalDate fechaTrm, BigDecimal tasaVes, LocalDate fechaTasaVes) {}
