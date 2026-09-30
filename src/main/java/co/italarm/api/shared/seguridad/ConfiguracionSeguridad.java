package co.italarm.api.shared.seguridad;

import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad de la API. La sesión se maneja con un token opaco en {@code Authorization: Bearer}
 * guardado por el frontend en {@code localStorage} (decisión P-05). Como la API no usa cookies, no
 * aplica protección CSRF.
 */
@Configuration
public class ConfiguracionSeguridad {

  private static final String CSP =
      "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
          + "object-src 'none'; base-uri 'none'; frame-ancestors 'none'";

  @Bean
  public SecurityFilterChain cadenaSeguridad(
      HttpSecurity http, ValidadorToken validador, ManejadorErroresSeguridad manejador)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            reglas ->
                reglas
                    .requestMatchers(HttpMethod.POST, "/api/v1/sesion")
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(new FiltroToken(validador), AnonymousAuthenticationFilter.class)
        .exceptionHandling(
            e -> e.authenticationEntryPoint(manejador).accessDeniedHandler(manejador))
        .headers(
            h ->
                h.contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
        .build();
  }

  @Bean
  public PasswordEncoder codificadorContrasenas() {
    return new BCryptPasswordEncoder();
  }

  /** CORS limitado al dominio del frontend (BP-20). */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(PropiedadesItalarm propiedades) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(propiedades.cors().origenes());
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id"));
    cors.setExposedHeaders(List.of("X-Correlation-Id"));
    cors.setAllowCredentials(false);
    cors.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
    fuente.registerCorsConfiguration("/**", cors);
    return fuente;
  }
}
