package co.italarm.api.shared.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Monto con su moneda. Siempre {@link BigDecimal}; nunca {@code double} ni {@code float} (BP-06).
 * Solo opera con dinero de la misma moneda: las conversiones se hacen de forma explícita con una
 * tasa de cambio.
 */
public record Dinero(BigDecimal monto, Moneda moneda) implements Comparable<Dinero> {

  private static final BigDecimal CIEN = new BigDecimal("100");

  public Dinero {
    Objects.requireNonNull(monto, "El monto es obligatorio");
    Objects.requireNonNull(moneda, "La moneda es obligatoria");
  }

  public static Dinero de(String monto, Moneda moneda) {
    return new Dinero(new BigDecimal(monto), moneda);
  }

  public static Dinero cero(Moneda moneda) {
    return new Dinero(BigDecimal.ZERO, moneda);
  }

  public Dinero sumar(Dinero otro) {
    exigirMismaMoneda(otro);
    return new Dinero(monto.add(otro.monto), moneda);
  }

  public Dinero restar(Dinero otro) {
    exigirMismaMoneda(otro);
    return new Dinero(monto.subtract(otro.monto), moneda);
  }

  public Dinero multiplicar(BigDecimal factor) {
    return new Dinero(monto.multiply(factor), moneda);
  }

  /** Porcentaje del monto; por ejemplo {@code porcentaje(7)} de US$ 100 es US$ 7. */
  public Dinero porcentaje(BigDecimal porcentaje) {
    return new Dinero(Redondeo.dividir(monto.multiply(porcentaje), CIEN), moneda);
  }

  public boolean esCero() {
    return monto.signum() == 0;
  }

  public boolean esNegativo() {
    return monto.signum() < 0;
  }

  public boolean esMayorQue(Dinero otro) {
    return compareTo(otro) > 0;
  }

  public Dinero paraAlmacenar() {
    return new Dinero(Redondeo.paraAlmacenar(monto), moneda);
  }

  public Dinero paraMostrar() {
    return new Dinero(Redondeo.paraMostrar(monto, moneda), moneda);
  }

  @Override
  public int compareTo(Dinero otro) {
    exigirMismaMoneda(otro);
    return monto.compareTo(otro.monto);
  }

  /** Dos montos son iguales si valen lo mismo en la misma moneda, sin importar la escala. */
  @Override
  public boolean equals(Object objeto) {
    return objeto instanceof Dinero otro
        && moneda == otro.moneda
        && monto.compareTo(otro.monto) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(monto.stripTrailingZeros(), moneda);
  }

  @Override
  public String toString() {
    return moneda + " " + monto.toPlainString();
  }

  private void exigirMismaMoneda(Dinero otro) {
    if (moneda != otro.moneda) {
      throw new IllegalArgumentException(
          "No se puede operar " + moneda + " con " + otro.moneda + " sin una tasa de cambio");
    }
  }
}
