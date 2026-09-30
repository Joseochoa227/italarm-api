package co.italarm.api.shared.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Formato de errores, dinero, CORS, cabeceras y endpoints públicos (BP-27). */
class ApiComunIntegracionTest extends PruebaIntegracion {

  private String token;

  @BeforeEach
  void iniciarSesion() throws Exception {
    token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
  }

  @Test
  void unErrorDeNegocioRespondeConSuCodigoYMensaje() throws Exception {
    mvc.perform(get("/api/v1/_pruebas/negocio").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.title").value("Regla de negocio incumplida"))
        .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
        .andExpect(jsonPath("$.detail").value("Stock insuficiente · quedan 3"));
  }

  @Test
  void unErrorInesperadoNoExponeDetallesTecnicos() throws Exception {
    mvc.perform(get("/api/v1/_pruebas/inesperado").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
        .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intenta de nuevo."))
        .andExpect(content().string(not(containsString("detalle interno"))))
        .andExpect(content().string(not(containsString("IllegalStateException"))));
  }

  @Test
  void unConflictoDeVersionPideRecargar() throws Exception {
    mvc.perform(
            get("/api/v1/_pruebas/concurrencia").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("MODIFICADO_POR_OTRO_USUARIO"));
  }

  @Test
  void accesoDenegadoResponde403() throws Exception {
    mvc.perform(get("/api/v1/_pruebas/acceso").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
  }

  @Test
  void unRecursoInexistenteResponde404() throws Exception {
    mvc.perform(get("/api/v1/no-existe").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
  }

  @Test
  void unMetodoNoPermitidoResponde405() throws Exception {
    mvc.perform(patch("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
  }

  @Test
  void unFormatoNoSoportadoResponde415() throws Exception {
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/sesion")
                .contentType(MediaType.TEXT_PLAIN)
                .content("hola"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.codigo").value("FORMATO_NO_SOPORTADO"));
  }

  @Test
  void elDineroViajaComoTextoDecimalConSuMoneda() throws Exception {
    mvc.perform(get("/api/v1/_pruebas/dinero").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"monto\":\"19.5000\",\"moneda\":\"USD\"}", true));
    mvc.perform(
            get("/api/v1/_pruebas/dinero-grande").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(jsonPath("$.monto").value("10000000"));
  }

  @Test
  void devuelveElIdentificadorDeCorrelacionRecibido() throws Exception {
    mvc.perform(get("/api/v1/sesion").header(FiltroCorrelacion.CABECERA, "abc-123"))
        .andExpect(header().string(FiltroCorrelacion.CABECERA, "abc-123"))
        .andExpect(jsonPath("$.correlationId").value("abc-123"));
  }

  @Test
  void generaUnIdentificadorDeCorrelacionSiNoLlegaUnoValido() throws Exception {
    mvc.perform(get("/api/v1/sesion").header(FiltroCorrelacion.CABECERA, "<script>"))
        .andExpect(
            header()
                .string(
                    FiltroCorrelacion.CABECERA,
                    matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
  }

  @Test
  void corsPermiteSoloElOrigenDelFrontend() throws Exception {
    mvc.perform(
            options("/api/v1/sesion")
                .header(HttpHeaders.ORIGIN, "https://app.italarm.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.italarm.test"))
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

    mvc.perform(
            options("/api/v1/sesion")
                .header(HttpHeaders.ORIGIN, "https://otro-sitio.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void incluyeLasCabecerasDeSeguridad() throws Exception {
    mvc.perform(get("/api/v1/sesion").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(
            header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
  }

  @Test
  void laSaludEsPublica() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
  }

  @Test
  void losDemasEndpointsDeActuatorNoEstanExpuestos() throws Exception {
    mvc.perform(get("/actuator/env").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound());
  }

  @Test
  void elContratoOpenApiEsPublico() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("API ITALARM"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.paths['/api/v1/sesion'].post.security").isEmpty())
        .andExpect(jsonPath("$.paths['/api/v1/usuarios/actual/contrasena'].put").exists());
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  /**
   * El contrato versionado en {@code contrato/openapi.json} debe coincidir con el código (RT-08).
   * Si cambia un endpoint, se actualiza con {@code ./mvnw test -Dtest=ApiComunIntegracionTest
   * -Dcontrato.actualizar=true} y el archivo se sube junto con el cambio. También queda una copia
   * en target/openapi.json para la CI.
   */
  @Test
  void elContratoVersionadoCorrespondeAlCodigo() throws Exception {
    String generado =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    String bonito =
        json.writerWithDefaultPrettyPrinter().writeValueAsString(json.readTree(generado)) + "\n";
    java.nio.file.Path copia = java.nio.file.Path.of("target", "openapi.json");
    java.nio.file.Files.createDirectories(copia.getParent());
    java.nio.file.Files.writeString(copia, bonito);

    java.nio.file.Path contrato = java.nio.file.Path.of("contrato", "openapi.json");
    if (Boolean.getBoolean("contrato.actualizar") || !java.nio.file.Files.exists(contrato)) {
      java.nio.file.Files.createDirectories(contrato.getParent());
      java.nio.file.Files.writeString(contrato, bonito);
    }
    org.assertj.core.api.Assertions.assertThat(
            json.readTree(java.nio.file.Files.readString(contrato)))
        .withFailMessage(
            "contrato/openapi.json no corresponde al código. Actualízalo con: ./mvnw test"
                + " -Dtest=ApiComunIntegracionTest -Dcontrato.actualizar=true")
        .isEqualTo(json.readTree(generado));
  }
}
