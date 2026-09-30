package co.italarm.api.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import co.italarm.api.soporte.PruebaIntegracion;
import co.italarm.api.usuarios.infraestructura.InicializadorUsuarios;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class InicializadorUsuariosIntegracionTest extends PruebaIntegracion {

  @Autowired InicializadorUsuarios inicializador;
  @Autowired ServicioContrasenas contrasenas;

  @Autowired PropiedadesItalarm propiedades;

  private PropiedadesItalarm propiedadesConClave(String clave) {
    return new PropiedadesItalarm(
        propiedades.cors(),
        new PropiedadesItalarm.Usuarios(clave),
        propiedades.almacenamiento(),
        propiedades.trm());
  }

  private String hashDe(String correo) {
    return jdbc.queryForObject(
        "select contrasena_hash from usuario where correo = ?", String.class, correo);
  }

  @Test
  void losUsuariosInicialesSonJoseYVictor() {
    List<String> nombres =
        jdbc.queryForList("select nombre from usuario order by id", String.class);

    assertThat(nombres).containsExactly("Jose", "Victor");
  }

  @Test
  void asignaLaContrasenaInicialSoloAQuienNoTiene() throws Exception {
    jdbc.update("update usuario set contrasena_hash = null where correo = ?", CORREO_VICTOR);
    String hashJose = hashDe(CORREO_JOSE);

    inicializador.run(null);

    assertThat(hashDe(CORREO_JOSE)).isEqualTo(hashJose);
    assertThat(codificador.matches(CLAVE_INICIAL, hashDe(CORREO_VICTOR))).isTrue();
    assertThat(ingresar(CORREO_VICTOR, CLAVE_INICIAL)).isNotBlank();
  }

  @Test
  void noSobrescribeContrasenasEnUnNuevoArranque() throws Exception {
    String hashJose = hashDe(CORREO_JOSE);
    String hashVictor = hashDe(CORREO_VICTOR);

    inicializador.run(null);

    assertThat(hashDe(CORREO_JOSE)).isEqualTo(hashJose);
    assertThat(hashDe(CORREO_VICTOR)).isEqualTo(hashVictor);
  }

  @Test
  void sinVariableDefinidaAvisaYNoAsigna(CapturedOutput salida) throws Exception {
    jdbc.update("update usuario set contrasena_hash = null where correo = ?", CORREO_VICTOR);
    InicializadorUsuarios sinClave =
        new InicializadorUsuarios(contrasenas, propiedadesConClave(" "));

    sinClave.run(null);

    assertThat(hashDe(CORREO_VICTOR)).isNull();
    assertThat(salida.getAll()).contains("ITALARM_CLAVE_INICIAL no está definida");
  }

  @Test
  void sinVariableYSinPendientesNoAvisa(CapturedOutput salida) throws Exception {
    InicializadorUsuarios sinClave =
        new InicializadorUsuarios(contrasenas, propiedadesConClave(null));

    sinClave.run(null);

    assertThat(salida.getAll()).doesNotContain("ITALARM_CLAVE_INICIAL no está definida");
  }
}
