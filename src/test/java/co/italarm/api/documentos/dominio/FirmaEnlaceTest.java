package co.italarm.api.documentos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FirmaEnlaceTest {

  private static final Instant AHORA = Instant.parse("2026-10-01T15:00:00Z");
  private final FirmaEnlace firma =
      new FirmaEnlace("clave-secreta".getBytes(StandardCharsets.UTF_8));

  @Test
  void unEnlaceFirmadoEsValidoHastaQueVence() {
    EnlaceFirmado enlace = firma.firmar("productos/1/foto.jpg", AHORA, Duration.ofMinutes(15));

    assertThat(enlace.expira()).isEqualTo(AHORA.plusSeconds(900).getEpochSecond());
    assertThat(firma.esValido("productos/1/foto.jpg", enlace.expira(), enlace.firma(), AHORA))
        .isTrue();
    assertThat(
            firma.esValido(
                "productos/1/foto.jpg", enlace.expira(), enlace.firma(), AHORA.plusSeconds(900)))
        .isTrue();
    assertThat(
            firma.esValido(
                "productos/1/foto.jpg", enlace.expira(), enlace.firma(), AHORA.plusSeconds(901)))
        .isFalse();
  }

  @Test
  void rechazaUnEnlaceAdulterado() {
    EnlaceFirmado enlace = firma.firmar("productos/1/foto.jpg", AHORA, Duration.ofMinutes(15));

    assertThat(firma.esValido("productos/2/foto.jpg", enlace.expira(), enlace.firma(), AHORA))
        .isFalse();
    assertThat(firma.esValido("productos/1/foto.jpg", enlace.expira() + 60, enlace.firma(), AHORA))
        .isFalse();
    assertThat(firma.esValido("productos/1/foto.jpg", enlace.expira(), "abc", AHORA)).isFalse();
    assertThat(firma.esValido("productos/1/foto.jpg", enlace.expira(), null, AHORA)).isFalse();
  }

  @Test
  void otraClaveProduceOtraFirma() {
    FirmaEnlace otra = new FirmaEnlace("otra-clave".getBytes(StandardCharsets.UTF_8));
    EnlaceFirmado enlace = firma.firmar("logo.png", AHORA, Duration.ofMinutes(15));

    assertThat(otra.esValido("logo.png", enlace.expira(), enlace.firma(), AHORA)).isFalse();
  }
}
