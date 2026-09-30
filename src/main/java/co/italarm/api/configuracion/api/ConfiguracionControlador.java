package co.italarm.api.configuracion.api;

import co.italarm.api.configuracion.aplicacion.ConfiguracionVista;
import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/configuracion")
@Tag(name = "Configuración", description = "Datos de la empresa y valores por defecto (3.17)")
public class ConfiguracionControlador {

  private final ServicioConfiguracion servicio;

  public ConfiguracionControlador(ServicioConfiguracion servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(summary = "Configuración actual")
  public ConfiguracionVista obtener() {
    return servicio.obtener();
  }

  @PutMapping
  @Operation(
      summary = "Editar la configuración",
      description =
          "Errores: CONFIGURACION_INVALIDA (400), MODIFICADO_POR_OTRO_USUARIO (409). Validez: 8,"
              + " 15 o 30 días; garantías: 1 a 3 meses; límite de variación: más de 0 % y hasta"
              + " 100 %.")
  public ConfiguracionVista actualizar(@Validated @RequestBody SolicitudConfiguracion solicitud) {
    return servicio.actualizar(solicitud.aDatos(), solicitud.version());
  }

  @PutMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Subir o reemplazar el logo (JPEG, PNG o WebP; máximo 5 MB)",
      description = "Errores: ARCHIVO_TIPO_NO_PERMITIDO, ARCHIVO_DEMASIADO_GRANDE (400).")
  public ConfiguracionVista cambiarLogo(@RequestPart("archivo") MultipartFile archivo)
      throws IOException {
    return servicio.cambiarLogo(archivo.getBytes());
  }

  @DeleteMapping("/logo")
  @Operation(summary = "Quitar el logo")
  public ConfiguracionVista quitarLogo() {
    return servicio.quitarLogo();
  }
}
