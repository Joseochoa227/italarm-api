package co.italarm.api.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL real, reloj controlable y fuentes externas simuladas (BP-14, BP-25). */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedoresPrueba {

  /** Reloj controlable: reemplaza al reloj del sistema en las pruebas de integración. */
  @Bean
  @Primary
  RelojPrueba relojPrueba() {
    return new RelojPrueba();
  }

  /** Fuente de la TRM simulada: las pruebas nunca consultan internet. */
  @Bean
  @Primary
  FuenteTrmSimulada fuenteTrmSimulada() {
    return new FuenteTrmSimulada();
  }

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgres() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
  }
}
