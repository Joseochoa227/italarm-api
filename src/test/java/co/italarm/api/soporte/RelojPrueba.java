package co.italarm.api.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj que las pruebas pueden fijar y adelantar (BP-13). Sin fijar, marca la hora real. {@link
 * PruebaIntegracion} lo restablece antes de cada prueba.
 */
public class RelojPrueba extends Clock {

  private volatile Instant fijo;

  public void fijar(Instant instante) {
    this.fijo = instante;
  }

  public void avanzar(Duration duracion) {
    this.fijo = instant().plus(duracion);
  }

  public void restablecer() {
    this.fijo = null;
  }

  @Override
  public Instant instant() {
    Instant actual = fijo;
    return actual != null ? actual : Instant.now();
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zona) {
    return Clock.fixed(instant(), zona);
  }
}
