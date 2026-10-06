package co.italarm.api.instalaciones.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

/** Reglas de los datos de una instalación (RF-107, RF-113, P-37 a P-42). */
public final class ReglasInstalacion {

  /** P-42: máximo de fotos por grupo. */
  public static final int FOTOS_POR_GRUPO = 30;

  private ReglasInstalacion() {}

  /** P-38: la fecha puede ser anterior a hoy, nunca futura. */
  public static void validarFecha(LocalDate fecha, LocalDate hoy) {
    if (fecha.isAfter(hoy)) {
      throw new InstalacionInvalidaException(
          InstalacionInvalidaException.FECHA_FUTURA,
          "La fecha de la instalación no puede ser posterior a hoy.");
    }
  }

  /** P-37: al menos un técnico. */
  public static void validarTecnicos(Collection<Long> tecnicos) {
    if (tecnicos == null || tecnicos.isEmpty()) {
      throw new InstalacionInvalidaException(
          InstalacionInvalidaException.SIN_TECNICOS, "Elige al menos un técnico.");
    }
  }

  /** P-41: al menos material o mano de obra. */
  public static void validarContenido(int lineasMaterial, BigDecimal manoDeObra) {
    if (lineasMaterial == 0 && (manoDeObra == null || manoDeObra.signum() <= 0)) {
      throw new InstalacionInvalidaException(
          InstalacionInvalidaException.VACIA, "La instalación debe tener material o mano de obra.");
    }
  }

  /** P-39: la garantía de mano de obra es de 1, 2 o 3 meses. */
  public static void validarMesesGarantia(int meses) {
    if (meses < 1 || meses > 3) {
      throw new InstalacionInvalidaException(
          InstalacionInvalidaException.GARANTIA_INVALIDA,
          "La garantía de mano de obra debe ser de 1 a 3 meses.");
    }
  }
}
