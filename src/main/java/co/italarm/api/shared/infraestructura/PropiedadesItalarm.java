package co.italarm.api.shared.infraestructura;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Propiedades propias de la aplicación, tomadas de variables de entorno (BP-17). */
@ConfigurationProperties("italarm")
public record PropiedadesItalarm(
    @DefaultValue Cors cors,
    @DefaultValue Usuarios usuarios,
    @DefaultValue Almacenamiento almacenamiento,
    @DefaultValue Trm trm) {

  /** Orígenes del frontend permitidos por CORS. */
  public record Cors(@DefaultValue List<String> origenes) {}

  /** Contraseña inicial de los usuarios que aún no tienen una (P-01). */
  public record Usuarios(String claveInicial) {}

  /**
   * Dónde se guardan fotos, logos y PDF: {@code disco} (desarrollo local) o {@code s3} (hosting).
   *
   * @param urlPublica dirección con la que el navegador llega a la API (modo disco)
   * @param claveEnlaces clave para firmar los enlaces (modo disco); si falta se genera al arrancar
   * @param duracionEnlaces vigencia de los enlaces firmados
   */
  public record Almacenamiento(
      @DefaultValue("disco") String tipo,
      @DefaultValue("./almacenamiento") String carpeta,
      @DefaultValue("http://localhost:8080") String urlPublica,
      String claveEnlaces,
      @DefaultValue("15m") Duration duracionEnlaces,
      @DefaultValue S3 s3) {}

  /** Conexión a un almacenamiento compatible con S3. */
  public record S3(
      String endpoint,
      @DefaultValue("auto") String region,
      String bucket,
      String accessKey,
      String secretKey) {}

  /**
   * Consulta automática de la TRM (RF-28, P-18).
   *
   * @param url fuente oficial de datos abiertos de la Superintendencia Financiera
   * @param programada si la tarea diaria está activa
   * @param alArrancar si se consulta al arrancar la API cuando falta la TRM de hoy
   */
  public record Trm(
      @DefaultValue("https://www.datos.gov.co/resource/32sa-8pi3.json") String url,
      @DefaultValue("true") boolean programada,
      @DefaultValue("true") boolean alArrancar,
      @DefaultValue("10s") Duration tiempoMaximo) {}
}
