package co.italarm.api.usuarios.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import co.italarm.api.usuarios.dominio.TokenSesion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@ExtendWith(OutputCaptureExtension.class)
class SesionApiIntegracionTest extends PruebaIntegracion {

  @Test
  void ingresoCorrectoDevuelveTokenYUsuario() throws Exception {
    mvc.perform(
            post("/api/v1/sesion")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "Prueba/1.0")
                .content(cuerpo(Map.of("correo", CORREO_JOSE, "contrasena", CLAVE_INICIAL))))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.token", notNullValue()))
        .andExpect(jsonPath("$.usuario.nombre").value("Jose"))
        .andExpect(jsonPath("$.usuario.correo").value(CORREO_JOSE))
        .andExpect(jsonPath("$.usuario.id", notNullValue()));
  }

  @Test
  void enLaBaseDeDatosSoloQuedaElHashDelToken() throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);

    Map<String, Object> sesion = jdbc.queryForMap("select token_hash, agente_usuario from sesion");
    assertThat(sesion.get("token_hash")).isEqualTo(TokenSesion.hashDe(token)).isNotEqualTo(token);
  }

  @Test
  void elCorreoNoDistingueMayusculas() throws Exception {
    String token = ingresar("JoseOchoa227@Gmail.COM", CLAVE_INICIAL);

    assertThat(token).isNotBlank();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "contraseña errada, joseochoa227@gmail.com, OtraClave1",
    "correo inexistente, nadie@italarm.com, PruebaInicial1"
  })
  void ingresoIncorrectoResponde401ConElMismoMensaje(String caso, String correo, String contrasena)
      throws Exception {
    mvc.perform(
            post("/api/v1/sesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("correo", correo, "contrasena", contrasena))))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
        .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."))
        .andExpect(jsonPath("$.correlationId", notNullValue()));
  }

  @Test
  void unUsuarioInactivoNoPuedeIngresar() throws Exception {
    jdbc.update("update usuario set activo = false where correo = ?", CORREO_VICTOR);

    mvc.perform(
            post("/api/v1/sesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("correo", CORREO_VICTOR, "contrasena", CLAVE_INICIAL))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
  }

  @Test
  void unUsuarioSinContrasenaNoPuedeIngresar() throws Exception {
    jdbc.update("update usuario set contrasena_hash = null where correo = ?", CORREO_VICTOR);

    mvc.perform(
            post("/api/v1/sesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("correo", CORREO_VICTOR, "contrasena", CLAVE_INICIAL))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
  }

  @Test
  void validaLosDatosDeIngreso() throws Exception {
    mvc.perform(
            post("/api/v1/sesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("correo", "no-es-correo", "contrasena", ""))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACION"))
        .andExpect(jsonPath("$.title").value("Datos inválidos"))
        .andExpect(jsonPath("$.errores", hasSize(2)))
        .andExpect(
            jsonPath("$.errores[?(@.campo=='correo')].mensaje").value("El correo no es válido."))
        .andExpect(
            jsonPath("$.errores[?(@.campo=='contrasena')].mensaje")
                .value("Ingresa tu contraseña."));
  }

  @Test
  void rechazaUnCuerpoMalFormado() throws Exception {
    mvc.perform(post("/api/v1/sesion").contentType(MediaType.APPLICATION_JSON).content("{roto"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACION"))
        .andExpect(jsonPath("$.detail").value("La petición no es válida."));
  }

  @Test
  void consultaElUsuarioDeLaSesion() throws Exception {
    String token = ingresar(CORREO_VICTOR, CLAVE_INICIAL);

    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Victor"))
        .andExpect(jsonPath("$.correo").value(CORREO_VICTOR));
  }

  @Test
  void sinTokenResponde401EnProblemDetails() throws Exception {
    mvc.perform(get("/api/v1/sesion"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
        .andExpect(jsonPath("$.detail").value("Debes iniciar sesión para continuar."));
  }

  @ParameterizedTest
  @CsvSource({"Bearer inventado", "Bearer ", "Basic am9zZTp4", "token-sin-prefijo"})
  void conUnTokenInvalidoResponde401(String cabecera) throws Exception {
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, cabecera))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
  }

  @Test
  void unTokenDemasiadoLargoSeRechaza() throws Exception {
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer("x".repeat(500))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void conUnUsuarioDesactivadoSuTokenDejaDeServir() throws Exception {
    String token = ingresar(CORREO_VICTOR, CLAVE_INICIAL);
    jdbc.update("update usuario set activo = false where correo = ?", CORREO_VICTOR);

    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void cerrarSesionInvalidaSoloEseToken() throws Exception {
    String celular = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    String computador = ingresar(CORREO_JOSE, CLAVE_INICIAL);

    mvc.perform(delete("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(celular)))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(celular)))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(computador)))
        .andExpect(status().isOk());
  }

  @Test
  void losLogsNoContienenContrasenasNiTokens(CapturedOutput salida) throws Exception {
    String token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    mvc.perform(
        post("/api/v1/sesion")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(Map.of("correo", CORREO_JOSE, "contrasena", "ClaveErrada#9"))));

    assertThat(salida.getAll())
        .doesNotContain(CLAVE_INICIAL)
        .doesNotContain("ClaveErrada#9")
        .doesNotContain(token);
  }

  @Test
  void lasSolicitudesNoExponenLaContrasenaAlConvertirseEnTexto() {
    assertThat(new SolicitudIngreso(CORREO_JOSE, "Secreta#1").toString())
        .contains(CORREO_JOSE)
        .doesNotContain("Secreta#1");
    assertThat(new SolicitudCambioContrasena("Secreta#1", "Nueva#2026", "Nueva#2026").toString())
        .doesNotContain("Secreta#1")
        .doesNotContain("Nueva#2026");
    assertThat(
            new RespuestaIngreso("tok-123", new UsuarioActual(1L, "Jose", CORREO_JOSE)).toString())
        .doesNotContain("tok-123");
  }
}
