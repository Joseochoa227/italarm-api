package co.italarm.api.instalaciones.api;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.instalaciones.aplicacion.EnlaceComprobante;
import co.italarm.api.instalaciones.aplicacion.InstalacionVista;
import co.italarm.api.instalaciones.aplicacion.ListadoInstalacionesVista;
import co.italarm.api.instalaciones.aplicacion.ServicioInstalaciones;
import co.italarm.api.instalaciones.aplicacion.VistaPreviaInstalacionVista;
import co.italarm.api.instalaciones.dominio.GrupoFoto;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.EstadoGarantia;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/instalaciones")
@Tag(name = "Instalaciones", description = "Instalaciones, fotos y su anulación (sección 3.13)")
public class InstalacionControlador {

  static final String OPERACION = "INSTALACION";

  private final ServicioInstalaciones servicio;

  public InstalacionControlador(ServicioInstalaciones servicio) {
    this.servicio = servicio;
  }

  @PostMapping("/vista-previa")
  @Operation(
      summary = "Vista previa: material, cobro, utilidad y garantías, sin guardar",
      description =
          "Es el valor oficial que muestra el frontend (BF-06). Si una línea no tiene stock"
              + " suficiente, trae avisoStock y puedeGuardar=false.")
  public VistaPreviaInstalacionVista vistaPrevia(
      @Validated @RequestBody SolicitudInstalacion solicitud) {
    return servicio.vistaPrevia(solicitud.aDatos());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar una instalación y descontar el material del inventario",
      description =
          "Con la misma Idempotency-Key devuelve la instalación ya creada. Errores (400):"
              + " INSTALACION_FECHA_FUTURA, INSTALACION_VACIA, INSTALACION_SIN_TECNICOS,"
              + " INSTALACION_GARANTIA_INVALIDA, INSTALACION_PRODUCTO_REPETIDO,"
              + " TECNICO_NO_EXISTE, CLIENTE_NO_EXISTE, PRODUCTO_NO_EXISTE, CANTIDAD_INVALIDA,"
              + " SERIALES_NO_COINCIDEN, DESCUENTO_INVALIDO, PRECIO_INVALIDO; (422)"
              + " STOCK_INSUFICIENTE, SERIAL_NO_DISPONIBLE, PRODUCTO_INACTIVO,"
              + " TASA_NO_DISPONIBLE.")
  public InstalacionVista registrar(
      @Validated @RequestBody SolicitudInstalacion solicitud,
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
      summary = "Listado paginado de instalaciones con los totales del período",
      description =
          "Sin fechas, el mes en curso. Los totales no incluyen las anuladas. Filtrar por estado"
              + " de garantía excluye las anuladas. Orden por defecto: más recientes primero.")
  public ListadoInstalacionesVista listar(
      @RequestParam(required = false) Long clienteId,
      @RequestParam(required = false) Long tecnicoId,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @Parameter(description = "Estado de la garantía de mano de obra")
          @RequestParam(required = false)
          EstadoGarantia estadoGarantia,
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
    return servicio.listar(
        clienteId, tecnicoId, desde, hasta, estadoGarantia, incluirAnuladas, pagina);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle con material, seriales, fotos, garantías y cobro")
  public InstalacionVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Corregir dirección, descripción, técnicos, condiciones y observaciones",
      description =
          "Los valores, la fecha y el plazo de garantía no se editan (RF-122, P-44). Error:"
              + " MODIFICADO_POR_OTRO_USUARIO (409).")
  public InstalacionVista actualizar(
      @PathVariable Long id, @Validated @RequestBody SolicitudEdicionInstalacion solicitud) {
    return servicio.actualizar(id, solicitud.aDatos(), solicitud.version());
  }

  @PostMapping("/{id}/anular")
  @Operation(
      summary = "Anular una instalación",
      description =
          "El material vuelve al costo vigente y los seriales a bodega; las fotos se conservan."
              + " Error: INSTALACION_YA_ANULADA (409).")
  public InstalacionVista anular(
      @PathVariable Long id,
      @Validated @RequestBody SolicitudAnulacionInstalacion solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.anular(id, solicitud.motivo(), usuario.usuarioId());
  }

  @PostMapping(path = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Agregar una foto a un grupo (JPEG, PNG o WebP; máximo 5 MB)",
      description =
          "Máximo 30 por grupo (P-42). Errores: ARCHIVO_TIPO_NO_PERMITIDO,"
              + " ARCHIVO_DEMASIADO_GRANDE (400); FOTOS_MAXIMAS (422).")
  public InstalacionVista agregarFoto(
      @PathVariable Long id,
      @RequestParam GrupoFoto grupo,
      @RequestPart("archivo") MultipartFile archivo)
      throws IOException {
    return servicio.agregarFoto(id, grupo, archivo.getBytes());
  }

  @DeleteMapping("/{id}/fotos/{fotoId}")
  @Operation(summary = "Quitar una foto")
  public InstalacionVista quitarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
    return servicio.quitarFoto(id, fotoId);
  }

  @GetMapping(path = "/{id}/comprobante", produces = MediaType.APPLICATION_PDF_VALUE)
  @Operation(summary = "Descargar el comprobante de instalación en PDF (RF-133)")
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
      description = "Vence a los 30 días (P-33). Trae el mensaje y el enlace wa.me del cliente.")
  public EnlaceComprobante enlace(
      @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.crearEnlace(id, usuario.usuarioId());
  }
}
