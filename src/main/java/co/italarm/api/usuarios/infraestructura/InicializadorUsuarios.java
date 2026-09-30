package co.italarm.api.usuarios.infraestructura;

import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import co.italarm.api.usuarios.aplicacion.ServicioContrasenas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, asigna la contraseña inicial de la variable {@code ITALARM_CLAVE_INICIAL} a los
 * usuarios que no tienen una (P-01). La contraseña nunca se escribe en el repositorio ni en los
 * logs.
 */
@Component
public class InicializadorUsuarios implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(InicializadorUsuarios.class);

  private final ServicioContrasenas contrasenas;
  private final PropiedadesItalarm propiedades;

  public InicializadorUsuarios(ServicioContrasenas contrasenas, PropiedadesItalarm propiedades) {
    this.contrasenas = contrasenas;
    this.propiedades = propiedades;
  }

  @Override
  public void run(ApplicationArguments args) {
    String claveInicial = propiedades.usuarios().claveInicial();
    if (claveInicial == null || claveInicial.isBlank()) {
      long pendientes = contrasenas.contarUsuariosSinContrasena();
      if (pendientes > 0) {
        LOG.warn(
            "Hay {} usuario(s) sin contraseña y ITALARM_CLAVE_INICIAL no está definida: no podrán"
                + " ingresar",
            pendientes);
      }
      return;
    }
    int asignados = contrasenas.asignarContrasenaInicial(claveInicial);
    if (asignados > 0) {
      LOG.info("Contraseña inicial asignada a {} usuario(s)", asignados);
    }
  }
}
