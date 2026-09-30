package co.italarm.api.shared.infraestructura;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Contrato OpenAPI: la fuente única de los tipos del frontend (RT-08). */
@Configuration
public class ConfiguracionOpenApi {

  public static final String ESQUEMA_TOKEN = "bearerAuth";

  @Bean
  public OpenAPI contratoApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("API ITALARM")
                .version("v1")
                .description(
                    "Inventario, ventas, cotizaciones e instalaciones de ITALARM. Los errores"
                        + " responden en Problem Details (RFC 9457) con la propiedad `codigo`. El"
                        + " dinero viaja como texto decimal junto con su moneda."))
        .components(
            new Components()
                .addSecuritySchemes(
                    ESQUEMA_TOKEN,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("Token recibido en POST /api/v1/sesion.")))
        .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_TOKEN));
  }
}
