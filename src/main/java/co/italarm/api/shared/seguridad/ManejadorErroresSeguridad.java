package co.italarm.api.shared.seguridad;

import co.italarm.api.shared.api.FabricaProblemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Responde 401 y 403 en Problem Details, sin redirecciones ni páginas HTML. */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final FabricaProblemas fabrica;

  public ManejadorErroresSeguridad(FabricaProblemas fabrica) {
    this.fabrica = fabrica;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    fabrica.escribir(response, fabrica.crearParaEstado(HttpStatus.UNAUTHORIZED));
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    fabrica.escribir(response, fabrica.crearParaEstado(HttpStatus.FORBIDDEN));
  }
}
