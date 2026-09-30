package co.italarm.api.shared.dominio;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fechas de negocio en hora de Colombia (BP-13). Los instantes se guardan en UTC; las fechas de
 * negocio (fecha de venta, vencimientos) se calculan en {@link #ZONA}. El reloj es inyectable para
 * poder probar vencimientos.
 */
public final class FechaNegocio {

  public static final ZoneId ZONA = ZoneId.of("America/Bogota");

  private final Clock reloj;

  public FechaNegocio(Clock reloj) {
    this.reloj = reloj;
  }

  public LocalDate hoy() {
    return LocalDate.now(reloj.withZone(ZONA));
  }

  public Instant ahora() {
    return reloj.instant();
  }

  public LocalDate fechaDe(Instant instante) {
    return LocalDate.ofInstant(instante, ZONA);
  }
}
