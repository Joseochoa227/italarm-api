package co.italarm.api.configuracion;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class ConfiguracionApiIntegracionTest extends PruebaIntegracion {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 1};

  private String token;

  @BeforeEach
  void iniciarSesion() throws Exception {
    token = ingresar(CORREO_VICTOR, CLAVE_INICIAL);
  }

  private static Map<String, Object> datos(int validez, int version) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("empresaNombre", "ITALARM");
    datos.put("empresaLema", "Instalación de cámaras de seguridad");
    datos.put("empresaNit", "900.123.456-7");
    datos.put("empresaCiudad", "Cúcuta");
    datos.put("empresaTelefono", "+57 300 123 4567");
    datos.put("empresaCorreo", "contacto@italarm.com");
    datos.put("limiteVariacionTasa", "7.5");
    datos.put("validezCotizacionDias", validez);
    datos.put("garantiaManoObraMeses", 3);
    datos.put("garantiaEquiposMeses", 3);
    datos.put("condicionesGarantia", "No cubre daños por descargas eléctricas.");
    datos.put("piePdf", "Documento no válido como factura.");
    datos.put("version", version);
    return datos;
  }

  private ResultActions editar(Map<String, Object> datos) throws Exception {
    return mvc.perform(
        put("/api/v1/configuracion")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(datos)));
  }

  @Test
  void muestraLosValoresPorDefecto() throws Exception {
    mvc.perform(get("/api/v1/configuracion").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.empresaNombre").value("ITALARM"))
        .andExpect(jsonPath("$.limiteVariacionTasa").value("5"))
        .andExpect(jsonPath("$.validezCotizacionDias").value(15))
        .andExpect(jsonPath("$.garantiaManoObraMeses").value(3))
        .andExpect(jsonPath("$.logoUrl").value(nullValue()));
  }

  @Test
  void editaLosDatosDeLaEmpresaYLosValoresPorDefecto() throws Exception {
    editar(datos(30, 0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.empresaNit").value("900.123.456-7"))
        .andExpect(jsonPath("$.limiteVariacionTasa").value("7.5"))
        .andExpect(jsonPath("$.validezCotizacionDias").value(30))
        .andExpect(jsonPath("$.version").value(1));
    editar(datos(15, 0))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("MODIFICADO_POR_OTRO_USUARIO"));
  }

  @Test
  void rechazaValoresFueraDeLoPermitido() throws Exception {
    editar(datos(20, 0))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CONFIGURACION_INVALIDA"));
  }

  @Test
  void subeYQuitaElLogo() throws Exception {
    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/configuracion/logo")
                .file(new MockMultipartFile("archivo", "logo.png", "image/png", PNG))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.logoUrl")
                .value(
                    startsWith(
                        "http://localhost:8080/api/v1/archivos?clave=configuracion%2Flogo-")));

    mvc.perform(
            delete("/api/v1/configuracion/logo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.logoUrl").value(nullValue()));
  }
}
