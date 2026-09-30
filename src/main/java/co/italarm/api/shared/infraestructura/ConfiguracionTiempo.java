package co.italarm.api.shared.infraestructura;

import co.italarm.api.shared.dominio.FechaNegocio;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable (BP-13): las pruebas lo reemplazan para simular fechas. */
@Configuration
public class ConfiguracionTiempo {

  @Bean
  public Clock reloj() {
    return Clock.systemUTC();
  }

  @Bean
  public FechaNegocio fechaNegocio(Clock reloj) {
    return new FechaNegocio(reloj);
  }
}
