package co.italarm.api.documentos.api;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ServicioEnlacesComprobante;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comprobantes")
@Tag(
    name = "Comprobantes",
    description = "PDF compartidos por WhatsApp con un enlace público (RF-134)")
public class ComprobanteControlador {

  private final ServicioEnlacesComprobante enlaces;

  public ComprobanteControlador(ServicioEnlacesComprobante enlaces) {
    this.enlaces = enlaces;
  }

  @GetMapping(path = "/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
  @SecurityRequirements
  @Operation(
      summary = "Descargar un comprobante con su enlace público, sin sesión",
      description = "El enlace vence a los 30 días (P-33). Error: RECURSO_NO_ENCONTRADO (404).")
  public ResponseEntity<byte[]> descargar(@PathVariable String token) {
    ArchivoGenerado archivo = enlaces.descargar(token);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(archivo.nombre()).build().toString())
        .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
        .body(archivo.contenido());
  }
}
