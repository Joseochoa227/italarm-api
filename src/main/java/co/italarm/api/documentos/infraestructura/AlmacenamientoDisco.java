package co.italarm.api.documentos.infraestructura;

import co.italarm.api.documentos.aplicacion.AlmacenamientoArchivos;
import co.italarm.api.documentos.dominio.ClaveArchivo;
import co.italarm.api.documentos.dominio.EnlaceFirmado;
import co.italarm.api.documentos.dominio.FirmaEnlace;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/** Almacenamiento en una carpeta local, para desarrollo sin cuenta S3. */
public class AlmacenamientoDisco implements AlmacenamientoArchivos {

  private final Path raiz;
  private final String urlPublica;
  private final FirmaEnlace firma;
  private final Duration duracion;
  private final Clock reloj;

  public AlmacenamientoDisco(
      Path raiz, String urlPublica, FirmaEnlace firma, Duration duracion, Clock reloj) {
    this.raiz = raiz.toAbsolutePath().normalize();
    this.urlPublica =
        urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
    this.firma = firma;
    this.duracion = duracion;
    this.reloj = reloj;
  }

  @Override
  public void guardar(String clave, byte[] contenido, String tipoContenido) {
    Path destino = ruta(clave);
    try {
      Files.createDirectories(destino.getParent());
      Path temporal = Files.createTempFile(destino.getParent(), ".subida-", ".tmp");
      Files.write(temporal, contenido);
      Files.move(
          temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo guardar el archivo " + clave, e);
    }
  }

  @Override
  public Optional<byte[]> leer(String clave) {
    Path origen = ruta(clave);
    if (!Files.isRegularFile(origen)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Files.readAllBytes(origen));
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo leer el archivo " + clave, e);
    }
  }

  @Override
  public void eliminar(String clave) {
    try {
      Files.deleteIfExists(ruta(clave));
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo eliminar el archivo " + clave, e);
    }
  }

  @Override
  public String urlFirmada(String clave) {
    EnlaceFirmado enlace = firma.firmar(ClaveArchivo.validar(clave), reloj.instant(), duracion);
    return urlPublica
        + "/api/v1/archivos?clave="
        + URLEncoder.encode(clave, StandardCharsets.UTF_8)
        + "&expira="
        + enlace.expira()
        + "&firma="
        + enlace.firma();
  }

  /** Ruta del archivo, garantizando que quede dentro de la carpeta raíz. */
  private Path ruta(String clave) {
    Path ruta = raiz.resolve(ClaveArchivo.validar(clave)).normalize();
    if (!ruta.startsWith(raiz)) {
      throw new IllegalArgumentException("Clave de archivo fuera del almacenamiento: " + clave);
    }
    return ruta;
  }
}
