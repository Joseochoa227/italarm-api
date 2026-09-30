package co.italarm.api.usuarios.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Gestión de usuarios (RF-148, P-15). */
class GestionUsuariosApiIntegracionTest extends PruebaIntegracion {

  private static final String CLAVE_NUEVA = "Italarm#2026";

  private String token;
  private long idJose;
  private long idVictor;

  @BeforeEach
  void preparar() throws Exception {
    token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    idJose =
        jdbc.queryForObject("select id from usuario where correo = ?", Long.class, CORREO_JOSE);
    idVictor =
        jdbc.queryForObject("select id from usuario where correo = ?", Long.class, CORREO_VICTOR);
  }

  private ResultActions publicar(String ruta, Object cuerpo) throws Exception {
    return mvc.perform(
        post(ruta)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(cuerpo)));
  }

  @Test
  void listaLosUsuarios() throws Exception {
    mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].nombre").value("Jose"))
        .andExpect(jsonPath("$[0].activo").value(true))
        .andExpect(jsonPath("$[0].contrasenaHash").doesNotExist());
  }

  @Test
  void creaUnUsuarioQuePuedeIngresar() throws Exception {
    publicar(
            "/api/v1/usuarios",
            Map.of(
                "nombre",
                "Ana",
                "correo",
                "Ana@Italarm.com",
                "contrasena",
                CLAVE_NUEVA,
                "confirmacion",
                CLAVE_NUEVA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.correo").value("ana@italarm.com"))
        .andExpect(jsonPath("$.activo").value(true));

    ingresar("ana@italarm.com", CLAVE_NUEVA);
  }

  @Test
  void validaLaContrasenaYElCorreoAlCrear() throws Exception {
    publicar(
            "/api/v1/usuarios",
            Map.of(
                "nombre",
                "Ana",
                "correo",
                "ana@italarm.com",
                "contrasena",
                "debil",
                "confirmacion",
                "debil"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CONTRASENA_DEBIL"));
    publicar(
            "/api/v1/usuarios",
            Map.of(
                "nombre",
                "Otro",
                "correo",
                CORREO_VICTOR.toUpperCase(),
                "contrasena",
                CLAVE_NUEVA,
                "confirmacion",
                CLAVE_NUEVA))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("USUARIO_CORREO_DUPLICADO"));
  }

  @Test
  void desactivarCierraLasSesionesYActivarDevuelveElAcceso() throws Exception {
    String tokenVictor = ingresar(CORREO_VICTOR, CLAVE_INICIAL);

    publicar("/api/v1/usuarios/" + idVictor + "/desactivar", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activo").value(false));
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(tokenVictor)))
        .andExpect(status().isUnauthorized());

    publicar("/api/v1/usuarios/" + idVictor + "/activar", Map.of())
        .andExpect(jsonPath("$.activo").value(true));
    ingresar(CORREO_VICTOR, CLAVE_INICIAL);
  }

  @Test
  void nadieSeDesactivaASiMismo() throws Exception {
    publicar("/api/v1/usuarios/" + idJose + "/desactivar", Map.of())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("NO_PUEDE_DESACTIVARSE_A_SI_MISMO"));
  }

  @Test
  void restableceLaContrasenaDeOtroYCierraSusSesiones() throws Exception {
    String tokenVictor = ingresar(CORREO_VICTOR, CLAVE_INICIAL);

    publicar(
            "/api/v1/usuarios/" + idVictor + "/restablecer-contrasena",
            Map.of("contrasenaNueva", CLAVE_NUEVA, "confirmacion", CLAVE_NUEVA))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(tokenVictor)))
        .andExpect(status().isUnauthorized());
    ingresar(CORREO_VICTOR, CLAVE_NUEVA);
  }

  @Test
  void laPropiaContrasenaSeCambiaConLaOpcionDeCambio() throws Exception {
    publicar(
            "/api/v1/usuarios/" + idJose + "/restablecer-contrasena",
            Map.of("contrasenaNueva", CLAVE_NUEVA, "confirmacion", CLAVE_NUEVA))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("USAR_CAMBIO_DE_CONTRASENA"));
    publicar(
            "/api/v1/usuarios/" + idVictor + "/restablecer-contrasena",
            Map.of("contrasenaNueva", CLAVE_NUEVA, "confirmacion", "Otra#2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CONTRASENA_NO_COINCIDE"));
    publicar("/api/v1/usuarios/999/activar", Map.of()).andExpect(status().isNotFound());
  }
}
