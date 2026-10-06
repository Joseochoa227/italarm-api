package co.italarm.api.garantias.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GarantiaApiIntegracionTest extends PruebaInventario {

  private long cliente;
  private long otro;
  private long instalacionId;
  private long serialVendido;

  @BeforeEach
  void instalarYVender() throws Exception {
    long proveedor = crearProveedor("Distribuidora Andina");
    cliente = crearCliente("Ana Gómez", "CLIENTE_FINAL");
    otro = crearCliente("Juan Instalador", "INSTALADOR");
    long camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(camara, "3", "20", List.of("INS-1", "VEN-1", "VEN-2")));

    Map<String, Object> material = new HashMap<>();
    material.put("productoId", camara);
    material.put("seriales", List.of("INS-1"));
    Map<String, Object> instalacion = new HashMap<>();
    instalacion.put("clienteId", cliente);
    instalacion.put("descripcion", "Cámara en la entrada");
    instalacion.put("tecnicos", List.of(1));
    instalacion.put("moneda", "USD");
    instalacion.put("lineas", List.of(material));
    instalacion.put("manoDeObra", "40");
    instalacionId =
        leer(enviar(post("/api/v1/instalaciones"), instalacion).andExpect(status().isCreated()))
            .get("id")
            .asLong();

    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", camara);
    linea.put("seriales", List.of("VEN-1"));
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", otro);
    venta.put("moneda", "USD");
    venta.put("lineas", List.of(linea));
    enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated());
    serialVendido = jdbc.queryForObject("select id from serial where numero = 'VEN-1'", Long.class);
  }

  @Test
  void cp20_instalacionDel1DeOctubre_garantiaHasta1DeEneroYPorVencerSusUltimos30Dias()
      throws Exception {
    sinCuerpo(get("/api/v1/garantias").param("tipo", "INSTALACION"))
        .andExpect(jsonPath("$.contenido", hasSize(2)))
        .andExpect(jsonPath("$.contenido[0].clase").value("MANO_OBRA"))
        .andExpect(jsonPath("$.contenido[0].documento.consecutivo").value("I-0001"))
        .andExpect(jsonPath("$.contenido[0].vencimiento").value("2027-01-01"))
        .andExpect(jsonPath("$.contenido[0].estado").value("VIGENTE"))
        .andExpect(jsonPath("$.contenido[0].diasRestantes").value(92))
        .andExpect(jsonPath("$.contenido[1].clase").value("EQUIPO"))
        .andExpect(jsonPath("$.contenido[1].serial").value("INS-1"));

    reloj.fijar(Instant.parse("2026-12-01T15:00:00Z"));
    sinCuerpo(get("/api/v1/garantias").param("estado", "POR_VENCER"))
        .andExpect(jsonPath("$.contenido", hasSize(0)));
    reloj.fijar(Instant.parse("2026-12-02T15:00:00Z"));
    sinCuerpo(get("/api/v1/garantias").param("estado", "POR_VENCER"))
        .andExpect(jsonPath("$.contenido", hasSize(3)))
        .andExpect(jsonPath("$.contenido[0].estado").value("POR_VENCER"));
    sinCuerpo(get("/api/v1/instalaciones/{id}", instalacionId))
        .andExpect(jsonPath("$.garantias.estadoManoObra").value("POR_VENCER"));
    reloj.fijar(Instant.parse("2027-01-02T15:00:00Z"));
    sinCuerpo(get("/api/v1/garantias").param("estado", "VENCIDA"))
        .andExpect(jsonPath("$.contenido", hasSize(3)))
        .andExpect(jsonPath("$.contenido[0].diasRestantes").value(-1));
  }

  @Test
  void filtrosPorClienteTipoYSerial_yLasAnuladasNoAparecen() throws Exception {
    sinCuerpo(get("/api/v1/garantias")).andExpect(jsonPath("$.totalElementos").value(3));
    sinCuerpo(get("/api/v1/garantias").param("tipo", "VENTA"))
        .andExpect(jsonPath("$.contenido", hasSize(1)))
        .andExpect(jsonPath("$.contenido[0].cliente").value("Juan Instalador"));
    sinCuerpo(get("/api/v1/garantias").param("clienteId", String.valueOf(cliente)))
        .andExpect(jsonPath("$.contenido", hasSize(2)));
    sinCuerpo(get("/api/v1/garantias").param("serial", "ven"))
        .andExpect(jsonPath("$.contenido", hasSize(1)))
        .andExpect(jsonPath("$.contenido[0].producto").value("Producto CAM-S"));

    enviar(post("/api/v1/instalaciones/{id}/anular", instalacionId), Map.of("motivo", "Error"))
        .andExpect(status().isOk());
    sinCuerpo(get("/api/v1/garantias")).andExpect(jsonPath("$.totalElementos").value(1));
  }

  @Test
  void reclamosDentroYFueraDeGarantia_conSolucionPosterior() throws Exception {
    JsonNode reclamo =
        leer(
            enviar(
                    post("/api/v1/garantias/reclamos"),
                    Map.of("instalacionId", instalacionId, "problema", "La cámara no graba"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INSTALACION"))
                .andExpect(jsonPath("$.documento").value("I-0001"))
                .andExpect(jsonPath("$.cliente").value("Ana Gómez"))
                .andExpect(jsonPath("$.enGarantia").value(true))
                .andExpect(jsonPath("$.solucion").doesNotExist()));
    enviar(
            put("/api/v1/garantias/reclamos/{id}", reclamo.get("id").asLong()),
            Map.of("solucion", "Se reconfiguró el grabador", "version", 0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.solucion").value("Se reconfiguró el grabador"));

    reloj.fijar(Instant.parse("2027-02-01T15:00:00Z"));
    enviar(
            post("/api/v1/garantias/reclamos"),
            Map.of("serialId", serialVendido, "problema", "Sin imagen de noche"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tipo").value("SERIAL"))
        .andExpect(jsonPath("$.serial").value("VEN-1"))
        .andExpect(jsonPath("$.enGarantia").value(false));

    sinCuerpo(get("/api/v1/garantias/reclamos")).andExpect(jsonPath("$", hasSize(2)));
    sinCuerpo(get("/api/v1/garantias/reclamos").param("clienteId", String.valueOf(otro)))
        .andExpect(jsonPath("$", hasSize(1)));
    sinCuerpo(get("/api/v1/seriales/{id}", serialVendido))
        .andExpect(jsonPath("$.serial.vencimientoGarantia").value("2027-01-01"))
        .andExpect(jsonPath("$.reclamos", hasSize(1)))
        .andExpect(jsonPath("$.reclamos[0].enGarantia").value(false));

    long enBodega = jdbc.queryForObject("select id from serial where numero = 'VEN-2'", Long.class);
    enviar(post("/api/v1/garantias/reclamos"), Map.of("serialId", enBodega, "problema", "x"))
        .andExpect(status().isNotFound());
    enviar(
            post("/api/v1/garantias/reclamos"),
            Map.of("instalacionId", instalacionId, "serialId", enBodega, "problema", "x"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("RECLAMO_INVALIDO"));
  }
}
