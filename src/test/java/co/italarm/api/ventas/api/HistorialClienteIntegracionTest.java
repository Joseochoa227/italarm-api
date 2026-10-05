package co.italarm.api.ventas.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HistorialClienteIntegracionTest extends PruebaInventario {

  private long vender(long clienteId, long productoId, List<String> seriales) throws Exception {
    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", productoId);
    linea.put("seriales", seriales);
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", clienteId);
    venta.put("moneda", "USD");
    venta.put("lineas", List.of(linea));
    return leer(enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  @Test
  void elClienteMuestraSusMovimientosYElSerialAQuienSeVendio() throws Exception {
    long proveedor = crearProveedor("Distribuidora Andina");
    long cliente = crearCliente("Juan Pérez", "INSTALADOR");
    long otro = crearCliente("Ana Gómez", "CLIENTE_FINAL");
    long camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(camara, "3", "20", List.of("A1", "A2", "A3")));
    long primera = vender(cliente, camara, List.of("A1"));
    reloj.fijar(Instant.parse("2026-10-20T15:00:00Z"));
    vender(cliente, camara, List.of("A2"));
    long anulada = vender(cliente, camara, List.of("A3"));
    enviar(post("/api/v1/ventas/{id}/anular", anulada), Map.of("motivo", "Error"))
        .andExpect(status().isOk());

    sinCuerpo(get("/api/v1/clientes/{id}", cliente))
        .andExpect(jsonPath("$.cantidadMovimientos").value(2))
        .andExpect(jsonPath("$.fechaUltimoMovimiento").value("2026-10-20"));
    sinCuerpo(get("/api/v1/clientes").param("buscar", "Ana"))
        .andExpect(jsonPath("$.contenido[0].cantidadMovimientos").value(0));
    sinCuerpo(get("/api/v1/clientes/{id}/historial", cliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.compras").value(2))
        .andExpect(jsonPath("$.instalaciones").value(0))
        .andExpect(jsonPath("$.movimientos", hasSize(3)))
        .andExpect(jsonPath("$.movimientos[0].consecutivo").value("V-0003"))
        .andExpect(jsonPath("$.movimientos[0].estado").value("ANULADA"))
        .andExpect(jsonPath("$.movimientos[2].descripcion").value("Producto CAM-S × 1 und"))
        .andExpect(jsonPath("$.movimientos[2].total.monto").value("30.0000"));
    sinCuerpo(get("/api/v1/clientes/{id}/historial", otro))
        .andExpect(jsonPath("$.movimientos", hasSize(0)));

    long serial =
        leer(sinCuerpo(get("/api/v1/seriales").param("numero", "A1"))).at("/0/id").asLong();
    sinCuerpo(get("/api/v1/seriales/{id}", serial))
        .andExpect(jsonPath("$.serial.estado").value("VENDIDO"))
        .andExpect(jsonPath("$.serial.documentoSalida.id").value(primera))
        .andExpect(jsonPath("$.serial.vencimientoGarantia").value("2027-01-01"))
        .andExpect(jsonPath("$.movimientos[1].tipo").value("VENTA"))
        .andExpect(jsonPath("$.movimientos[1].detalle", containsString("Cliente: Juan Pérez")));
  }
}
