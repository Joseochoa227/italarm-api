package co.italarm.api.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Base de las pruebas de integración: aplicación completa sobre PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
public abstract class PruebaIntegracion {

  protected static final String CORREO_JOSE = "joseochoa227@gmail.com";
  protected static final String CORREO_VICTOR = "victor8amanuelvd@gmail.com";
  protected static final String CLAVE_INICIAL = "PruebaInicial1";

  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected PasswordEncoder codificador;
  @Autowired protected RelojPrueba reloj;
  @Autowired protected FuenteTrmSimulada fuenteTrm;

  /** Cada prueba parte de los dos usuarios activos, con la contraseña inicial y sin sesiones. */
  @BeforeEach
  void restablecerUsuarios() {
    reloj.restablecer();
    fuenteTrm.restablecer();
    LimpiezaDatos.restablecer(jdbc);
    jdbc.update("delete from sesion");
    jdbc.update(
        "update usuario set contrasena_hash = ?, activo = true", codificador.encode(CLAVE_INICIAL));
  }

  protected String cuerpo(Object objeto) throws Exception {
    return json.writeValueAsString(objeto);
  }

  protected String ingresar(String correo, String contrasena) throws Exception {
    String respuesta =
        mvc.perform(
                post("/api/v1/sesion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo(Map.of("correo", correo, "contrasena", contrasena))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode nodo = json.readTree(respuesta);
    return nodo.get("token").asText();
  }

  protected static String bearer(String token) {
    return "Bearer " + token;
  }
}
