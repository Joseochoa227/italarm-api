package co.italarm.api.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SesionTest {

  private static final Instant INICIO = Instant.parse("2026-10-01T13:00:00Z");

  @Test
  void seAbreActiva() {
    Sesion sesion = Sesion.abrir(1L, "h".repeat(64), "Chrome", INICIO);

    assertThat(sesion.estaActiva()).isTrue();
    assertThat(sesion.getUsuarioId()).isEqualTo(1L);
    assertThat(sesion.getTokenHash()).isEqualTo("h".repeat(64));
    assertThat(sesion.getAgenteUsuario()).isEqualTo("Chrome");
    assertThat(sesion.getCreadaEn()).isEqualTo(INICIO);
    assertThat(sesion.getUltimoUso()).isEqualTo(INICIO);
    assertThat(sesion.getRevocadaEn()).isNull();
  }

  @Test
  void recortaElAgenteDeUsuarioLargo() {
    Sesion sesion = Sesion.abrir(1L, "h".repeat(64), "x".repeat(500), INICIO);

    assertThat(sesion.getAgenteUsuario()).hasSize(300);
  }

  @Test
  void admiteAgenteDeUsuarioAusente() {
    assertThat(Sesion.abrir(1L, "h".repeat(64), null, INICIO).getAgenteUsuario()).isNull();
  }

  @Test
  void alRevocarseDejaDeEstarActivaYConservaLaPrimeraFecha() {
    Sesion sesion = Sesion.abrir(1L, "h".repeat(64), null, INICIO);

    sesion.revocar(INICIO.plusSeconds(60));
    sesion.revocar(INICIO.plusSeconds(120));

    assertThat(sesion.estaActiva()).isFalse();
    assertThat(sesion.getRevocadaEn()).isEqualTo(INICIO.plusSeconds(60));
  }

  @Test
  void registraElUsoSoloCadaCincoMinutos() {
    Sesion sesion = Sesion.abrir(1L, "h".repeat(64), null, INICIO);

    assertThat(sesion.registrarUso(INICIO.plusSeconds(299))).isFalse();
    assertThat(sesion.getUltimoUso()).isEqualTo(INICIO);

    assertThat(sesion.registrarUso(INICIO.plusSeconds(300))).isTrue();
    assertThat(sesion.getUltimoUso()).isEqualTo(INICIO.plusSeconds(300));
  }
}
