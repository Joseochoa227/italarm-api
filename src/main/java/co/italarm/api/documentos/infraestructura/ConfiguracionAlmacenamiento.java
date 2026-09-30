package co.italarm.api.documentos.infraestructura;

import co.italarm.api.documentos.aplicacion.AlmacenamientoArchivos;
import co.italarm.api.documentos.dominio.FirmaEnlace;
import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Elige el almacenamiento según {@code ITALARM_ALMACENAMIENTO}: {@code disco} o {@code s3}. */
@Configuration
public class ConfiguracionAlmacenamiento {

  private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionAlmacenamiento.class);
  private static final String PROPIEDAD = "italarm.almacenamiento.tipo";

  @Bean
  @ConditionalOnProperty(name = PROPIEDAD, havingValue = "disco", matchIfMissing = true)
  public FirmaEnlace firmaEnlaces(PropiedadesItalarm propiedades) {
    String clave = propiedades.almacenamiento().claveEnlaces();
    if (clave == null || clave.isBlank()) {
      byte[] aleatoria = new byte[32];
      new SecureRandom().nextBytes(aleatoria);
      LOG.info(
          "ITALARM_ENLACES_CLAVE no definida: se generó una clave; los enlaces vencen al reiniciar");
      return new FirmaEnlace(aleatoria);
    }
    return new FirmaEnlace(clave.getBytes(StandardCharsets.UTF_8));
  }

  @Bean
  @ConditionalOnProperty(name = PROPIEDAD, havingValue = "disco", matchIfMissing = true)
  public AlmacenamientoArchivos almacenamientoDisco(
      PropiedadesItalarm propiedades, FirmaEnlace firma, Clock reloj) {
    PropiedadesItalarm.Almacenamiento config = propiedades.almacenamiento();
    Path carpeta = Path.of(config.carpeta());
    LOG.info("Almacenamiento de archivos en disco: {}", carpeta.toAbsolutePath().normalize());
    return new AlmacenamientoDisco(
        carpeta, config.urlPublica(), firma, config.duracionEnlaces(), reloj);
  }

  @Bean
  @ConditionalOnProperty(name = PROPIEDAD, havingValue = "s3")
  public AlmacenamientoArchivos almacenamientoS3(PropiedadesItalarm propiedades) {
    PropiedadesItalarm.S3 s3 = propiedades.almacenamiento().s3();
    exigir(s3.bucket(), "ITALARM_S3_BUCKET");
    exigir(s3.accessKey(), "ITALARM_S3_ACCESS_KEY");
    exigir(s3.secretKey(), "ITALARM_S3_SECRET_KEY");
    StaticCredentialsProvider credenciales =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()));
    Region region = Region.of(s3.region());
    boolean conEndpoint = s3.endpoint() != null && !s3.endpoint().isBlank();
    S3Configuration configuracion =
        S3Configuration.builder().pathStyleAccessEnabled(conEndpoint).build();

    var constructorCliente =
        S3Client.builder()
            .region(region)
            .credentialsProvider(credenciales)
            .serviceConfiguration(configuracion)
            // R2 y otros compatibles no aceptan las sumas de verificación nuevas por defecto.
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
    var constructorFirmador =
        S3Presigner.builder()
            .region(region)
            .credentialsProvider(credenciales)
            .serviceConfiguration(configuracion);
    if (conEndpoint) {
      URI endpoint = URI.create(s3.endpoint());
      constructorCliente.endpointOverride(endpoint);
      constructorFirmador.endpointOverride(endpoint);
    }
    LOG.info("Almacenamiento de archivos en S3: bucket {}", s3.bucket());
    return new AlmacenamientoS3(
        constructorCliente.build(),
        constructorFirmador.build(),
        s3.bucket(),
        propiedades.almacenamiento().duracionEnlaces());
  }

  private static void exigir(String valor, String variable) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException(
          "Falta la variable " + variable + " para usar el almacenamiento S3");
    }
  }
}
