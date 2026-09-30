package co.italarm.api.terceros.api;

import co.italarm.api.shared.api.Edicion;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.terceros.aplicacion.ProveedorVista;
import co.italarm.api.terceros.aplicacion.ServicioProveedores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proveedores")
@Tag(name = "Proveedores", description = "Proveedores y su moneda habitual (sección 3.6)")
public class ProveedorControlador {

  private final ServicioProveedores servicio;

  public ProveedorControlador(ServicioProveedores servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(
      summary = "Listado paginado de proveedores",
      description = "Busca por nombre, NIT o ciudad. Orden por defecto: nombre.")
  public Pagina<ProveedorVista> buscar(
      @RequestParam(required = false) String buscar,
      @ParameterObject @PageableDefault(sort = "nombre", direction = Sort.Direction.ASC)
          Pageable pagina) {
    return Pagina.de(servicio.buscar(buscar, pagina), p -> p);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de un proveedor")
  public ProveedorVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crear un proveedor", description = "Error: TELEFONO_INVALIDO (400).")
  public ProveedorVista crear(@Validated @RequestBody SolicitudProveedor solicitud) {
    return servicio.crear(solicitud.aDatos());
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Editar un proveedor",
      description = "Además: MODIFICADO_POR_OTRO_USUARIO (409).")
  public ProveedorVista actualizar(
      @PathVariable Long id,
      @Validated({Default.class, Edicion.class}) @RequestBody SolicitudProveedor solicitud) {
    return servicio.actualizar(id, solicitud.aDatos(), solicitud.version());
  }
}
