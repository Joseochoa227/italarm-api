package co.italarm.api.cargainicial.api;

import co.italarm.api.cargainicial.aplicacion.ResultadoCargaVista;
import co.italarm.api.cargainicial.aplicacion.ServicioCargaInicial;
import co.italarm.api.inventario.aplicacion.CargaInicialVista;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/carga-inicial")
@Tag(
    name = "Carga inicial",
    description =
        "Productos, inventario inicial, clientes y proveedores desde Excel (sección 3.18)")
public class CargaInicialControlador {

  static final String OPERACION = "CARGA_INICIAL";
  static final String TIPO_XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final ServicioCargaInicial servicio;

  public CargaInicialControlador(ServicioCargaInicial servicio) {
    this.servicio = servicio;
  }

  @GetMapping(path = "/plantilla", produces = TIPO_XLSX)
  @Operation(summary = "Descargar la plantilla .xlsx")
  public ResponseEntity<byte[]> plantilla() {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("plantilla-carga-inicial.xlsx")
                .build()
                .toString())
        .contentType(MediaType.parseMediaType(TIPO_XLSX))
        .body(servicio.plantilla());
  }

  @PostMapping(path = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Revisar el archivo sin guardar nada",
      description =
          "Devuelve los errores por hoja y fila y un resumen. Errores: ARCHIVO_TIPO_NO_PERMITIDO,"
              + " ARCHIVO_DEMASIADO_GRANDE (400).")
  public ResultadoCargaVista validar(@RequestPart("archivo") MultipartFile archivo)
      throws IOException {
    return servicio.validar(archivo.getBytes());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Guardar la carga si el archivo no tiene errores",
      description =
          "Crea los productos, clientes y proveedores y el documento II-00N. Con un solo error no"
              + " guarda nada: CARGA_INICIAL_CON_ERRORES (400) con la lista en 'errores'. Además:"
              + " ARCHIVO_TIPO_NO_PERMITIDO, ARCHIVO_DEMASIADO_GRANDE (400).")
  public CargaInicialVista confirmar(
      @RequestPart("archivo") MultipartFile archivo,
      @Parameter(description = "Clave única por intento de guardar (RT-07)")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String claveIdempotencia,
      @AuthenticationPrincipal UsuarioAutenticado usuario)
      throws IOException {
    ClaveIdempotencia clave =
        claveIdempotencia == null
            ? null
            : new ClaveIdempotencia(usuario.usuarioId(), OPERACION, claveIdempotencia);
    return servicio.confirmar(
        archivo.getBytes(), archivo.getOriginalFilename(), usuario.usuarioId(), clave);
  }

  @GetMapping
  @Operation(summary = "Cargas realizadas, de la más reciente a la más antigua")
  public List<CargaInicialVista> listar() {
    return servicio.listar();
  }
}
