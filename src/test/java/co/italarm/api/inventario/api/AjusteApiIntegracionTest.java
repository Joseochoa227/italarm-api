package co.italarm.api.inventario.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AjusteApiIntegracionTest extends PruebaInventario {

  private long proveedor;

  @BeforeEach
  void preparar() {
    proveedor = crearProveedor("Distribuidora Andina");
  }

  private static Map<String, Object> ajuste(
      long productoId, String motivo, String cantidad, List<String> seriales) {
    Map<String, Object> ajuste = new HashMap<>();
    ajuste.put("productoId", productoId);
    ajuste.put("motivo", motivo);
    ajuste.put("cantidad", cantidad);
    ajuste.put("seriales", seriales);
    return ajuste;
  }

  @Test
  void cp19_conteoFisicoConDosConectoresDeMas_entraAlCostoVigente() throws Exception {
    long conector = crearProducto("CON-BNC", 7, UNIDAD, false);
    comprar(proveedor, "USD", linea(conector, "100", "0.50"));

    Map<String, Object> datos = ajuste(conector, "CONTEO_FISICO", "2", List.of());
    datos.put("costoUnitarioUsd", "9.99");
    enviar(post("/api/v1/ajustes"), datos)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.consecutivo").value("AJ-001"))
        .andExpect(jsonPath("$.tipo").value("ENTRADA"))
        .andExpect(jsonPath("$.motivoEtiqueta").value("Conteo físico"))
        .andExpect(jsonPath("$.cantidad").value("2"))
        .andExpect(jsonPath("$.costoUnitarioUsd.monto").value("0.5000"))
        .andExpect(jsonPath("$.valorUsd.monto").value("1.0000"))
        .andExpect(jsonPath("$.registradoPor").value("Jose"));

    assertThat(stock(conector)).isEqualByComparingTo("102");
    assertThat(costo(conector)).isEqualByComparingTo("0.50");
    Map<String, Object> movimiento =
        jdbc.queryForMap(
            "select tipo, detalle, documento_consecutivo from movimiento_inventario"
                + " where producto_id = ? order by id desc limit 1",
            conector);
    assertThat(movimiento)
        .containsEntry("tipo", "AJUSTE_ENTRADA")
        .containsEntry("detalle", "Conteo físico")
        .containsEntry("documento_consecutivo", "AJ-001");
  }

  @Test
  void cp17_anularCompraConUnaUnidadVendida_noSePermite() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
    JsonNode compra = comprar(proveedor, "USD", linea(camara, "5", "20"));
    // En esta fase la venta se simula con un ajuste de salida.
    enviar(post("/api/v1/ajustes"), ajuste(camara, "PERDIDA", "-1", List.of()))
        .andExpect(status().isCreated());

    enviar(
            post("/api/v1/compras/{id}/anular", compra.get("id").asLong()),
            Map.of("motivo", "Error"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("COMPRA_NO_ANULABLE"))
        .andExpect(jsonPath("$.detail", containsString("ajuste")));
    assertThat(stock(camara)).isEqualByComparingTo("4");
  }

  @Test
  void entradaDeProductoSinCosto_exigeElCostoYLoDejaComoCostoActual() throws Exception {
    long regalo = crearProducto("CAJA", 8, UNIDAD, false);

    enviar(post("/api/v1/ajustes"), ajuste(regalo, "CONTEO_FISICO", "3", List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("COSTO_REQUERIDO"));

    Map<String, Object> datos = ajuste(regalo, "CONTEO_FISICO", "3", List.of());
    datos.put("costoUnitarioUsd", "1.25");
    enviar(post("/api/v1/ajustes"), datos).andExpect(status().isCreated());

    assertThat(stock(regalo)).isEqualByComparingTo("3");
    assertThat(costo(regalo)).isEqualByComparingTo("1.25");
    assertThat(
            jdbc.queryForObject(
                "select regla from historial_costo where producto_id = ?", String.class, regalo))
        .isEqualTo("AJUSTE");
  }

  @Test
  void salidaMayorQueElStock_respondeStockInsuficiente() throws Exception {
    long cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    comprar(proveedor, "USD", linea(cable, "50", "0.30"));

    enviar(post("/api/v1/ajustes"), ajuste(cable, "DANO", "-60", List.of()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
        .andExpect(jsonPath("$.detail").value("Stock insuficiente · quedan 50 m"));
    enviar(post("/api/v1/ajustes"), ajuste(cable, "DANO", "-12.5", List.of()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tipo").value("SALIDA"))
        .andExpect(jsonPath("$.cantidad").value("12.5"));
    assertThat(stock(cable)).isEqualByComparingTo("37.5");
  }

  @Test
  void salidaConSerial_loDaDeBajaYSoloSiEstaEnBodega() throws Exception {
    long camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("S1", "S2")));

    enviar(post("/api/v1/ajustes"), ajuste(camara, "DANO", "-1", List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("SERIALES_NO_COINCIDEN"));
    enviar(post("/api/v1/ajustes"), ajuste(camara, "DANO", "-1", List.of("NO-EXISTE")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("SERIAL_NO_DISPONIBLE"));
    enviar(post("/api/v1/ajustes"), ajuste(camara, "DANO", "-1", List.of("s1")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.seriales[0]").value("S1"));
    enviar(post("/api/v1/ajustes"), ajuste(camara, "GARANTIA", "-1", List.of("S1")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("SERIAL_NO_DISPONIBLE"));

    assertThat(jdbc.queryForObject("select estado from serial where numero = 'S1'", String.class))
        .isEqualTo("DADO_DE_BAJA");
    assertThat(stock(camara)).isEqualByComparingTo("1");
  }

  @Test
  void entradaConSerial_registraLosNuevos() throws Exception {
    long camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(camara, "1", "20", List.of("S1")));

    enviar(post("/api/v1/ajustes"), ajuste(camara, "CONTEO_FISICO", "1", List.of("S1")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("SERIAL_DUPLICADO"));
    enviar(post("/api/v1/ajustes"), ajuste(camara, "CONTEO_FISICO", "1", List.of("S9")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.seriales[0]").value("S9"));
    assertThat(stock(camara)).isEqualByComparingTo("2");
  }

  @Test
  void motivoOtroSinDescripcionYCantidadCero_seRechazan() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
    comprar(proveedor, "USD", linea(camara, "5", "20"));

    enviar(post("/api/v1/ajustes"), ajuste(camara, "OTRO", "1", List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("AJUSTE_INVALIDO"));
    enviar(post("/api/v1/ajustes"), ajuste(camara, "PERDIDA", "0", List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("AJUSTE_INVALIDO"));
    enviar(post("/api/v1/ajustes"), ajuste(camara, "PERDIDA", "-1.5", List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"));

    Map<String, Object> otro = ajuste(camara, "OTRO", "1", List.of());
    otro.put("descripcion", "Unidad devuelta por un técnico");
    enviar(post("/api/v1/ajustes"), otro)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.descripcion").value("Unidad devuelta por un técnico"));
    assertThat(
            jdbc.queryForObject(
                "select detalle from movimiento_inventario where tipo = 'AJUSTE_ENTRADA'",
                String.class))
        .isEqualTo("Otro: Unidad devuelta por un técnico");
  }

  @Test
  void listadoYDetalle() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
    long cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    comprar(proveedor, "USD", linea(cable, "10", "1"));
    long id =
        leer(enviar(post("/api/v1/ajustes"), ajuste(camara, "PERDIDA", "-1", List.of()))
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    enviar(post("/api/v1/ajustes"), ajuste(cable, "DANO", "-2", List.of()))
        .andExpect(status().isCreated());

    sinCuerpo(get("/api/v1/ajustes"))
        .andExpect(jsonPath("$.contenido", hasSize(2)))
        .andExpect(jsonPath("$.contenido[0].consecutivo").value("AJ-002"));
    sinCuerpo(get("/api/v1/ajustes").param("productoId", String.valueOf(camara)))
        .andExpect(jsonPath("$.contenido", hasSize(1)))
        .andExpect(jsonPath("$.contenido[0].producto.codigo").value("CAM-1"));
    sinCuerpo(get("/api/v1/ajustes").param("desde", "2026-10-02"))
        .andExpect(jsonPath("$.contenido", hasSize(0)));
    sinCuerpo(get("/api/v1/ajustes").param("hasta", "2026-10-01"))
        .andExpect(jsonPath("$.contenido", hasSize(2)));
    sinCuerpo(get("/api/v1/ajustes/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.motivo").value("PERDIDA"));
    sinCuerpo(get("/api/v1/ajustes/999999")).andExpect(status().isNotFound());
  }

  @Test
  void laMismaIdempotencyKey_creaUnSoloAjuste() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    String cuerpo = cuerpo(ajuste(camara, "PERDIDA", "-1", List.of()));

    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/api/v1/ajustes")
                  .header(HttpHeaders.AUTHORIZATION, bearer(token))
                  .header("Idempotency-Key", "ajuste-1")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(cuerpo))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.consecutivo").value("AJ-001"));
    }
    assertThat(stock(camara)).isEqualByComparingTo("4");
  }

  /** BP-08: dos salidas simultáneas de la última unidad; una se guarda y la otra no. */
  @Test
  void dosAjustesDeSalidaSimultaneosDeLaUltimaUnidad_soloUnoSeGuarda() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
    comprar(proveedor, "USD", linea(camara, "1", "20"));
    String cuerpo = cuerpo(ajuste(camara, "PERDIDA", "-1", List.of()));
    CountDownLatch salida = new CountDownLatch(1);
    Callable<Integer> tarea =
        () -> {
          salida.await();
          return mvc.perform(
                  post("/api/v1/ajustes")
                      .header(HttpHeaders.AUTHORIZATION, bearer(token))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(cuerpo))
              .andReturn()
              .getResponse()
              .getStatus();
        };

    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> resultados = new ArrayList<>();
      resultados.add(hilos.submit(tarea));
      resultados.add(hilos.submit(tarea));
      salida.countDown();
      List<Integer> estados = new ArrayList<>();
      for (Future<Integer> resultado : resultados) {
        estados.add(resultado.get());
      }
      assertThat(estados).containsExactlyInAnyOrder(201, 422);
    } finally {
      hilos.shutdownNow();
    }
    assertThat(stock(camara)).isEqualByComparingTo("0");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from movimiento_inventario where tipo = 'AJUSTE_SALIDA'",
                Long.class))
        .isEqualTo(1);
  }
}
