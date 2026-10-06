package co.italarm.api.garantias.api;

import co.italarm.api.garantias.aplicacion.GarantiaVista;
import co.italarm.api.garantias.aplicacion.ReclamoVista;
import co.italarm.api.garantias.aplicacion.ServicioGarantias;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.EstadoGarantia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/garantias")
@Tag(name = "Garantías", description = "Consulta de garantías y reclamos (sección 3.14)")
public class GarantiaControlador {

  private final ServicioGarantias servicio;

  public GarantiaControlador(ServicioGarantias servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(
      summary = "Consulta de garantías de ventas e instalaciones",
      description =
          "Mano de obra de cada instalación y cada equipo con serial vendido o instalado, de"
              + " documentos no anulados. Ordenadas de la que vence primero a la última.")
  public Pagina<GarantiaVista> consultar(
      @RequestParam(required = false) EstadoGarantia estado,
      @RequestParam(required = false) Long clienteId,
      @Parameter(description = "INSTALACION o VENTA") @RequestParam(required = false) String tipo,
      @Parameter(description = "Parte del número de serie") @RequestParam(required = false)
          String serial,
      @ParameterObject @PageableDefault(size = 20) Pageable pagina) {
    return Pagina.de(servicio.consultar(estado, clienteId, tipo, serial, pagina), g -> g);
  }

  @PostMapping("/reclamos")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar un reclamo sobre una instalación o un serial",
      description =
          "Si la garantía ya venció se registra igual, con enGarantia=false (P-45). Errores:"
              + " RECLAMO_INVALIDO (400), RECURSO_NO_ENCONTRADO (404).")
  public ReclamoVista registrar(@Validated @RequestBody SolicitudReclamo solicitud) {
    return servicio.registrarReclamo(solicitud.aDatos());
  }

  @PutMapping("/reclamos/{id}")
  @Operation(
      summary = "Escribir o corregir la solución de un reclamo",
      description = "Error: MODIFICADO_POR_OTRO_USUARIO (409).")
  public ReclamoVista cambiarSolucion(
      @PathVariable Long id, @Validated @RequestBody SolicitudSolucion solicitud) {
    return servicio.cambiarSolucion(id, solicitud.solucion(), solicitud.version());
  }

  @GetMapping("/reclamos")
  @Operation(summary = "Reclamos registrados, del más reciente al más antiguo")
  public List<ReclamoVista> reclamos(
      @RequestParam(required = false) Long instalacionId,
      @RequestParam(required = false) Long serialId,
      @RequestParam(required = false) Long clienteId) {
    return servicio.reclamos(instalacionId, serialId, clienteId);
  }
}
