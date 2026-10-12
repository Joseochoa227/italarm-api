package co.italarm.api.documentos.api;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Respuesta para descargar un archivo generado (PDF o Excel). */
public final class Descargas {

  /** Tipo de los archivos de Excel, para {@code produces}. */
  public static final String EXCEL =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private Descargas() {}

  public static ResponseEntity<byte[]> adjunto(ArchivoGenerado archivo) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
        .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
        .body(archivo.contenido());
  }
}
