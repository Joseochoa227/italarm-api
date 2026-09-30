package co.italarm.api.shared.infraestructura;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Propiedades propias de la aplicación, tomadas de variables de entorno (BP-17). */
@ConfigurationProperties("italarm")
public record PropiedadesItalarm(@DefaultValue Cors cors, @DefaultValue Usuarios usuarios) {

  /** Orígenes del frontend permitidos por CORS. */
  public record Cors(@DefaultValue List<String> origenes) {}

  /** Contraseña inicial de los usuarios que aún no tienen una (P-01). */
  public record Usuarios(String claveInicial) {}
}
