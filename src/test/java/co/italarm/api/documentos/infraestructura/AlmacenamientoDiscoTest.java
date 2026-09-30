package co.italarm.api.documentos.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.documentos.dominio.FirmaEnlace;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlmacenamientoDiscoTest {

  @TempDir Path carpeta;

  private final Clock reloj = Clock.fixed(Instant.parse("2026-10-01T15:00:00Z"), ZoneOffset.UTC);
  private final FirmaEnlace firma = new FirmaEnlace("k".getBytes(StandardCharsets.UTF_8));

  private AlmacenamientoDisco almacenamiento() {
    return new AlmacenamientoDisco(
        carpeta, "http://localhost:8080/", firma, Duration.ofMinutes(15), reloj);
  }

  @Test
  void guardaLeeYEliminaDentroDeLaCarpeta() throws Exception {
    AlmacenamientoDisco almacenamiento = almacenamiento();
    byte[] contenido = {1, 2, 3};

    almacenamiento.guardar("productos/7/foto-1.png", contenido, "image/png");

    assertThat(Files.readAllBytes(carpeta.resolve("productos/7/foto-1.png"))).isEqualTo(contenido);
    assertThat(almacenamiento.leer("productos/7/foto-1.png")).contains(contenido);

    almacenamiento.eliminar("productos/7/foto-1.png");

    assertThat(almacenamiento.leer("productos/7/foto-1.png")).isEmpty();
    almacenamiento.eliminar("productos/7/foto-1.png");
  }

  @Test
  void elEnlaceApuntaAlEndpointDeDescargaConFirmaValida() {
    URI enlace = URI.create(almacenamiento().urlFirmada("productos/7/foto-1.png"));

    assertThat(enlace.getPath()).isEqualTo("/api/v1/archivos");
    assertThat(enlace.toString())
        .startsWith("http://localhost:8080/api/v1/archivos?clave=productos%2F7%2Ffoto-1.png");
    long expira = reloj.instant().plusSeconds(900).getEpochSecond();
    assertThat(enlace.getQuery()).contains("expira=" + expira);
    String valorFirma = enlace.getQuery().replaceAll(".*firma=", "");
    assertThat(firma.esValido("productos/7/foto-1.png", expira, valorFirma, reloj.instant()))
        .isTrue();
  }

  @Test
  void noPermiteSalirDeLaCarpeta() {
    assertThatThrownBy(() -> almacenamiento().guardar("../fuera.jpg", new byte[1], "image/jpeg"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> almacenamiento().leer("a/../../fuera.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
