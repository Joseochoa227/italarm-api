package co.italarm.api.catalogo.dominio;

import java.math.BigDecimal;

/**
 * Cantidades según la unidad (P-09): las unidades que admiten decimales (Metro) aceptan hasta 2;
 * las demás (Unidad, Par), solo enteros. Nunca negativas.
 */
public final class ReglaCantidad {

  public static final int DECIMALES_MAXIMOS = 2;

  private ReglaCantidad() {}

  public static BigDecimal validar(
      BigDecimal cantidad, boolean admiteDecimales, String abreviatura, String campo) {
    if (cantidad == null) {
      return null;
    }
    if (cantidad.signum() < 0) {
      throw new CantidadInvalidaException(campo + ": la cantidad no puede ser negativa.");
    }
    int decimales = Math.max(cantidad.stripTrailingZeros().scale(), 0);
    if (!admiteDecimales && decimales > 0) {
      throw new CantidadInvalidaException(
          campo + ": en " + abreviatura + " la cantidad debe ser un número entero.");
    }
    if (decimales > DECIMALES_MAXIMOS) {
      throw new CantidadInvalidaException(
          campo + ": en " + abreviatura + " se admiten máximo 2 decimales.");
    }
    return cantidad;
  }
}
