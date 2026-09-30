package co.italarm.api.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL real y reloj controlable para las pruebas de integración (BP-25). */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedoresPrueba {

  /** Reloj controlable: reemplaza al reloj del sistema en las pruebas de integración. */
  @Bean
  @Primary
  RelojPrueba relojPrueba() {
    return new RelojPrueba();
  }

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgres() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
  }
}
