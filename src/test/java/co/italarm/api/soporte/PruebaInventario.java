package co.italarm.api.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base de las pruebas de compras, ajustes e inventario: sesión de Jose, reloj fijo el 01/10/2026 a
 * las 10:00 de Bogotá, TRM 4.000 y tasa del bolívar 50 de ese día, y utilidades para crear
 * productos, proveedores y compras.
 */
public abstract class PruebaInventario extends PruebaIntegracion {

  protected static final LocalDate HOY = LocalDate.of(2026, 10, 1);
  protected static final long CATEGORIA_CAMARAS = 1;
  protected static final long CATEGORIA_CABLE = 4;
  protected static final long UNIDAD = 1;
  protected static final long METRO = 2;

  protected String token;

  @BeforeEach
  void prepararInventario() throws Exception {
    reloj.fijar(Instant.parse("2026-10-01T15:00:00Z"));
    token = ingresar(CORREO_JOSE, CLAVE_INICIAL);
    registrarTasa("USD_COP", HOY, "4000");
    registrarTasa("USD_VES", HOY, "50");
  }

  protected ResultActions enviar(MockHttpServletRequestBuilder peticion, Object cuerpo)
      throws Exception {
    return mvc.perform(
        peticion
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(cuerpo)));
  }

  protected ResultActions sinCuerpo(MockHttpServletRequestBuilder peticion) throws Exception {
    return mvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  protected JsonNode leer(ResultActions resultado) throws Exception {
    return json.readTree(resultado.andReturn().getResponse().getContentAsString());
  }

  protected void registrarTasa(String par, LocalDate fecha, String valor) {
    jdbc.update("delete from tasa_cambio where par = ? and fecha = ?", par, fecha);
    boolean manual = "USD_VES".equals(par);
    jdbc.update(
        "insert into tasa_cambio (par, fecha, valor, fuente, registrada_por, registrada_en)"
            + " values (?, ?, ?, ?, ?, now())",
        par,
        fecha,
        new BigDecimal(valor),
        manual ? "MANUAL" : "SUPERFINANCIERA",
        manual ? 1L : null);
  }

  protected long crearProveedor(String nombre) {
    return jdbc.queryForObject(
        "insert into proveedor (nombre, moneda_habitual) values (?, 'USD') returning id",
        Long.class,
        nombre);
  }

  /** Cliente de prueba: {@code tipo} es INSTALADOR o CLIENTE_FINAL. */
  protected long crearCliente(String nombre, String tipo) {
    return jdbc.queryForObject(
        "insert into cliente (tipo, nombre, tipo_documento, numero_documento, telefono, direccion)"
            + " values (?, ?, 'CC', ?, '+573001234567', 'Calle 10 # 5-20') returning id",
        Long.class,
        tipo,
        nombre,
        String.valueOf(Math.abs(nombre.hashCode())));
  }

  protected long crearProducto(String codigo, long categoriaId, long unidadId, boolean serial)
      throws Exception {
    Map<String, Object> datos = new HashMap<>();
    datos.put("codigo", codigo);
    datos.put("nombre", "Producto " + codigo);
    datos.put("categoriaId", categoriaId);
    datos.put("unidadMedidaId", unidadId);
    datos.put("controlaSerial", serial);
    datos.put("precioInstalador", "30.00");
    datos.put("precioClienteFinal", "40.00");
    return leer(enviar(post("/api/v1/productos"), datos).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  protected static Map<String, Object> linea(long productoId, String cantidad, String costo) {
    return linea(productoId, cantidad, costo, List.of());
  }

  protected static Map<String, Object> linea(
      long productoId, String cantidad, String costo, List<String> seriales) {
    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", productoId);
    linea.put("cantidad", cantidad);
    linea.put("costoUnitario", costo);
    linea.put("seriales", seriales);
    return linea;
  }

  protected static Map<String, Object> compra(
      long proveedorId, String moneda, List<Map<String, Object>> lineas) {
    Map<String, Object> compra = new HashMap<>();
    compra.put("proveedorId", proveedorId);
    compra.put("numeroFactura", "F-" + lineas.hashCode());
    compra.put("moneda", moneda);
    compra.put("lineas", new ArrayList<>(lineas));
    return compra;
  }

  /** Registra una compra por la API y devuelve su respuesta. */
  protected JsonNode comprar(long proveedorId, String moneda, Map<String, Object> linea)
      throws Exception {
    return leer(
        enviar(post("/api/v1/compras"), compra(proveedorId, moneda, List.of(linea)))
            .andExpect(status().isCreated()));
  }

  protected BigDecimal stock(long productoId) {
    return jdbc.queryForObject(
        "select stock from producto where id = ?", BigDecimal.class, productoId);
  }

  protected BigDecimal costo(long productoId) {
    return jdbc.queryForObject(
        "select costo_actual_usd from producto where id = ?", BigDecimal.class, productoId);
  }
}
