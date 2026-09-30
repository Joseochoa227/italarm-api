package co.italarm.api.catalogo.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Crea productos por la API para las pruebas. */
final class ProductosDePrueba {

  private static final ObjectMapper JSON = new ObjectMapper();

  private ProductosDePrueba() {}

  static Map<String, Object> datos(String codigo, long categoriaId, long unidadId) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("codigo", codigo);
    datos.put("nombre", "Producto " + codigo);
    datos.put("marca", "Hikvision");
    datos.put("categoriaId", categoriaId);
    datos.put("unidadMedidaId", unidadId);
    datos.put("controlaSerial", false);
    datos.put("precioInstalador", "20.00");
    datos.put("precioClienteFinal", "25.50");
    return datos;
  }

  static long crear(MockMvc mvc, String token, String codigo, long categoriaId, long unidadId)
      throws Exception {
    String respuesta =
        mvc.perform(
                post("/api/v1/productos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JSON.writeValueAsString(datos(codigo, categoriaId, unidadId))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JSON.readTree(respuesta).get("id").asLong();
  }
}
