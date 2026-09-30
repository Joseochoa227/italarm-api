package co.italarm.api.documentos.infraestructura;

import co.italarm.api.documentos.aplicacion.AlmacenamientoArchivos;
import co.italarm.api.documentos.dominio.ClaveArchivo;
import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Almacenamiento en un bucket privado compatible con S3: Cloudflare R2, AWS S3 o DigitalOcean
 * Spaces (9.1, 9.5). Los archivos se entregan con enlaces firmados de corta duración.
 */
public class AlmacenamientoS3 implements AlmacenamientoArchivos, AutoCloseable {

  private final S3Client cliente;
  private final S3Presigner firmador;
  private final String bucket;
  private final Duration duracion;

  public AlmacenamientoS3(
      S3Client cliente, S3Presigner firmador, String bucket, Duration duracion) {
    this.cliente = cliente;
    this.firmador = firmador;
    this.bucket = bucket;
    this.duracion = duracion;
  }

  @Override
  public void guardar(String clave, byte[] contenido, String tipoContenido) {
    cliente.putObject(
        p -> p.bucket(bucket).key(ClaveArchivo.validar(clave)).contentType(tipoContenido),
        RequestBody.fromBytes(contenido));
  }

  @Override
  public Optional<byte[]> leer(String clave) {
    try {
      return Optional.of(
          cliente
              .getObjectAsBytes(g -> g.bucket(bucket).key(ClaveArchivo.validar(clave)))
              .asByteArray());
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    }
  }

  @Override
  public void eliminar(String clave) {
    cliente.deleteObject(d -> d.bucket(bucket).key(ClaveArchivo.validar(clave)));
  }

  @Override
  public String urlFirmada(String clave) {
    return firmador
        .presignGetObject(
            p ->
                p.signatureDuration(duracion)
                    .getObjectRequest(g -> g.bucket(bucket).key(ClaveArchivo.validar(clave))))
        .url()
        .toString();
  }

  @Override
  public void close() {
    firmador.close();
    cliente.close();
  }
}
