package co.italarm.api.reportes.api;

import co.italarm.api.reportes.aplicacion.InicioVista;
import co.italarm.api.reportes.aplicacion.ServicioInicio;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inicio")
@Tag(name = "Inicio", description = "Pantalla de inicio (sección 3.2)")
public class InicioControlador {

  private final ServicioInicio servicio;

  public InicioControlador(ServicioInicio servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  @Operation(
      summary = "Todo lo de la pantalla de Inicio en una petición",
      description =
          "Saludo, avisos de tasas, indicadores del mes en USD con sus equivalentes, stock bajo,"
              + " garantías y cotizaciones por vencer y últimos movimientos. Cada bloque trae"
              + " máximo 10 filas y su total.")
  public InicioVista inicio(@AuthenticationPrincipal UsuarioAutenticado usuario) {
    return servicio.inicio(usuario.usuarioId());
  }
}
