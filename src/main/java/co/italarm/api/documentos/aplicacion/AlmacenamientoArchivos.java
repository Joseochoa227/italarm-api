package co.italarm.api.documentos.aplicacion;

import java.util.Optional;

/**
 * Almacenamiento de fotos, logos y PDF (BP-14). Hay dos implementaciones: S3 (hosting) y disco
 * (desarrollo local). Los archivos nunca van en la base de datos.
 */
public interface AlmacenamientoArchivos {

  void guardar(String clave, byte[] contenido, String tipoContenido);

  Optional<byte[]> leer(String clave);

  void eliminar(String clave);

  /** Enlace de descarga firmado y de corta duración (9.5). */
  String urlFirmada(String clave);
}
