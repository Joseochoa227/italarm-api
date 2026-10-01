package co.italarm.api.inventario.api;

import co.italarm.api.inventario.aplicacion.AjusteVista;
import co.italarm.api.inventario.aplicacion.ServicioAjustes;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ajustes")
@Tag(
    name = "Ajustes",
    description = "Ajustes de inventario (RF-58 a RF-62). No se editan ni se anulan (P-24).")
public class AjusteControlador {

  static final String OPERACION = "AJUSTE";

  private final ServicioAjustes servicio;

  public AjusteControlador(ServicioAjustes servicio) {
    this.servicio = servicio;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar un ajuste de entrada (cantidad positiva) o de salida (negativa)",
      description =
          "La entrada entra al costo vigente; si el producto nunca tuvo costo, exige"
              + " costoUnitarioUsd. Con la misma Idempotency-Key devuelve el ajuste ya creado."
              + " Errores (400): AJUSTE_INVALIDO, COSTO_REQUERIDO, CANTIDAD_INVALIDA,"
              + " SERIALES_NO_COINCIDEN, PRODUCTO_NO_EXISTE; (409) SERIAL_DUPLICADO; (422)"
              + " STOCK_INSUFICIENTE, SERIAL_NO_DISPONIBLE.")
  public AjusteVista registrar(
      @Validated @RequestBody SolicitudAjuste solicitud,
      @Parameter(description = "Clave única por intento de guardar (RT-07)")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String claveIdempotencia,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    ClaveIdempotencia clave =
        claveIdempotencia == null
            ? null
            : new ClaveIdempotencia(usuario.usuarioId(), OPERACION, claveIdempotencia);
    return servicio.detalle(servicio.registrar(solicitud.aDatos(), usuario.usuarioId(), clave));
  }

  @GetMapping
  @Operation(
      summary = "Listado paginado de ajustes",
      description = "Orden por defecto: más recientes primero.")
  public Pagina<AjusteVista> listar(
      @RequestParam(required = false) Long productoId,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @ParameterObject
          @PageableDefault(size = 20)
          @SortDefault.SortDefaults({
            @SortDefault(sort = "fecha", direction = Sort.Direction.DESC),
            @SortDefault(sort = "numero", direction = Sort.Direction.DESC)
          })
          Pageable pagina) {
    return Pagina.de(servicio.listar(productoId, desde, hasta, pagina), a -> a);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de un ajuste")
  public AjusteVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }
}
