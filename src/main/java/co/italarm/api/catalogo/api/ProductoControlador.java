package co.italarm.api.catalogo.api;

import co.italarm.api.catalogo.aplicacion.ProductoVista;
import co.italarm.api.catalogo.aplicacion.ServicioProductos;
import co.italarm.api.shared.api.Edicion;
import co.italarm.api.shared.api.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import java.io.IOException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo de productos (sección 3.3)")
public class ProductoControlador {

  private final ServicioProductos servicio;

  public ProductoControlador(ServicioProductos servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(
      summary = "Listado paginado de productos",
      description = "Busca por nombre, código o marca. Orden por defecto: nombre.")
  public Pagina<ProductoVista> buscar(
      @RequestParam(required = false) Long categoriaId,
      @Parameter(description = "true: solo activos; false: solo inactivos; vacío: todos")
          @RequestParam(required = false)
          Boolean activo,
      @RequestParam(required = false) String buscar,
      @ParameterObject @PageableDefault(sort = "nombre", direction = Sort.Direction.ASC)
          Pageable pagina) {
    return Pagina.de(servicio.buscar(categoriaId, activo, buscar, pagina), p -> p);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalle de un producto")
  public ProductoVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Crear un producto (queda con stock 0 y sin costo)",
      description =
          "Errores: PRODUCTO_CODIGO_DUPLICADO (409), CATEGORIA_NO_EXISTE, UNIDAD_NO_EXISTE,"
              + " CANTIDAD_INVALIDA (400).")
  public ProductoVista crear(@Validated @RequestBody SolicitudProducto solicitud) {
    return servicio.crear(solicitud.aDatos(), solicitud.categoriaId(), solicitud.unidadMedidaId());
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Editar un producto",
      description =
          "Errores: PRODUCTO_CODIGO_DUPLICADO, MODIFICADO_POR_OTRO_USUARIO (409);"
              + " PRODUCTO_CAMBIO_NO_PERMITIDO (422).")
  public ProductoVista actualizar(
      @PathVariable Long id,
      @Validated({Default.class, Edicion.class}) @RequestBody SolicitudProducto solicitud) {
    return servicio.actualizar(
        id,
        solicitud.aDatos(),
        solicitud.categoriaId(),
        solicitud.unidadMedidaId(),
        solicitud.version());
  }

  @PostMapping("/{id}/desactivar")
  @Operation(summary = "Desactivar: deja de aparecer para vender, conserva su historial")
  public ProductoVista desactivar(@PathVariable Long id) {
    return servicio.desactivar(id);
  }

  @PostMapping("/{id}/activar")
  @Operation(summary = "Activar un producto desactivado")
  public ProductoVista activar(@PathVariable Long id) {
    return servicio.activar(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Eliminar un producto sin movimientos",
      description = "Error: PRODUCTO_CON_MOVIMIENTOS (422).")
  public void eliminar(@PathVariable Long id) {
    servicio.eliminar(id);
  }

  @PutMapping(path = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Subir o reemplazar la foto (JPEG, PNG o WebP; máximo 5 MB)",
      description = "Errores: ARCHIVO_TIPO_NO_PERMITIDO, ARCHIVO_DEMASIADO_GRANDE (400).")
  public ProductoVista cambiarFoto(
      @PathVariable Long id, @RequestPart("archivo") MultipartFile archivo) throws IOException {
    return servicio.cambiarFoto(id, archivo.getBytes());
  }

  @DeleteMapping("/{id}/foto")
  @Operation(summary = "Quitar la foto")
  public ProductoVista quitarFoto(@PathVariable Long id) {
    return servicio.quitarFoto(id);
  }
}
