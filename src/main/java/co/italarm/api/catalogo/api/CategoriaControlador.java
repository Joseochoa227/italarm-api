package co.italarm.api.catalogo.api;

import co.italarm.api.catalogo.aplicacion.CategoriaVista;
import co.italarm.api.catalogo.aplicacion.ServicioCategorias;
import co.italarm.api.shared.api.Edicion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Categorías de productos (RF-15)")
public class CategoriaControlador {

  private final ServicioCategorias servicio;

  public CategoriaControlador(ServicioCategorias servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(summary = "Todas las categorías, con su cantidad de productos")
  public List<CategoriaVista> listar() {
    return servicio.listar();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crear una categoría", description = "Error: CATEGORIA_DUPLICADA (409).")
  public CategoriaVista crear(@Validated @RequestBody SolicitudCategoria solicitud) {
    return servicio.crear(solicitud.nombre());
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Editar una categoría",
      description = "Errores: CATEGORIA_DUPLICADA, MODIFICADO_POR_OTRO_USUARIO (409).")
  public CategoriaVista actualizar(
      @PathVariable Long id,
      @Validated({Default.class, Edicion.class}) @RequestBody SolicitudCategoria solicitud) {
    return servicio.actualizar(id, solicitud.nombre(), solicitud.version());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Eliminar una categoría sin productos",
      description = "Error: CATEGORIA_CON_PRODUCTOS (422).")
  public void eliminar(@PathVariable Long id) {
    servicio.eliminar(id);
  }
}
