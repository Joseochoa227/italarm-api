package co.italarm.api.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asigna a cada petición un identificador de correlación (BP-21): lo toma de la cabecera {@value
 * #CABECERA} si es válido o genera uno nuevo, lo deja en el MDC de los logs y lo devuelve en la
 * respuesta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroCorrelacion extends OncePerRequestFilter {

  public static final String CABECERA = "X-Correlation-Id";
  public static final String CLAVE_MDC = "correlationId";

  private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String recibido = request.getHeader(CABECERA);
    String id =
        recibido != null && ID_VALIDO.matcher(recibido).matches()
            ? recibido
            : UUID.randomUUID().toString();
    MDC.put(CLAVE_MDC, id);
    response.setHeader(CABECERA, id);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(CLAVE_MDC);
    }
  }
}
