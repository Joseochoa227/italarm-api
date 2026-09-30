package co.italarm.api.documentos.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.documentos.aplicacion.AlmacenamientoArchivos;
import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El almacenamiento S3 con el SDK real de AWS contra un servidor S3 en memoria. */
class AlmacenamientoS3Test {

  private ServidorS3Falso servidor;
  private AlmacenamientoArchivos almacenamiento;

  private static PropiedadesItalarm propiedades(PropiedadesItalarm.S3 s3) {
    return new PropiedadesItalarm(
        new PropiedadesItalarm.Cors(List.of()),
        new PropiedadesItalarm.Usuarios(null),
        new PropiedadesItalarm.Almacenamiento(
            "s3", "no-se-usa", "http://localhost:8080", null, Duration.ofMinutes(15), s3),
        new PropiedadesItalarm.Trm("http://localhost:1", false, false, Duration.ofSeconds(1)));
  }

  @BeforeEach
  void iniciar() throws Exception {
    servidor = new ServidorS3Falso();
    almacenamiento =
        new ConfiguracionAlmacenamiento()
            .almacenamientoS3(
                propiedades(
                    new PropiedadesItalarm.S3(
                        servidor.url(), "auto", "italarm-pruebas", "clave", "secreto")));
  }

  @AfterEach
  void detener() throws Exception {
    ((AutoCloseable) almacenamiento).close();
    servidor.close();
  }

  @Test
  void guardaLeeYEliminaEnElBucket() {
    byte[] contenido = "imagen".getBytes(StandardCharsets.UTF_8);

    almacenamiento.guardar("productos/1/foto-ab.jpg", contenido, "image/jpeg");

    ServidorS3Falso.Objeto guardado =
        servidor.objetos.get("/italarm-pruebas/productos/1/foto-ab.jpg");
    assertThat(guardado.contenido()).isEqualTo(contenido);
    assertThat(guardado.tipoContenido()).isEqualTo("image/jpeg");
    assertThat(almacenamiento.leer("productos/1/foto-ab.jpg")).contains(contenido);

    almacenamiento.eliminar("productos/1/foto-ab.jpg");

    assertThat(almacenamiento.leer("productos/1/foto-ab.jpg")).isEmpty();
  }

  @Test
  void generaUnEnlaceFirmadoQueVenceEnQuinceMinutos() {
    URI enlace = URI.create(almacenamiento.urlFirmada("configuracion/logo-1.png"));

    assertThat(enlace.toString())
        .startsWith(servidor.url() + "/italarm-pruebas/configuracion/logo-1.png?");
    assertThat(enlace.getQuery())
        .contains("X-Amz-Signature=")
        .contains("X-Amz-Expires=900")
        .contains("X-Amz-Credential=clave");
  }

  @Test
  void rechazaClavesPeligrosas() {
    assertThatThrownBy(() -> almacenamiento.guardar("../fuera.jpg", new byte[1], "image/jpeg"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void exigeLasVariablesDeConexion() {
    ConfiguracionAlmacenamiento configuracion = new ConfiguracionAlmacenamiento();

    assertThatThrownBy(
            () ->
                configuracion.almacenamientoS3(
                    propiedades(new PropiedadesItalarm.S3(null, "auto", "", "c", "s"))))
        .hasMessageContaining("ITALARM_S3_BUCKET");
    assertThatThrownBy(
            () ->
                configuracion.almacenamientoS3(
                    propiedades(new PropiedadesItalarm.S3(null, "auto", "b", null, "s"))))
        .hasMessageContaining("ITALARM_S3_ACCESS_KEY");
    assertThatThrownBy(
            () ->
                configuracion.almacenamientoS3(
                    propiedades(new PropiedadesItalarm.S3(null, "auto", "b", "c", " "))))
        .hasMessageContaining("ITALARM_S3_SECRET_KEY");
  }

  @Test
  void sinEndpointUsaAwsS3() throws Exception {
    AlmacenamientoArchivos aws =
        new ConfiguracionAlmacenamiento()
            .almacenamientoS3(
                propiedades(new PropiedadesItalarm.S3("", "us-east-1", "bucket-aws", "c", "s")));

    assertThat(aws.urlFirmada("a.png")).startsWith("https://bucket-aws.s3.amazonaws.com/a.png?");
    ((AutoCloseable) aws).close();
  }
}
