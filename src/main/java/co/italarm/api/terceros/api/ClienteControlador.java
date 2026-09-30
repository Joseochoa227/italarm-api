package co.italarm.api.terceros.api;

import co.italarm.api.shared.api.Edicion;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.terceros.aplicacion.ClienteVista;
import co.italarm.api.terceros.aplicacion.ServicioClientes;
import co.italarm.api.terceros.dominio.TipoCliente;
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
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Instaladores y clientes finales (sección 3.10)")
public class ClienteControlador {

  private final ServicioClientes servicio;

  public ClienteControlador(ServicioClientes servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(
      summary = "Listado paginado de clientes",
      description = "Busca por nombre, documento, teléfono o ciudad. Orden por defecto: nombre.")
  public Pagina<ClienteVista> buscar(
      @RequestParam(required = false) TipoCliente tipo,
      @RequestParam(required = false) String buscar,
      @ParameterObject @PageableDefault(sort = "nombre", direction = Sort.Direction.ASC)
          Pageable pagina) {
    return Pagina.de(servicio.buscar(tipo, buscar, pagina), c -> c);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de un cliente")
  public ClienteVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Crear un cliente",
      description =
          "Errores: TELEFONO_INVALIDO, DOCUMENTO_INCOMPLETO (400); CLIENTE_DOCUMENTO_DUPLICADO"
              + " (409).")
  public ClienteVista crear(@Validated @RequestBody SolicitudCliente solicitud) {
    return servicio.crear(solicitud.aDatos());
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Editar un cliente",
      description = "Además: MODIFICADO_POR_OTRO_USUARIO (409).")
  public ClienteVista actualizar(
      @PathVariable Long id,
      @Validated({Default.class, Edicion.class}) @RequestBody SolicitudCliente solicitud) {
    return servicio.actualizar(id, solicitud.aDatos(), solicitud.version());
  }
}
