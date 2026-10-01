package co.italarm.api.compras.api;

import co.italarm.api.compras.aplicacion.CompraVista;
import co.italarm.api.compras.aplicacion.ListadoComprasVista;
import co.italarm.api.compras.aplicacion.ServicioCompras;
import co.italarm.api.compras.aplicacion.VistaPreviaCompraVista;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/compras")
@Tag(name = "Compras", description = "Compras a proveedores y su anulación (sección 3.6)")
public class CompraControlador {

  static final String OPERACION = "COMPRA";

  private final ServicioCompras servicio;

  public CompraControlador(ServicioCompras servicio) {
    this.servicio = servicio;
  }

  @PostMapping("/vista-previa")
  @Operation(
      summary = "Vista previa: costo actual → nuevo y regla por línea, sin guardar",
      description =
          "Es el valor oficial que muestra el frontend (BF-06). Errores: TASA_NO_DISPONIBLE,"
              + " COMPRA_PRODUCTO_REPETIDO, COMPRA_FECHA_FUTURA, PRODUCTO_NO_EXISTE.")
  public VistaPreviaCompraVista vistaPrevia(
      @Validated @RequestBody SolicitudVistaPreviaCompra solicitud) {
    return servicio.vistaPrevia(solicitud.fecha(), solicitud.moneda(), solicitud.aLineas());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar una compra y sumarla al inventario",
      description =
          "Con la misma Idempotency-Key devuelve la compra ya creada. Errores (400):"
              + " SERIALES_NO_COINCIDEN, CANTIDAD_INVALIDA, COMPRA_PRODUCTO_REPETIDO,"
              + " COMPRA_FECHA_FUTURA, COMPRA_COSTO_INVALIDO, PROVEEDOR_NO_EXISTE,"
              + " PRODUCTO_NO_EXISTE; (409) SERIAL_DUPLICADO; (422) TASA_NO_DISPONIBLE,"
              + " PRODUCTO_INACTIVO.")
  public CompraVista registrar(
      @Validated @RequestBody SolicitudCompra solicitud,
      @Parameter(description = "Clave única por intento de guardar (RT-07)")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String claveIdempotencia,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    ClaveIdempotencia clave =
        claveIdempotencia == null
            ? null
            : new ClaveIdempotencia(usuario.usuarioId(), OPERACION, claveIdempotencia);
    Long id = servicio.registrar(solicitud.aDatos(), usuario.usuarioId(), clave);
    return servicio.detalle(id);
  }

  @GetMapping
  @Operation(
      summary = "Listado paginado de compras con los totales del período",
      description =
          "Sin fechas, el mes en curso. Los totales no incluyen las anuladas. Orden por defecto:"
              + " más recientes primero.")
  public ListadoComprasVista listar(
      @RequestParam(required = false) Long proveedorId,
      @RequestParam(required = false) Long productoId,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @Parameter(description = "Incluir las anuladas en el listado (por defecto, sí)")
          @RequestParam(required = false)
          Boolean incluirAnuladas,
      @ParameterObject
          @PageableDefault(size = 20)
          @SortDefault.SortDefaults({
            @SortDefault(sort = "fecha", direction = Sort.Direction.DESC),
            @SortDefault(sort = "numero", direction = Sort.Direction.DESC)
          })
          Pageable pagina) {
    return servicio.listar(proveedorId, productoId, desde, hasta, incluirAnuladas, pagina);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de una compra, con si se puede anular y por qué no")
  public CompraVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PostMapping("/{id}/anular")
  @Operation(
      summary = "Anular una compra",
      description =
          "Solo si es el último movimiento de cada producto y sus seriales siguen en bodega."
              + " Errores: COMPRA_NO_ANULABLE (422), COMPRA_YA_ANULADA (409).")
  public CompraVista anular(
      @PathVariable Long id,
      @Validated @RequestBody SolicitudAnulacion solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.anular(id, solicitud.motivo(), usuario.usuarioId());
  }

  @PutMapping(path = "/{id}/factura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Subir o reemplazar la factura (JPEG, PNG, WebP o PDF; máximo 5 MB)",
      description = "Errores: ARCHIVO_TIPO_NO_PERMITIDO, ARCHIVO_DEMASIADO_GRANDE (400).")
  public CompraVista cambiarFactura(
      @PathVariable Long id, @RequestPart("archivo") MultipartFile archivo) throws IOException {
    return servicio.cambiarFactura(id, archivo.getBytes());
  }

  @DeleteMapping("/{id}/factura")
  @Operation(summary = "Quitar la factura")
  public CompraVista quitarFactura(@PathVariable Long id) {
    return servicio.quitarFactura(id);
  }
}
