package co.italarm.api.usuarios.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ContrasenaApiIntegracionTest extends PruebaIntegracion {

  private static final String NUEVA = "Italarm#2026";

  private ResultActions cambiar(String token, String actual, String nueva, String confirmacion)
      throws Exception {
    return mvc.perform(
        put("/api/v1/usuarios/actual/contrasena")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                cuerpo(
                    Map.of(
                        "contrasenaActual", actual,
                        "contrasenaNueva", nueva,
                        "confirmacion", confirmacion))));
  }

  @Test
  void cambiaLaContrasenaYCierraLasDemasSesiones() throws Exception {
    String otroDispositivo = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    String actual = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    String deVictor = ingresar(CORREO_VICTOR, CLAVE_INICIAL);

    cambiar(actual, CLAVE_INICIAL, NUEVA, NUEVA).andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(actual)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(otroDispositivo)))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(deVictor)))
        .andExpect(status().isOk());

    assertThat(ingresar(CORREO_JOSE, NUEVA)).isNotBlank();
    mvc.perform(
            post("/api/v1/sesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("correo", CORREO_JOSE, "contrasena", CLAVE_INICIAL))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void elCambioQuedaAuditadoConElUsuarioYSubeLaVersion() throws Exception {
    Long idJose =
        jdbc.queryForObject("select id from usuario where correo = ?", Long.class, CORREO_JOSE);
    Long versionAntes =
        jdbc.queryForObject("select version from usuario where id = ?", Long.class, idJose);
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);

    cambiar(token, CLAVE_INICIAL, NUEVA, NUEVA).andExpect(status().isNoContent());

    Map<String, Object> fila =
        jdbc.queryForMap("select updated_by, version from usuario where id = ?", idJose);
    assertThat(fila.get("updated_by")).isEqualTo(idJose);
    assertThat((Long) fila.get("version")).isGreaterThan(versionAntes);
  }

  @Test
  void rechazaUnaConfirmacionDistinta() throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);

    cambiar(token, CLAVE_INICIAL, NUEVA, "Italarm#2027")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CONTRASENA_NO_COINCIDE"));
  }

  @Test
  void rechazaUnaContrasenaDebilIndicandoQueFalta() throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);

    cambiar(token, CLAVE_INICIAL, "Italarm2026", "Italarm2026")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CONTRASENA_DEBIL"))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "La contraseña debe tener al menos 8 caracteres, una mayúscula, una"
                        + " minúscula, un número y un signo. Falta: un signo."));
  }

  @Test
  void rechazaUnaContrasenaActualIncorrecta() throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);

    cambiar(token, "NoEsLaActual1", NUEVA, NUEVA)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CONTRASENA_ACTUAL_INCORRECTA"));
  }

  @Test
  void laNuevaDebeSerDistintaDeLaActual() throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    cambiar(token, CLAVE_INICIAL, NUEVA, NUEVA).andExpect(status().isNoContent());

    cambiar(token, NUEVA, NUEVA, NUEVA)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CONTRASENA_DEBIL"))
        .andExpect(
            jsonPath("$.detail").value("La nueva contraseña debe ser distinta de la actual."));
  }

  @Test
  void validaLosCamposObligatorios() throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);

    cambiar(token, "", "", "")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACION"))
        .andExpect(jsonPath("$.errores.length()").value(3));
  }

  @Test
  void sinSesionNoSePuedeCambiar() throws Exception {
    mvc.perform(
            put("/api/v1/usuarios/actual/contrasena")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }
}
