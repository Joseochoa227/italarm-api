package co.italarm.api.catalogo.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class CategoriaYUnidadApiIntegracionTest extends PruebaIntegracion {

  private String token;

  @BeforeEach
  void iniciarSesion() throws Exception {
    token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
  }

  private ResultActions enviar(MockHttpServletRequestBuilder peticion, Object cuerpo)
      throws Exception {
    return mvc.perform(
        peticion
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(cuerpo)));
  }

  private ResultActions obtener(String ruta) throws Exception {
    return mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  @Test
  void listaLasOchoCategoriasInicialesOrdenadas() throws Exception {
    obtener("/api/v1/categorias")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(8)))
        .andExpect(jsonPath("$[0].nombre").value("Balunes"))
        .andExpect(jsonPath("$[0].cantidadProductos").value(0));
  }

  @Test
  void creaEditaYEliminaUnaCategoria() throws Exception {
    String creada =
        enviar(post("/api/v1/categorias"), Map.of("nombre", "  Herramientas "))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Herramientas"))
            .andExpect(jsonPath("$.version").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(creada).get("id").asLong();

    enviar(
            put("/api/v1/categorias/" + id),
            Map.of("nombre", "Herramientas y equipos", "version", 0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Herramientas y equipos"))
        .andExpect(jsonPath("$.version").value(1));

    mvc.perform(delete("/api/v1/categorias/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());
    obtener("/api/v1/categorias").andExpect(jsonPath("$", hasSize(8)));
  }

  @Test
  void noPermiteCategoriasConElMismoNombre() throws Exception {
    enviar(post("/api/v1/categorias"), Map.of("nombre", "cámaras"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CATEGORIA_DUPLICADA"));
    enviar(put("/api/v1/categorias/2"), Map.of("nombre", "CABLE", "version", 0))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CATEGORIA_DUPLICADA"));
  }

  @Test
  void alEditarExigeLaVersionYDetectaCambiosDeOtroUsuario() throws Exception {
    enviar(put("/api/v1/categorias/1"), Map.of("nombre", "Cámaras IP"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("version"));
    enviar(put("/api/v1/categorias/1"), Map.of("nombre", "Cámaras IP", "version", 0))
        .andExpect(status().isOk());
    enviar(put("/api/v1/categorias/1"), Map.of("nombre", "Cámaras análogas", "version", 0))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("MODIFICADO_POR_OTRO_USUARIO"));
  }

  @Test
  void noEliminaUnaCategoriaConProductos() throws Exception {
    ProductosDePrueba.crear(mvc, token, "CAM-D2", 1, 1);

    mvc.perform(delete("/api/v1/categorias/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("CATEGORIA_CON_PRODUCTOS"));
    obtener("/api/v1/categorias").andExpect(jsonPath("$[?(@.id==1)].cantidadProductos").value(1));
  }

  @Test
  void unaCategoriaInexistenteResponde404() throws Exception {
    mvc.perform(delete("/api/v1/categorias/999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
  }

  @Test
  void listaLasUnidadesIniciales() throws Exception {
    obtener("/api/v1/unidades-medida")
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[?(@.abreviatura=='m')].admiteDecimales").value(true))
        .andExpect(jsonPath("$[?(@.abreviatura=='und')].admiteDecimales").value(false));
  }

  @Test
  void creaEditaYEliminaUnaUnidad() throws Exception {
    String creada =
        enviar(
                post("/api/v1/unidades-medida"),
                Map.of("nombre", "Caja", "abreviatura", "cja", "admiteDecimales", false))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(creada).get("id").asLong();

    enviar(
            put("/api/v1/unidades-medida/" + id),
            Map.of("nombre", "Caja", "abreviatura", "caja", "admiteDecimales", true, "version", 0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.abreviatura").value("caja"));
    mvc.perform(
            delete("/api/v1/unidades-medida/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());
  }

  @Test
  void noPermiteUnidadesDuplicadas() throws Exception {
    enviar(
            post("/api/v1/unidades-medida"),
            Map.of("nombre", "Metros", "abreviatura", "M", "admiteDecimales", true))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("UNIDAD_DUPLICADA"));
  }

  @Test
  void unaUnidadEnUsoNoSeEliminaNiCambiaSusDecimales() throws Exception {
    ProductosDePrueba.crear(mvc, token, "CONECTOR", 7, 1);

    mvc.perform(
            delete("/api/v1/unidades-medida/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("UNIDAD_EN_USO"));
    enviar(
            put("/api/v1/unidades-medida/1"),
            Map.of("nombre", "Unidad", "abreviatura", "und", "admiteDecimales", true, "version", 0))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("UNIDAD_EN_USO"));
    enviar(
            put("/api/v1/unidades-medida/1"),
            Map.of(
                "nombre", "Unidades", "abreviatura", "und", "admiteDecimales", false, "version", 0))
        .andExpect(status().isOk());
  }
}
