package co.italarm.api.shared.infraestructura;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Carga los datos de volumen para medir el rendimiento en local (Fase 6, T4). Solo con el perfil
 * {@code volumen} y solo sobre una base sin compras ni ventas: nunca se activa en producción.
 */
@Component
@Profile("volumen")
public class CargaDatosVolumen implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CargaDatosVolumen.class);

  private final DataSource datos;
  private final JdbcTemplate jdbc;

  public CargaDatosVolumen(DataSource datos, JdbcTemplate jdbc) {
    this.datos = datos;
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments argumentos) {
    Long documentos =
        jdbc.queryForObject(
            "select (select count(*) from compra) + (select count(*) from venta)", Long.class);
    if (documentos != null && documentos > 0) {
      LOG.warn("La base ya tiene compras o ventas: no se cargan los datos de volumen.");
      return;
    }
    LOG.info("Cargando los datos de volumen (3 años de operación simulados)...");
    new ResourceDatabasePopulator(new ClassPathResource("volumen/datos-volumen.sql"))
        .execute(datos);
    LOG.info("Datos de volumen cargados.");
  }
}
