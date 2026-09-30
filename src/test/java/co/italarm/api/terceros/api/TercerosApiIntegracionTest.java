package co.italarm.api.terceros.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaIntegracion;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class TercerosApiIntegracionTest extends PruebaIntegracion {

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

  private static Map<String, Object> cliente(
      String tipo, String nombre, String cc, String telefono) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("tipo", tipo);
    datos.put("nombre", nombre);
    if (cc != null) {
      datos.put("tipoDocumento", "CC");
      datos.put("numeroDocumento", cc);
    }
    datos.put("telefono", telefono);
    datos.put("ciudad", "Cúcuta");
    return datos;
  }

  private long crearCliente(Map<String, Object> datos) throws Exception {
    String respuesta =
        enviar(post("/api/v1/clientes"), datos)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(respuesta).get("id").asLong();
  }

  @Test
  void creaUnClienteEIndicaElPrecioQueSeLeAplica() throws Exception {
    enviar(
            post("/api/v1/clientes"),
            cliente("INSTALADOR", "Carlos Pérez", "1.090.123.456", "300 123 4567"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tipo").value("INSTALADOR"))
        .andExpect(jsonPath("$.precioAplicado").value("INSTALADOR"))
        .andExpect(
            jsonPath("$.precioAplicadoDescripcion").value("Se le aplicará el precio instalador"))
        .andExpect(jsonPath("$.numeroDocumento").value("1090123456"))
        .andExpect(jsonPath("$.telefono").value("+573001234567"))
        .andExpect(jsonPath("$.cantidadMovimientos").value(0));
  }

  @Test
  void validaElTelefonoYElDocumento() throws Exception {
    enviar(post("/api/v1/clientes"), cliente("CLIENTE_FINAL", "Ana", null, "12345"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("TELEFONO_INVALIDO"));

    Map<String, Object> sinNumero = cliente("CLIENTE_FINAL", "Ana", null, "3001234567");
    sinNumero.put("tipoDocumento", "NIT");
    enviar(post("/api/v1/clientes"), sinNumero)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("DOCUMENTO_INCOMPLETO"));

    enviar(post("/api/v1/clientes"), Map.of("nombre", "Sin tipo"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.length()").value(2));
  }

  @Test
  void noPermiteDosClientesConElMismoDocumento() throws Exception {
    crearCliente(cliente("INSTALADOR", "Carlos", "1090123456", "3001234567"));
    long otro = crearCliente(cliente("INSTALADOR", "Luis", null, "3007654321"));

    enviar(
            post("/api/v1/clientes"),
            cliente("CLIENTE_FINAL", "Otro", "1.090.123.456", "3000000000"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CLIENTE_DOCUMENTO_DUPLICADO"));

    Map<String, Object> edicion = cliente("INSTALADOR", "Luis", "1090123456", "3007654321");
    edicion.put("version", 0);
    enviar(put("/api/v1/clientes/" + otro), edicion)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CLIENTE_DOCUMENTO_DUPLICADO"));
  }

  @Test
  void editaConControlDeVersion() throws Exception {
    long id = crearCliente(cliente("INSTALADOR", "Carlos", null, "3001234567"));
    Map<String, Object> edicion = cliente("CLIENTE_FINAL", "Carlos Pérez", null, "+58 412 1234567");
    edicion.put("version", 0);

    enviar(put("/api/v1/clientes/" + id), edicion)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.precioAplicado").value("CLIENTE_FINAL"))
        .andExpect(jsonPath("$.telefono").value("+584121234567"));
    enviar(put("/api/v1/clientes/" + id), edicion)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("MODIFICADO_POR_OTRO_USUARIO"));
  }

  @Test
  void filtraPorTipoYBusca() throws Exception {
    crearCliente(cliente("INSTALADOR", "Carlos Pérez", "1090123456", "3001234567"));
    crearCliente(cliente("CLIENTE_FINAL", "Ana Gómez", null, "3109998877"));
    crearCliente(cliente("CLIENTE_FINAL", "Comercial Andina", null, "3155554433"));

    obtener("/api/v1/clientes").andExpect(jsonPath("$.totalElementos").value(3));
    obtener("/api/v1/clientes?tipo=CLIENTE_FINAL")
        .andExpect(jsonPath("$.totalElementos").value(2))
        .andExpect(jsonPath("$.contenido[0].nombre").value("Ana Gómez"));
    obtener("/api/v1/clientes?buscar=andina").andExpect(jsonPath("$.totalElementos").value(1));
    obtener("/api/v1/clientes?buscar=1090123").andExpect(jsonPath("$.totalElementos").value(1));
    obtener("/api/v1/clientes?buscar=310 999").andExpect(jsonPath("$.totalElementos").value(1));
    obtener("/api/v1/clientes/999").andExpect(status().isNotFound());
  }

  @Test
  void creaEditaYBuscaProveedores() throws Exception {
    Map<String, Object> datos = new HashMap<>();
    datos.put("nombre", "Importadora XYZ");
    datos.put("nit", "900.555.111-2");
    datos.put("ciudad", "Bogotá");
    datos.put("monedaHabitual", "COP");
    String respuesta =
        enviar(post("/api/v1/proveedores"), datos)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nit").value("900555111-2"))
            .andExpect(jsonPath("$.monedaHabitual").value("COP"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(respuesta).get("id").asLong();

    datos.put("monedaHabitual", "USD");
    datos.put("telefono", "6011234567");
    datos.put("version", 0);
    enviar(put("/api/v1/proveedores/" + id), datos)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monedaHabitual").value("USD"))
        .andExpect(jsonPath("$.telefono").value("+576011234567"));

    obtener("/api/v1/proveedores?buscar=xyz").andExpect(jsonPath("$.totalElementos").value(1));
    obtener("/api/v1/proveedores?buscar=nada").andExpect(jsonPath("$.totalElementos").value(0));
    obtener("/api/v1/proveedores/" + id).andExpect(jsonPath("$.nombre").value("Importadora XYZ"));
  }

  @Test
  void laMonedaHabitualEsObligatoria() throws Exception {
    enviar(post("/api/v1/proveedores"), Map.of("nombre", "Sin moneda"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("monedaHabitual"));
  }
}
