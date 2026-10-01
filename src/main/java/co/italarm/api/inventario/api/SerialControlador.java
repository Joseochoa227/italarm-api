package co.italarm.api.inventario.api;

import co.italarm.api.inventario.aplicacion.HistorialSerialVista;
import co.italarm.api.inventario.aplicacion.SerialVista;
import co.italarm.api.inventario.aplicacion.ServicioInventario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seriales")
@Tag(name = "Seriales", description = "Búsqueda e historial de números de serie (RF-24)")
public class SerialControlador {

  private final ServicioInventario servicio;

  public SerialControlador(ServicioInventario servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(
      summary = "Buscar un serial desde cualquier pantalla",
      description = "Hasta 50 seriales que contienen el texto, sin distinguir mayúsculas.")
  public List<SerialVista> buscar(@RequestParam String numero) {
    return servicio.buscarSeriales(numero);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Historial completo de un serial")
  public HistorialSerialVista historial(@PathVariable Long id) {
    return servicio.historialSerial(id);
  }
}
