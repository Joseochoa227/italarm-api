package co.italarm.api.shared.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import co.italarm.api.soporte.PruebaIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

/**
 * Revisión de seguridad (Fase 6, T5): todos los endpoints del contrato exigen sesión, salvo los
 * públicos acordados (ingreso, descarga con enlace firmado y comprobante compartido por WhatsApp).
 */
class SeguridadContratoIntegracionTest extends PruebaIntegracion {

  private static final Set<String> PUBLICOS =
      Set.of("POST /api/v1/sesion", "GET /api/v1/archivos", "GET /api/v1/comprobantes/{token}");

  @Test
  void todosLosEndpointsExigenSesionSalvoLosPublicos() throws Exception {
    JsonNode contrato = json.readTree(Files.readString(Path.of("contrato/openapi.json")));
    List<String> abiertos = new ArrayList<>();
    List<String> publicosRevisados = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> rutas = contrato.get("paths").fields();
    while (rutas.hasNext()) {
      Map.Entry<String, JsonNode> ruta = rutas.next();
      Iterator<String> metodos = ruta.getValue().fieldNames();
      while (metodos.hasNext()) {
        String metodo = metodos.next().toUpperCase(Locale.ROOT);
        String clave = metodo + " " + ruta.getKey();
        String url = ruta.getKey().replaceAll("\\{[^}]+}", "1");
        int estado =
            mvc.perform(request(HttpMethod.valueOf(metodo), url))
                .andReturn()
                .getResponse()
                .getStatus();
        if (PUBLICOS.contains(clave)) {
          publicosRevisados.add(clave);
          assertThat(estado).as(clave).isNotEqualTo(401);
        } else if (estado != 401) {
          abiertos.add(clave + " → " + estado);
        }
      }
    }
    assertThat(abiertos).as("Endpoints que responden sin sesión").isEmpty();
    assertThat(publicosRevisados).containsExactlyInAnyOrderElementsOf(PUBLICOS);
  }
}
