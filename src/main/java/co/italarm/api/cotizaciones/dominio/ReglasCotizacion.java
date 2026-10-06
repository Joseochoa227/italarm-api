package co.italarm.api.cotizaciones.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/** Reglas de validez, vencimiento y contenido de una cotización (RF-83, RF-91, RN-13, P-47). */
public final class ReglasCotizacion {

  /** RF-83: validez de 8, 15 o 30 días. */
  public static final Set<Integer> VALIDECES = Set.of(8, 15, 30);

  /** RF-91: en evaluación con 3 días o menos para vencer. */
  public static final int DIAS_POR_VENCER = 3;

  private ReglasCotizacion() {}

  public static void validarValidez(int dias) {
    if (!VALIDECES.contains(dias)) {
      throw new CotizacionInvalidaException(
          CotizacionInvalidaException.VALIDEZ_INVALIDA,
          "La validez de la cotización debe ser de 8, 15 o 30 días.");
    }
  }

  /** Último día en que la cotización es válida: fecha + validez (P-47). */
  public static LocalDate vencimiento(LocalDate fecha, int validezDias) {
    return fecha.plusDays(validezDias);
  }

  /** Días que faltan para vencer (RF-90): 0 el día del vencimiento, negativo si ya pasó. */
  public static long diasParaVencer(LocalDate vence, LocalDate hoy) {
    return ChronoUnit.DAYS.between(hoy, vence);
  }

  /** P-47: queda Vencida desde el día siguiente al vencimiento. */
  public static boolean vencida(LocalDate vence, LocalDate hoy) {
    return hoy.isAfter(vence);
  }

  /** RF-91: en evaluación y con 3 días o menos para vencer (incluido el día del vencimiento). */
  public static boolean porVencer(EstadoCotizacion estado, LocalDate vence, LocalDate hoy) {
    long dias = diasParaVencer(vence, hoy);
    return estado == EstadoCotizacion.EN_EVALUACION && dias >= 0 && dias <= DIAS_POR_VENCER;
  }

  /**
   * RF-83: una cotización de venta tiene productos y no tiene mano de obra; una de instalación
   * tiene material o mano de obra (como P-41) y la descripción del trabajo.
   */
  public static void validarContenido(
      TipoCotizacion tipo, int lineas, BigDecimal manoDeObra, String descripcion) {
    boolean conManoDeObra = manoDeObra != null && manoDeObra.signum() > 0;
    if (tipo == TipoCotizacion.VENTA) {
      if (lineas == 0) {
        throw new CotizacionInvalidaException(
            CotizacionInvalidaException.SIN_LINEAS,
            "La cotización de venta debe tener al menos un producto.");
      }
      if (conManoDeObra) {
        throw new CotizacionInvalidaException(
            CotizacionInvalidaException.MANO_OBRA_EN_VENTA,
            "Una cotización de venta de material no lleva mano de obra.");
      }
      return;
    }
    if (lineas == 0 && !conManoDeObra) {
      throw new CotizacionInvalidaException(
          CotizacionInvalidaException.VACIA,
          "La cotización de instalación debe tener material o mano de obra.");
    }
    if (descripcion == null || descripcion.isBlank()) {
      throw new CotizacionInvalidaException(
          CotizacionInvalidaException.SIN_DESCRIPCION, "Describe el trabajo a realizar.");
    }
  }
}
