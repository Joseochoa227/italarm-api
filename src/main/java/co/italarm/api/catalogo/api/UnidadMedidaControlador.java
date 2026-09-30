package co.italarm.api.catalogo.api;

import co.italarm.api.catalogo.aplicacion.ServicioUnidadesMedida;
import co.italarm.api.catalogo.aplicacion.UnidadMedidaVista;
import co.italarm.api.shared.api.Edicion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/unidades-medida")
@Tag(name = "Unidades de medida", description = "Unidad, Metro, Par… (RF-148, P-09)")
public class UnidadMedidaControlador {

  private final ServicioUnidadesMedida servicio;

  public UnidadMedidaControlador(ServicioUnidadesMedida servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(summary = "Todas las unidades de medida")
  public List<UnidadMedidaVista> listar() {
    return servicio.listar();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crear una unidad de medida", description = "Error: UNIDAD_DUPLICADA (409).")
  public UnidadMedidaVista crear(@Validated @RequestBody SolicitudUnidadMedida solicitud) {
    return servicio.crear(solicitud.nombre(), solicitud.abreviatura(), solicitud.admiteDecimales());
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Editar una unidad de medida",
      description =
          "Errores: UNIDAD_DUPLICADA, MODIFICADO_POR_OTRO_USUARIO (409); UNIDAD_EN_USO (422) si se"
              + " cambia admiteDecimales y la usan productos.")
  public UnidadMedidaVista actualizar(
      @PathVariable Long id,
      @Validated({Default.class, Edicion.class}) @RequestBody SolicitudUnidadMedida solicitud) {
    return servicio.actualizar(
        id,
        solicitud.nombre(),
        solicitud.abreviatura(),
        solicitud.admiteDecimales(),
        solicitud.version());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Eliminar una unidad que no usa ningún producto",
      description = "Error: UNIDAD_EN_USO (422).")
  public void eliminar(@PathVariable Long id) {
    servicio.eliminar(id);
  }
}
