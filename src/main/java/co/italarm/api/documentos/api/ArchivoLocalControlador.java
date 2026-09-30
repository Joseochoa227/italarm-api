package co.italarm.api.documentos.api;

import co.italarm.api.documentos.aplicacion.ArchivoDescargado;
import co.italarm.api.documentos.aplicacion.ServicioDescargaLocal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Descarga de archivos con enlace firmado. Solo en modo disco (desarrollo local). */
@RestController
@RequestMapping("/api/v1/archivos")
@ConditionalOnProperty(
    name = "italarm.almacenamiento.tipo",
    havingValue = "disco",
    matchIfMissing = true)
@Tag(name = "Archivos", description = "Solo en desarrollo local: en el hosting los sirve S3")
public class ArchivoLocalControlador {

  private final ServicioDescargaLocal servicio;

  public ArchivoLocalControlador(ServicioDescargaLocal servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @SecurityRequirements
  @Operation(summary = "Descargar un archivo con un enlace firmado")
  public ResponseEntity<byte[]> descargar(
      @RequestParam String clave, @RequestParam long expira, @RequestParam String firma) {
    ArchivoDescargado archivo = servicio.descargar(clave, expira, firma);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(15)).cachePrivate())
        .body(archivo.contenido());
  }
}
