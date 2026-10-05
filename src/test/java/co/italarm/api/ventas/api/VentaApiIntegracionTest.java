package co.italarm.api.ventas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
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

class VentaApiIntegracionTest extends PruebaInventario {

  private long proveedor;
  private long instalador;
  private long clienteFinal;
  private long camara;

  @BeforeEach
  void preparar() throws Exception {
    proveedor = crearProveedor("Distribuidora Andina");
    instalador = crearCliente("Juan Instalador", "INSTALADOR");
    clienteFinal = crearCliente("Ana Cliente", "CLIENTE_FINAL");
    camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
  }

  private static Map<String, Object> lineaVenta(long productoId, String cantidad, String precio) {
    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", productoId);
    linea.put("cantidad", cantidad);
    linea.put("precioUnitario", precio);
    return linea;
  }

  private static Map<String, Object> lineaConSeriales(long productoId, List<String> seriales) {
    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", productoId);
    linea.put("seriales", seriales);
    return linea;
  }

  private static Map<String, Object> venta(
      long clienteId, String moneda, List<Map<String, Object>> lineas) {
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", clienteId);
    venta.put("moneda", moneda);
    venta.put("lineas", new ArrayList<>(lineas));
    return venta;
  }

  private JsonNode vender(Map<String, Object> venta) throws Exception {
    return leer(enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated()));
  }

  @Test
  void cp08_ventaConCosto1750_conservaSuUtilidadAunqueElCostoSuba() throws Exception {
    comprar(proveedor, "USD", linea(camara, "10", "20"));
    comprar(proveedor, "USD", linea(camara, "10", "15"));
    assertThat(costo(camara)).isEqualByComparingTo("17.50");

    JsonNode venta = vender(venta(instalador, "USD", List.of(lineaVenta(camara, "1", "30"))));
    comprar(proveedor, "USD", linea(camara, "10", "25"));
    assertThat(costo(camara)).isEqualByComparingTo("25");

    sinCuerpo(get("/api/v1/ventas/{id}", venta.get("id").asLong()))
        .andExpect(jsonPath("$.lineas[0].costoUnitarioUsd.monto").value("17.5000"))
        .andExpect(jsonPath("$.resumen.costo.usd.monto").value("17.5000"))
        .andExpect(jsonPath("$.utilidad.monto").value("12.5000"))
        .andExpect(jsonPath("$.porcentajeUtilidad").value("41.67"));
  }

  @Test
  void cp09_costoConTasaDeHoyYDeLaUltimaCompra() throws Exception {
    comprar(proveedor, "USD", linea(camara, "10", "20"));
    comprar(proveedor, "COP", linea(camara, "10", "76000"));
    assertThat(costo(camara)).isEqualByComparingTo("19.50");
    registrarTasa("USD_COP", HOY, "4200");

    enviar(
            post("/api/v1/ventas/vista-previa"),
            venta(clienteFinal, "COP", List.of(lineaVenta(camara, "1", null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lineas[0].costoUnitarioHoy.cop.monto").value("81900.000000"))
        .andExpect(
            jsonPath("$.lineas[0].costoUnitarioUltimaCompra.cop.monto").value("78000.000000"))
        .andExpect(jsonPath("$.lineas[0].ultimaCompra.consecutivo").value("C-0002"))
        .andExpect(jsonPath("$.lineas[0].precioSugerido.monto").value(168000));
  }

  @Test
  void cp12_sinTasaDelBolivarHoy_usaLaUltimaConAviso() throws Exception {
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    jdbc.update("delete from tasa_cambio where par = 'USD_VES'");
    registrarTasa("USD_VES", HOY.minusDays(2), "48");

    enviar(
            post("/api/v1/ventas/vista-previa"),
            venta(clienteFinal, "VES", List.of(lineaVenta(camara, "1", null))))
        .andExpect(jsonPath("$.tasas.tasaVes").value("48.000000"))
        .andExpect(jsonPath("$.avisos", hasSize(1)))
        .andExpect(jsonPath("$.avisos[0]", containsString("29/09/2026")))
        .andExpect(jsonPath("$.lineas[0].precioSugerido.monto").value("1920.00"));
    JsonNode venta = vender(venta(clienteFinal, "VES", List.of(lineaVenta(camara, "1", null))));
    assertThat(venta.at("/tasas/fechaTasaVes").asText()).isEqualTo("2026-09-29");
    assertThat(venta.at("/total/monto").asText()).isEqualTo("1920.0000");
  }

  @Test
  void cp14_serialUsadoEnOtraSalida_noEstaDisponible() throws Exception {
    long conSerial = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(conSerial, "3", "20", List.of("S1", "S2", "S3")));
    vender(venta(instalador, "USD", List.of(lineaConSeriales(conSerial, List.of("S1")))));
    Map<String, Object> baja = new HashMap<>();
    baja.put("productoId", conSerial);
    baja.put("motivo", "DANO");
    baja.put("cantidad", "-1");
    baja.put("seriales", List.of("S2"));
    enviar(post("/api/v1/ajustes"), baja).andExpect(status().isCreated());

    for (String usado : List.of("S1", "S2")) {
      enviar(
              post("/api/v1/ventas"),
              venta(clienteFinal, "USD", List.of(lineaConSeriales(conSerial, List.of(usado)))))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.codigo").value("SERIAL_NO_DISPONIBLE"));
    }
    sinCuerpo(
            get("/api/v1/inventario/productos/{id}/seriales", conSerial)
                .param("estado", "EN_BODEGA"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].numero").value("S3"));
    assertThat(stock(conSerial)).isEqualByComparingTo("1");
  }

  @Test
  void cp18_anularVentaDeDosCamarasConSerial_devuelveStockYSeriales() throws Exception {
    long conSerial = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(conSerial, "3", "20", List.of("S1", "S2", "S3")));
    JsonNode venta =
        vender(venta(instalador, "USD", List.of(lineaConSeriales(conSerial, List.of("s1", "S2")))));
    assertThat(venta.at("/lineas/0/cantidad").asText()).isEqualTo("2");
    assertThat(stock(conSerial)).isEqualByComparingTo("1");

    enviar(
            post("/api/v1/ventas/{id}/anular", venta.get("id").asLong()),
            Map.of("motivo", "El cliente desistió"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ANULADA"))
        .andExpect(jsonPath("$.anulacion.motivo").value("El cliente desistió"))
        .andExpect(jsonPath("$.anulacion.usuario").value("Jose"))
        .andExpect(jsonPath("$.lineas[0].seriales", hasSize(0)));

    assertThat(stock(conSerial)).isEqualByComparingTo("3");
    assertThat(costo(conSerial)).isEqualByComparingTo("20");
    assertThat(
            jdbc.queryForList(
                "select estado from serial where producto_id = ? and vencimiento_garantia is null",
                String.class,
                conSerial))
        .containsOnly("EN_BODEGA")
        .hasSize(3);
    assertThat(
            jdbc.queryForObject(
                "select tipo from movimiento_inventario where producto_id = ?"
                    + " order by id desc limit 1",
                String.class,
                conSerial))
        .isEqualTo("ANULACION_VENTA");
    enviar(post("/api/v1/ventas/{id}/anular", venta.get("id").asLong()), Map.of("motivo", "x"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("VENTA_YA_ANULADA"));
  }

  @Test
  void cp25_ventaDel15DeOctubre_garantiaHasta15DeEnero() throws Exception {
    long conSerial = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(conSerial, "1", "20", List.of("G1")));
    reloj.fijar(Instant.parse("2026-10-15T15:00:00Z"));

    JsonNode venta =
        vender(venta(clienteFinal, "USD", List.of(lineaConSeriales(conSerial, List.of("G1")))));

    assertThat(venta.at("/fecha").asText()).isEqualTo("2026-10-15");
    assertThat(venta.at("/lineas/0/seriales/0/vencimientoGarantia").asText())
        .isEqualTo("2027-01-15");
    JsonNode serial = leer(sinCuerpo(get("/api/v1/seriales").param("numero", "G1")));
    assertThat(serial.at("/0/estado").asText()).isEqualTo("VENDIDO");
    assertThat(serial.at("/0/documentoSalida/consecutivo").asText()).isEqualTo("V-0001");
  }

  @Test
  void cp27_ventaDe100ConDescuentoDe7_total93YUtilidadSobre93() throws Exception {
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    Map<String, Object> datos = venta(instalador, "USD", List.of(lineaVenta(camara, "2", "50")));
    datos.put("descuentoTipo", "VALOR");
    datos.put("descuentoValor", "7");

    JsonNode venta = vender(datos);

    assertThat(venta.at("/consecutivo").asText()).isEqualTo("V-0001");
    assertThat(venta.at("/resumen/subtotal/usd/monto").asText()).isEqualTo("100.0000");
    assertThat(venta.at("/resumen/descuento/usd/monto").asText()).isEqualTo("7.0000");
    assertThat(venta.at("/total/monto").asText()).isEqualTo("93.0000");
    assertThat(venta.at("/utilidad/monto").asText()).isEqualTo("53.0000");
    assertThat(venta.at("/porcentajeUtilidad").asText()).isEqualTo("56.99");
    assertThat(venta.at("/resumen/total/cop/monto").asText()).isEqualTo("372000.000000");
  }

  @Test
  void ventaSinStock_seRechazaYNoGuardaNada() throws Exception {
    comprar(proveedor, "USD", linea(camara, "2", "20"));

    enviar(post("/api/v1/ventas"), venta(instalador, "USD", List.of(lineaVenta(camara, "3", null))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
        .andExpect(jsonPath("$.detail").value("Stock insuficiente · quedan 2 und"));

    assertThat(stock(camara)).isEqualByComparingTo("2");
    assertThat(jdbc.queryForObject("select count(*) from venta", Long.class)).isZero();
    enviar(
            post("/api/v1/ventas/vista-previa"),
            venta(instalador, "USD", List.of(lineaVenta(camara, "3", null))))
        .andExpect(jsonPath("$.puedeGuardar").value(false))
        .andExpect(jsonPath("$.lineas[0].disponible").value("2"))
        .andExpect(jsonPath("$.lineas[0].avisoStock").value("Stock insuficiente · quedan 2 und"));
  }

  @Test
  void precioSugeridoSegunElTipoDeCliente_yPrecioManualPorDebajoDelCosto() throws Exception {
    comprar(proveedor, "USD", linea(camara, "5", "20"));

    enviar(
            post("/api/v1/ventas/vista-previa"),
            venta(instalador, "USD", List.of(lineaVenta(camara, "1", null))))
        .andExpect(
            jsonPath("$.cliente.precioAplicado").value("Se le aplicará el precio instalador"))
        .andExpect(jsonPath("$.lineas[0].precioSugerido.monto").value("30.0000"))
        .andExpect(jsonPath("$.lineas[0].avisoPrecio").value(nullValue()))
        .andExpect(jsonPath("$.puedeGuardar").value(true));
    enviar(
            post("/api/v1/ventas/vista-previa"),
            venta(clienteFinal, "USD", List.of(lineaVenta(camara, "1", null))))
        .andExpect(jsonPath("$.lineas[0].precioSugerido.monto").value("40.0000"));
    enviar(
            post("/api/v1/ventas/vista-previa"),
            venta(clienteFinal, "COP", List.of(lineaVenta(camara, "1", null))))
        .andExpect(jsonPath("$.lineas[0].precioSugerido.monto").value(160000));

    JsonNode venta = vender(venta(clienteFinal, "USD", List.of(lineaVenta(camara, "1", "15"))));
    assertThat(venta.at("/lineas/0/precioSugerido/monto").asText()).isEqualTo("40.0000");
    assertThat(venta.at("/lineas/0/precioUnitario/monto").asText()).isEqualTo("15.0000");
    assertThat(venta.at("/utilidad/monto").asText()).isEqualTo("-5.0000");
    enviar(
            post("/api/v1/ventas/vista-previa"),
            venta(clienteFinal, "USD", List.of(lineaVenta(camara, "1", "15"))))
        .andExpect(jsonPath("$.lineas[0].avisoPrecio", containsString("por debajo del costo")));
  }

  @Test
  void descuentosEnPorcentajeYMayorQueElSubtotal() throws Exception {
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    Map<String, Object> porcentaje =
        venta(instalador, "USD", List.of(lineaVenta(camara, "2", "50")));
    porcentaje.put("descuentoTipo", "PORCENTAJE");
    porcentaje.put("descuentoValor", "10");
    enviar(post("/api/v1/ventas/vista-previa"), porcentaje)
        .andExpect(jsonPath("$.resumen.total.usd.monto").value("90.0000"));

    Map<String, Object> excesivo = venta(instalador, "USD", List.of(lineaVenta(camara, "2", "50")));
    excesivo.put("descuentoTipo", "VALOR");
    excesivo.put("descuentoValor", "101");
    enviar(post("/api/v1/ventas"), excesivo)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("DESCUENTO_INVALIDO"));
    assertThat(stock(camara)).isEqualByComparingTo("5");
  }

  @Test
  void productoRepetidoClienteInexistenteYProductoInactivo_seRechazan() throws Exception {
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    enviar(
            post("/api/v1/ventas"),
            venta(
                instalador,
                "USD",
                List.of(lineaVenta(camara, "1", null), lineaVenta(camara, "1", null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VENTA_PRODUCTO_REPETIDO"));
    enviar(post("/api/v1/ventas"), venta(999999, "USD", List.of(lineaVenta(camara, "1", null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CLIENTE_NO_EXISTE"));
    sinCuerpo(post("/api/v1/productos/{id}/desactivar", camara)).andExpect(status().isOk());
    enviar(post("/api/v1/ventas"), venta(instalador, "USD", List.of(lineaVenta(camara, "1", null))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("PRODUCTO_INACTIVO"));
  }

  @Test
  void listadoEdicionYTotales() throws Exception {
    long cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    JsonNode primera =
        vender(
            venta(
                instalador,
                "USD",
                List.of(lineaVenta(camara, "1", "30"), lineaVenta(cable, "12.5", "1"))));
    vender(venta(clienteFinal, "COP", List.of(lineaVenta(camara, "1", "160000"))));
    JsonNode anulada = vender(venta(clienteFinal, "USD", List.of(lineaVenta(camara, "1", "40"))));
    enviar(post("/api/v1/ventas/{id}/anular", anulada.get("id").asLong()), Map.of("motivo", "x"))
        .andExpect(status().isOk());

    sinCuerpo(get("/api/v1/ventas"))
        .andExpect(jsonPath("$.ventas.contenido", hasSize(3)))
        .andExpect(jsonPath("$.ventas.contenido[0].consecutivo").value("V-0003"))
        .andExpect(
            jsonPath("$.ventas.contenido[2].productos")
                .value("Producto CAM-1 × 1 und, Producto UTP × 12.5 m"))
        .andExpect(jsonPath("$.ventas.contenido[2].cliente").value("Juan Instalador"))
        .andExpect(jsonPath("$.totalesPorMoneda", hasSize(2)))
        .andExpect(jsonPath("$.totalesPorMoneda[0].total.monto").value("160000.0000"))
        .andExpect(jsonPath("$.totalesPorMoneda[1].total.monto").value("42.5000"))
        .andExpect(jsonPath("$.totalUsd.monto").value("82.5000"));
    sinCuerpo(get("/api/v1/ventas").param("clienteId", String.valueOf(clienteFinal)))
        .andExpect(jsonPath("$.ventas.contenido", hasSize(2)));
    sinCuerpo(get("/api/v1/ventas").param("productoId", String.valueOf(cable)))
        .andExpect(jsonPath("$.ventas.contenido", hasSize(1)));
    sinCuerpo(get("/api/v1/ventas").param("incluirAnuladas", "false"))
        .andExpect(jsonPath("$.ventas.contenido", hasSize(2)));

    long id = primera.get("id").asLong();
    Map<String, Object> edicion = new HashMap<>();
    edicion.put("observaciones", "Entregado en obra");
    edicion.put("monedasComprobante", List.of("COP", "VES", "USD"));
    edicion.put("version", primera.get("version").asLong());
    enviar(put("/api/v1/ventas/{id}", id), edicion)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.observaciones").value("Entregado en obra"))
        .andExpect(jsonPath("$.monedasComprobante", hasSize(2)));
    enviar(put("/api/v1/ventas/{id}", id), edicion)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("MODIFICADO_POR_OTRO_USUARIO"));
    sinCuerpo(get("/api/v1/ventas/999999")).andExpect(status().isNotFound());
  }

  @Test
  void laMismaIdempotencyKey_creaUnaSolaVenta() throws Exception {
    comprar(proveedor, "USD", linea(camara, "5", "20"));
    String cuerpo = cuerpo(venta(instalador, "USD", List.of(lineaVenta(camara, "1", null))));
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/api/v1/ventas")
                  .header(HttpHeaders.AUTHORIZATION, bearer(token))
                  .header("Idempotency-Key", "venta-1")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(cuerpo))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.consecutivo").value("V-0001"));
    }
    assertThat(stock(camara)).isEqualByComparingTo("4");
  }

  /** BP-25: dos ventas simultáneas de la última unidad; una se guarda y la otra no. */
  @Test
  void dosVentasSimultaneasDeLaUltimaUnidad_soloUnaSeGuarda() throws Exception {
    comprar(proveedor, "USD", linea(camara, "1", "20"));
    String cuerpo = cuerpo(venta(instalador, "USD", List.of(lineaVenta(camara, "1", null))));
    CountDownLatch salida = new CountDownLatch(1);
    Callable<Integer> tarea =
        () -> {
          salida.await();
          return mvc.perform(
                  post("/api/v1/ventas")
                      .header(HttpHeaders.AUTHORIZATION, bearer(token))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(cuerpo))
              .andReturn()
              .getResponse()
              .getStatus();
        };
    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> resultados = List.of(hilos.submit(tarea), hilos.submit(tarea));
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
    assertThat(jdbc.queryForObject("select count(*) from venta", Long.class)).isEqualTo(1);
  }

  @Test
  void elStockSigueIgualALaSumaDelKardexConVentasYAnulaciones() throws Exception {
    comprar(proveedor, "USD", linea(camara, "10", "20"));
    JsonNode venta = vender(venta(instalador, "USD", List.of(lineaVenta(camara, "4", null))));
    vender(venta(clienteFinal, "USD", List.of(lineaVenta(camara, "1", null))));
    enviar(post("/api/v1/ventas/{id}/anular", venta.get("id").asLong()), Map.of("motivo", "x"))
        .andExpect(status().isOk());

    Map<String, Object> kardex =
        jdbc.queryForMap(
            "select sum(entrada) - sum(salida) as neto from movimiento_inventario"
                + " where producto_id = ?",
            camara);
    assertThat((BigDecimal) kardex.get("neto")).isEqualByComparingTo(stock(camara));
    assertThat(stock(camara)).isEqualByComparingTo("9");
    assertThat(costo(camara)).isEqualByComparingTo("20");
  }
}
