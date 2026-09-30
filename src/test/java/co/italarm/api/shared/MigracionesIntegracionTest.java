package co.italarm.api.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.soporte.PruebaIntegracion;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Las migraciones aplican desde cero y Hibernate valida el esquema al arrancar (BP-18). */
class MigracionesIntegracionTest extends PruebaIntegracion {

  @Test
  void aplicaTodasLasMigracionesEnOrden() {
    List<String> versiones =
        jdbc.queryForList(
            "select version from flyway_schema_history where success order by installed_rank",
            String.class);

    assertThat(versiones).containsExactly("1", "2", "3");
  }

  @Test
  void elCorreoDebeGuardarseEnMinusculas() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into usuario (nombre, correo) values ('X', 'Mayus@Correo.com')"))
        .hasMessageContaining("ck_usuario_correo_minusculas");
  }

  @Test
  void elCorreoEsUnico() {
    assertThatThrownBy(
            () ->
                jdbc.update("insert into usuario (nombre, correo) values ('Otro', ?)", CORREO_JOSE))
        .hasMessageContaining("uq_usuario_correo");
  }
}
