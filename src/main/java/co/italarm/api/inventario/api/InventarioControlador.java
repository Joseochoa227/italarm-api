package co.italarm.api.inventario.api;

import co.italarm.api.inventario.aplicacion.CambioCostoVista;
import co.italarm.api.inventario.aplicacion.InventarioVista;
import co.italarm.api.inventario.aplicacion.MovimientoKardexVista;
import co.italarm.api.inventario.aplicacion.ProductoInventarioVista;
import co.italarm.api.inventario.aplicacion.SerialVista;
import co.italarm.api.inventario.aplicacion.ServicioInventario;
import co.italarm.api.inventario.dominio.EstadoSerial;
import co.italarm.api.shared.api.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventario")
@Tag(name = "Inventario", description = "Stock valorizado, kárdex, costos y seriales (sección 3.7)")
public class InventarioControlador {

  private final ServicioInventario servicio;

  public InventarioControlador(ServicioInventario servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(
      summary = "Listado valorizado con la cantidad de productos y el valor total",
      description =
          "Busca por nombre, código, marca o número de serie. Valores en USD, COP y VES con las"
              + " tasas vigentes. Orden por defecto: nombre.")
  public InventarioVista listar(
      @RequestParam(required = false) Long categoriaId,
      @Parameter(description = "true: solo activos; false: solo inactivos; vacío: todos")
          @RequestParam(required = false)
          Boolean activo,
      @RequestParam(required = false) String buscar,
      @ParameterObject @PageableDefault(sort = "nombre", direction = Sort.Direction.ASC)
          Pageable pagina) {
    return servicio.listar(categoriaId, activo, buscar, pagina);
  }

  @GetMapping("/productos/{id}")
  @Operation(summary = "Indicadores del producto en las tres monedas y seriales por estado")
  public ProductoInventarioVista detalle(@PathVariable Long id) {
    return servicio.detalle(id);
  }

  @GetMapping("/productos/{id}/kardex")
  @Operation(
      summary = "Kárdex del producto",
      description = "Orden por defecto: el movimiento más reciente primero.")
  public Pagina<MovimientoKardexVista> kardex(
      @PathVariable Long id,
      @ParameterObject @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC)
          Pageable pagina) {
    return Pagina.de(servicio.kardex(id, pagina), m -> m);
  }

  @GetMapping("/productos/{id}/historial-costo")
  @Operation(summary = "Cambios de costo del producto, del más reciente al más antiguo")
  public List<CambioCostoVista> historialCosto(@PathVariable Long id) {
    return servicio.historialCosto(id);
  }

  @GetMapping("/productos/{id}/seriales")
  @Operation(summary = "Seriales del producto, todos o de un estado")
  public List<SerialVista> seriales(
      @PathVariable Long id, @RequestParam(required = false) EstadoSerial estado) {
    return servicio.serialesDelProducto(id, estado);
  }
}
