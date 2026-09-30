package co.italarm.api.shared.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Único lugar donde se definen el modo de redondeo y las escalas del dinero (BP-06). */
public final class Redondeo {

  public static final RoundingMode MODO = RoundingMode.HALF_UP;

  /** Escala para cálculos intermedios de costos y tasas. */
  public static final int ESCALA_CALCULO = 6;

  /** Escala con la que se guardan dinero y tasas en la base de datos. */
  public static final int ESCALA_ALMACENAMIENTO = 4;

  private Redondeo() {}

  public static BigDecimal paraCalculo(BigDecimal valor) {
    return valor.setScale(ESCALA_CALCULO, MODO);
  }

  public static BigDecimal paraAlmacenar(BigDecimal valor) {
    return valor.setScale(ESCALA_ALMACENAMIENTO, MODO);
  }

  public static BigDecimal paraMostrar(BigDecimal valor, Moneda moneda) {
    return valor.setScale(moneda.decimalesVisibles(), MODO);
  }

  /** Divide con la escala de cálculo, para promedios y conversiones. */
  public static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
    return dividendo.divide(divisor, ESCALA_CALCULO, MODO);
  }
}
