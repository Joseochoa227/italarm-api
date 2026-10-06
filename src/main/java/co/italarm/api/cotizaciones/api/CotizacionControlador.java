package co.italarm.api.cotizaciones.api;

import co.italarm.api.cotizaciones.aplicacion.ConversionCotizacionVista;
import co.italarm.api.cotizaciones.aplicacion.CotizacionResumenVista;
import co.italarm.api.cotizaciones.aplicacion.CotizacionVista;
import co.italarm.api.cotizaciones.aplicacion.EnlaceCotizacionVista;
import co.italarm.api.cotizaciones.aplicacion.SeguimientoCotizacionVista;
import co.italarm.api.cotizaciones.aplicacion.ServicioCotizaciones;
import co.italarm.api.cotizaciones.aplicacion.VistaPreviaCotizacionVista;
import co.italarm.api.cotizaciones.dominio.EstadoCotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.shared.api.Edicion;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
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
@RequestMapping("/api/v1/cotizaciones")
@Tag(
    name = "Cotizaciones",
    description = "Cotizaciones, seguimiento y conversión en venta o instalación (sección 3.11)")
public class CotizacionControlador {

  static final String OPERACION = "COTIZACION";

  private final ServicioCotizaciones servicio;

  public CotizacionControlador(ServicioCotizaciones servicio) {
    this.servicio = servicio;
  }

  @PostMapping("/vista-previa")
  @Operation(
      summary = "Vista previa: precios, costos con las tasas de hoy y de compra, y utilidad",
      description =
          "No guarda nada. Es el valor oficial que muestra el frontend y con el que dibuja la"
              + " vista previa del PDF (RF-85). El stock es solo informativo (RF-86). Errores:"
              + " TASA_NO_DISPONIBLE, PRODUCTO_INACTIVO (422); DESCUENTO_INVALIDO,"
              + " COTIZACION_* , CLIENTE_NO_EXISTE (400).")
  public VistaPreviaCotizacionVista vistaPrevia(
      @Validated @RequestBody SolicitudCotizacion solicitud) {
    return servicio.vistaPrevia(solicitud.aDatos());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar una cotización en Borrador",
      description =
          "La fecha es hoy y no toca el inventario (RN-11). Con la misma Idempotency-Key devuelve"
              + " la cotización ya creada. Errores (400): COTIZACION_SIN_LINEAS,"
              + " COTIZACION_VACIA, COTIZACION_SIN_DESCRIPCION, COTIZACION_MANO_OBRA_EN_VENTA,"
              + " COTIZACION_VALIDEZ_INVALIDA, COTIZACION_PRODUCTO_REPETIDO, DESCUENTO_INVALIDO,"
              + " PRECIO_INVALIDO, CLIENTE_NO_EXISTE, PRODUCTO_NO_EXISTE; (422)"
              + " PRODUCTO_INACTIVO, TASA_NO_DISPONIBLE.")
  public CotizacionVista registrar(
      @Validated @RequestBody SolicitudCotizacion solicitud,
      @Parameter(description = "Clave única por intento de guardar (RT-07)")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String claveIdempotencia,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    ClaveIdempotencia clave =
        claveIdempotencia == null
            ? null
            : new ClaveIdempotencia(usuario.usuarioId(), OPERACION, claveIdempotencia);
    return servicio.detalle(servicio.registrar(solicitud.aDatos(), clave));
  }

  @GetMapping
  @Operation(
      summary = "Listado paginado de cotizaciones",
      description =
          "Filtros opcionales. porVencer=true trae solo las En evaluación que vencen en 3 días o"
              + " menos (RF-91). Orden por defecto: más recientes primero.")
  public Pagina<CotizacionResumenVista> listar(
      @RequestParam(required = false) EstadoCotizacion estado,
      @RequestParam(required = false) Long clienteId,
      @RequestParam(required = false) TipoCotizacion tipo,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @RequestParam(required = false) Boolean porVencer,
      @ParameterObject
          @PageableDefault(size = 20)
          @SortDefault.SortDefaults({
            @SortDefault(sort = "fecha", direction = Sort.Direction.DESC),
            @SortDefault(sort = "numero", direction = Sort.Direction.DESC)
          })
          Pageable pagina) {
    return servicio.listar(estado, clienteId, tipo, desde, hasta, porVencer, pagina);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de una cotización con sus versiones anteriores")
  public CotizacionVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Editar una cotización",
      description =
          "En Borrador se reemplaza; En evaluación se guarda como nueva versión (COT-0001 v2) y"
              + " se conserva la anterior (RF-88). La fecha y el vencimiento se recalculan desde"
              + " hoy. Errores: TRANSICION_NO_PERMITIDA (422), MODIFICADO_POR_OTRO_USUARIO (409).")
  public CotizacionVista editar(
      @PathVariable Long id,
      @Validated({Default.class, Edicion.class}) @RequestBody SolicitudCotizacion solicitud) {
    return servicio.editar(id, solicitud.aDatos(), solicitud.version());
  }

  @PostMapping("/{id}/enviar")
  @Operation(
      summary = "Marcar como enviada (pasa a En evaluación)",
      description =
          "El frontend la llama también al pulsar Descargar PDF (P-50). Reenviar una En evaluación"
              + " solo actualiza la fecha de envío. Error: TRANSICION_NO_PERMITIDA (422).")
  public CotizacionVista enviar(@PathVariable Long id) {
    return servicio.enviar(id);
  }

  @PostMapping("/{id}/aprobar")
  @Operation(
      summary = "Cliente aprobó",
      description = "Desde Borrador o En evaluación (P-48). Error: TRANSICION_NO_PERMITIDA (422).")
  public CotizacionVista aprobar(@PathVariable Long id) {
    return servicio.aprobar(id);
  }

  @PostMapping("/{id}/rechazar")
  @Operation(
      summary = "Marcar como rechazada, con motivo opcional",
      description =
          "Desde Borrador, En evaluación o Aprobada. Error: TRANSICION_NO_PERMITIDA (422).")
  public CotizacionVista rechazar(
      @PathVariable Long id, @Validated @RequestBody(required = false) SolicitudRechazo solicitud) {
    return servicio.rechazar(
        id,
        solicitud == null ? null : solicitud.motivo(),
        solicitud == null ? null : solicitud.detalle());
  }

  @PostMapping("/{id}/duplicar")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Duplicar en una cotización nueva en Borrador",
      description =
          "Copia productos, cantidades, precios cotizados, mano de obra y descripción, con fecha"
              + " y tasas de hoy (RF-87, P-52). Errores: PRODUCTO_INACTIVO, TASA_NO_DISPONIBLE"
              + " (422).")
  public CotizacionVista duplicar(
      @PathVariable Long id,
      @Parameter(description = "Clave única por intento de duplicar (RT-07)")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String claveIdempotencia,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.detalle(servicio.duplicar(id, usuario.usuarioId(), claveIdempotencia));
  }

  @GetMapping("/{id}/conversion")
  @Operation(
      summary = "Datos para convertir una cotización aprobada en venta o instalación",
      description =
          "Devuelve el formulario precargado y los avisos de precio, costo y stock cambiados"
              + " (RF-94, RF-96). La conversión se guarda con POST /ventas o POST /instalaciones"
              + " enviando cotizacionId. Error: COTIZACION_NO_CONVERTIBLE (422).")
  public ConversionCotizacionVista conversion(@PathVariable Long id) {
    return servicio.conversion(id);
  }

  @GetMapping(path = "/{id}/comprobante", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(
      summary = "Descargar la cotización en PDF (RF-133)",
      description = "No cambia el estado: para marcarla enviada, llamar a /enviar (P-50).")
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
      summary = "Crear el enlace público del PDF para WhatsApp",
      description =
          "Vence a los 30 días (P-33). Si la cotización estaba en Borrador, queda En evaluación"
              + " (P-50).")
  public EnlaceCotizacionVista enlace(
      @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.crearEnlace(id, usuario.usuarioId());
  }

  @GetMapping("/{id}/seguimiento")
  @Operation(summary = "Mensaje de seguimiento por WhatsApp (RF-92)")
  public SeguimientoCotizacionVista seguimiento(@PathVariable Long id) {
    return servicio.seguimiento(id);
  }
}
