package co.italarm.api.shared.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Los valores decimales (dinero, tasas) viajan como texto, nunca como número (RT-06). */
@Configuration
public class ConfiguracionJson {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer decimalesComoTexto() {
    return builder -> builder.serializerByType(BigDecimal.class, new DecimalComoTexto());
  }

  static final class DecimalComoTexto extends StdSerializer<BigDecimal> {

    DecimalComoTexto() {
      super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal valor, JsonGenerator generador, SerializerProvider proveedor)
        throws IOException {
      generador.writeString(valor.toPlainString());
    }
  }
}
