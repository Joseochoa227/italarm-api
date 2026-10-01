package co.italarm.api.shared.dominio;

import java.math.BigDecimal;

/**
 * Tasas con que se convierte un valor: {@code trm} pesos por dólar y {@code ves} bolívares por
 * dólar. Cualquiera puede faltar. Las conversiones calculan con la escala de cálculo (BP-06).
 */
public record Tasas(BigDecimal trm, BigDecimal ves) {

  public BigDecimal aUsd(BigDecimal monto, Moneda moneda) {
    return switch (moneda) {
      case USD -> monto;
      case COP -> Redondeo.dividir(monto, exigir(trm, Moneda.COP));
      case VES -> Redondeo.dividir(monto, exigir(ves, Moneda.VES));
    };
  }

  public BigDecimal desdeUsd(BigDecimal usd, Moneda destino) {
    return switch (destino) {
      case USD -> usd;
      case COP -> Redondeo.paraCalculo(usd.multiply(exigir(trm, Moneda.COP)));
      case VES -> Redondeo.paraCalculo(usd.multiply(exigir(ves, Moneda.VES)));
    };
  }

  /** El valor en las tres monedas; el equivalente que no se puede calcular queda vacío. */
  public MontoEnMonedas equivalentes(Dinero monto) {
    if (!puede(monto.moneda())) {
      return new MontoEnMonedas(
          monto.moneda() == Moneda.USD ? monto : null,
          monto.moneda() == Moneda.COP ? monto : null,
          monto.moneda() == Moneda.VES ? monto : null);
    }
    BigDecimal usd = aUsd(monto.monto(), monto.moneda());
    return new MontoEnMonedas(
        new Dinero(usd, Moneda.USD), en(monto, usd, Moneda.COP), en(monto, usd, Moneda.VES));
  }

  private Dinero en(Dinero original, BigDecimal usd, Moneda destino) {
    if (original.moneda() == destino) {
      return original;
    }
    return puede(destino) ? new Dinero(desdeUsd(usd, destino), destino) : null;
  }

  private boolean puede(Moneda moneda) {
    return switch (moneda) {
      case USD -> true;
      case COP -> trm != null;
      case VES -> ves != null;
    };
  }

  private static BigDecimal exigir(BigDecimal tasa, Moneda moneda) {
    if (tasa == null) {
      throw new TasaNoDisponibleException(
          moneda == Moneda.COP
              ? "No hay TRM registrada para convertir a dólares. Regístrala antes de continuar."
              : "No hay tasa del bolívar registrada para convertir a dólares. Regístrala antes de"
                  + " continuar.");
    }
    return tasa;
  }
}
