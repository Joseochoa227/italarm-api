package co.italarm.api.shared.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FechaNegocioTest {

  @Test
  void laFechaDeNegocioEsLaDeColombia() {
    // 23:30 del 1 de octubre en Bogotá = 04:30 UTC del 2 de octubre.
    Clock reloj = Clock.fixed(Instant.parse("2026-10-02T04:30:00Z"), ZoneOffset.UTC);
    FechaNegocio fechas = new FechaNegocio(reloj);

    assertThat(fechas.hoy()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(fechas.ahora()).isEqualTo(Instant.parse("2026-10-02T04:30:00Z"));
  }

  @Test
  void convierteUnInstanteEnFechaDeColombia() {
    FechaNegocio fechas = new FechaNegocio(Clock.systemUTC());

    assertThat(fechas.fechaDe(Instant.parse("2026-10-02T05:00:00Z")))
        .isEqualTo(LocalDate.of(2026, 10, 2));
    assertThat(fechas.fechaDe(Instant.parse("2026-10-02T04:59:59Z")))
        .isEqualTo(LocalDate.of(2026, 10, 1));
  }

  @Test
  void laZonaEstaDefinidaEnUnSoloLugar() {
    assertThat(FechaNegocio.ZONA.getId()).isEqualTo("America/Bogota");
  }
}
