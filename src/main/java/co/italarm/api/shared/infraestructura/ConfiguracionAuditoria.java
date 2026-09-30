package co.italarm.api.shared.infraestructura;

import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Auditoría automática (BP-12): quién y cuándo creó o modificó cada registro. Lo que hace el propio
 * sistema (por ejemplo al arrancar) queda sin usuario.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorActual", dateTimeProviderRef = "fechaAuditoria")
public class ConfiguracionAuditoria {

  @Bean
  public AuditorAware<Long> auditorActual() {
    return () ->
        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getPrincipal)
            .filter(UsuarioAutenticado.class::isInstance)
            .map(principal -> ((UsuarioAutenticado) principal).usuarioId());
  }

  @Bean
  public DateTimeProvider fechaAuditoria(Clock reloj) {
    return () -> Optional.of(reloj.instant());
  }
}
