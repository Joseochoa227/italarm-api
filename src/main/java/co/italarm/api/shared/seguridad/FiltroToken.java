package co.italarm.api.shared.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica la petición con el token opaco de la cabecera {@code Authorization: Bearer} (decisión
 * P-05). Si el token no es válido la petición sigue sin autenticar y Spring Security responde 401.
 */
public class FiltroToken extends OncePerRequestFilter {

  private static final String PREFIJO = "Bearer ";
  private static final List<SimpleGrantedAuthority> PERMISOS =
      List.of(new SimpleGrantedAuthority("ROLE_USUARIO"));

  private final ValidadorToken validador;
  private final SecurityContextHolderStrategy contextos =
      SecurityContextHolder.getContextHolderStrategy();

  public FiltroToken(ValidadorToken validador) {
    this.validador = validador;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (cabecera != null && cabecera.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
      String token = cabecera.substring(PREFIJO.length()).trim();
      validador
          .validar(token)
          .ifPresent(
              usuario -> {
                SecurityContext contexto = contextos.createEmptyContext();
                contexto.setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(usuario, null, PERMISOS));
                contextos.setContext(contexto);
              });
    }
    chain.doFilter(request, response);
  }
}
