package co.italarm.api.tasas.api;

import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.tasas.aplicacion.ResultadoTrm;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.ServicioTrm;
import co.italarm.api.tasas.aplicacion.TasaVista;
import co.italarm.api.tasas.aplicacion.TasasVigentesVista;
import co.italarm.api.tasas.aplicacion.VariacionVista;
import co.italarm.api.tasas.dominio.ParMoneda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasas")
@Tag(name = "Tasas de cambio", description = "TRM automática y tasa del bolívar manual (3.5)")
public class TasaControlador {

  private final ServicioTasas servicio;
  private final ServicioTrm trm;

  public TasaControlador(ServicioTasas servicio, ServicioTrm trm) {
    this.servicio = servicio;
    this.trm = trm;
  }

  @GetMapping("/vigentes")
  @Operation(
      summary = "Tasas del día para el menú y la barra superior (RF-30)",
      description =
          "Si falta la tasa de hoy devuelve la última con esDeHoy=false y un aviso (RF-33).")
  public TasasVigentesVista vigentes() {
    return servicio.vigentes();
  }

  @GetMapping
  @Operation(summary = "Historial de tasas por par y rango de fechas (RF-34)")
  public Pagina<TasaVista> historial(
      @RequestParam(required = false) ParMoneda par,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @ParameterObject @PageableDefault(sort = "fecha", direction = Sort.Direction.DESC)
          Pageable pagina) {
    return Pagina.de(servicio.historial(par, desde, hasta, pagina), t -> t);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de una tasa con sus correcciones")
  public TasaVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PostMapping("/vista-previa")
  @Operation(summary = "Tasa anterior, nueva y variación antes de guardar (RF-35b); no guarda nada")
  public VariacionVista vistaPrevia(@Validated @RequestBody SolicitudVistaPrevia solicitud) {
    return servicio.vistaPrevia(solicitud.par(), solicitud.valor(), solicitud.tasaId());
  }

  @PostMapping("/ves")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar la tasa del bolívar de hoy (RF-29, RF-35)",
      description =
          "Errores: TASA_NO_CONFIRMADA, TASA_INVALIDA (400); TASA_YA_REGISTRADA (409);"
              + " TASA_VARIACION_NO_ACEPTADA (422).")
  public TasaVista registrarBolivar(
      @Validated @RequestBody SolicitudTasaManual solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.registrarManual(
        ParMoneda.USD_VES,
        solicitud.valor(),
        solicitud.confirmacion(),
        solicitud.aceptarVariacion(),
        usuario);
  }

  @PostMapping("/trm")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar la TRM de hoy a mano cuando falla la consulta automática (RF-33)",
      description =
          "Errores: los mismos del bolívar y TRM_AUTOMATICA_DISPONIBLE (409) si ya está la"
              + " oficial. Si más tarde llega la oficial, reemplaza a la manual (P-13).")
  public TasaVista registrarTrm(
      @Validated @RequestBody SolicitudTasaManual solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.registrarManual(
        ParMoneda.USD_COP,
        solicitud.valor(),
        solicitud.confirmacion(),
        solicitud.aceptarVariacion(),
        usuario);
  }

  @PostMapping("/trm/consultar")
  @Operation(
      summary = "Consultar ahora la TRM oficial de hoy",
      description = "Resultado: EXITO, FALLO u OMITIDA (ya estaba guardada).")
  public ResultadoTrm consultarTrm() {
    return trm.actualizarTrmDeHoy();
  }

  @PostMapping("/{id}/corregir")
  @Operation(
      summary = "Corregir una tasa (RF-36)",
      description =
          "Queda registro del valor anterior, el nuevo y el usuario. Errores: TASA_NO_CONFIRMADA,"
              + " TASA_SIN_CAMBIO (400); TASA_VARIACION_NO_ACEPTADA (422).")
  public TasaVista corregir(
      @PathVariable Long id,
      @Validated @RequestBody SolicitudCorreccionTasa solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.corregir(
        id,
        solicitud.valor(),
        solicitud.confirmacion(),
        solicitud.aceptarVariacion(),
        solicitud.motivo(),
        usuario);
  }
}
