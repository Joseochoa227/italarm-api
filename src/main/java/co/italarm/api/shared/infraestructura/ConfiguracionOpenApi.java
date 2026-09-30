package co.italarm.api.shared.infraestructura;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.math.BigDecimal;
import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Contrato OpenAPI: la fuente única de los tipos del frontend (RT-08). */
@Configuration
public class ConfiguracionOpenApi {

  public static final String ESQUEMA_TOKEN = "bearerAuth";

  static {
    // El dinero, las cantidades y las tasas viajan como texto decimal (RT-06), no como número.
    SpringDocUtils.getConfig()
        .replaceWithSchema(
            BigDecimal.class,
            new StringSchema().format("decimal").pattern("^-?\\d+(\\.\\d+)?$").example("19.5000"));
  }

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
        // Ruta relativa: el contrato no depende del ambiente y Swagger usa el servidor actual.
        .servers(List.of(new Server().url("/")))
        .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_TOKEN));
  }
}
