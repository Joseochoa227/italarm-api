package co.italarm.api.ventas.api;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.ventas.aplicacion.EnlaceComprobanteVista;
import co.italarm.api.ventas.aplicacion.ListadoVentasVista;
import co.italarm.api.ventas.aplicacion.ServicioVentas;
import co.italarm.api.ventas.aplicacion.VentaVista;
import co.italarm.api.ventas.aplicacion.VistaPreviaVentaVista;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ventas")
@Tag(name = "Ventas", description = "Ventas de material y su anulación (sección 3.12)")
public class VentaControlador {

  static final String OPERACION = "VENTA";

  private final ServicioVentas servicio;

  public VentaControlador(ServicioVentas servicio) {
    this.servicio = servicio;
  }

  @PostMapping("/vista-previa")
  @Operation(
      summary = "Vista previa: precios, disponibilidad, costos y utilidad, sin guardar",
      description =
          "Es el valor oficial que muestra el frontend (BF-06). Si una línea no tiene stock"
              + " suficiente, trae avisoStock y puedeGuardar=false. Errores: TASA_NO_DISPONIBLE"
              + " (422), DESCUENTO_INVALIDO, VENTA_PRODUCTO_REPETIDO, CLIENTE_NO_EXISTE (400).")
  public VistaPreviaVentaVista vistaPrevia(@Validated @RequestBody SolicitudVenta solicitud) {
    return servicio.vistaPrevia(solicitud.aDatos());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar una venta y descontarla del inventario",
      description =
          "La fecha es hoy. Con la misma Idempotency-Key devuelve la venta ya creada. Errores"
              + " (400): CANTIDAD_INVALIDA, SERIALES_NO_COINCIDEN, DESCUENTO_INVALIDO,"
              + " VENTA_PRODUCTO_REPETIDO, VENTA_PRECIO_INVALIDO, CLIENTE_NO_EXISTE,"
              + " PRODUCTO_NO_EXISTE; (422) STOCK_INSUFICIENTE, SERIAL_NO_DISPONIBLE,"
              + " PRODUCTO_INACTIVO, TASA_NO_DISPONIBLE.")
  public VentaVista registrar(
      @Validated @RequestBody SolicitudVenta solicitud,
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
      summary = "Listado paginado de ventas con los totales del período",
      description =
          "Sin fechas, el mes en curso. Los totales no incluyen las anuladas. Orden por defecto:"
              + " más recientes primero.")
  public ListadoVentasVista listar(
      @RequestParam(required = false) Long clienteId,
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
    return servicio.listar(clienteId, productoId, desde, hasta, incluirAnuladas, pagina);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de una venta con seriales, garantía y utilidad")
  public VentaVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Cambiar las observaciones y las monedas adicionales del comprobante",
      description =
          "Los valores de la venta no se editan (RF-70). Error: MODIFICADO_POR_OTRO_USUARIO (409).")
  public VentaVista actualizar(
      @PathVariable Long id, @Validated @RequestBody SolicitudEdicionVenta solicitud) {
    return servicio.actualizar(
        id, solicitud.observaciones(), solicitud.monedasComprobante(), solicitud.version());
  }

  @GetMapping(path = "/{id}/comprobante", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "Descargar el comprobante de venta en PDF (RF-133)")
  public ResponseEntity<byte[]> comprobante(@PathVariable Long id) {
    ArchivoGenerado archivo = servicio.comprobante(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
        .contentType(MediaType.APPLICATION_PDF)
        .body(archivo.contenido());
  }

  @PostMapping("/{id}/enlace")
  @Operation(
      summary = "Crear el enlace público del comprobante para WhatsApp",
      description =
          "Vence a los 30 días (P-33). Trae el mensaje listo y el enlace wa.me con el número del"
              + " cliente (RF-134).")
  public EnlaceComprobanteVista enlace(
      @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.crearEnlace(id, usuario.usuarioId());
  }

  @PostMapping("/{id}/anular")
  @Operation(
      summary = "Anular una venta",
      description =
          "El material vuelve al costo vigente y los seriales a bodega. Error: VENTA_YA_ANULADA"
              + " (409).")
  public VentaVista anular(
      @PathVariable Long id,
      @Validated @RequestBody SolicitudAnulacionVenta solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.anular(id, solicitud.motivo(), usuario.usuarioId());
  }
}
