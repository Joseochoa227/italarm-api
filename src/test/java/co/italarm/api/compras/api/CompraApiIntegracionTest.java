package co.italarm.api.compras.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;

class CompraApiIntegracionTest extends PruebaInventario {

  private long proveedor;
  private long camara;

  @BeforeEach
  void preparar() throws Exception {
    proveedor = crearProveedor("Distribuidora Andina");
    camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
  }

  /** "Hay 10 cámaras a US$ 20" (sección 3.8). */
  private void hayDiezCamarasAVeinte() throws Exception {
    comprar(proveedor, "USD", linea(camara, "10", "20"));
    assertThat(stock(camara)).isEqualByComparingTo("10");
    assertThat(costo(camara)).isEqualByComparingTo("20");
  }

  @Test
  void cp01_compraConPrecioMayor_subeElCosto() throws Exception {
    hayDiezCamarasAVeinte();

    JsonNode compra = comprar(proveedor, "USD", linea(camara, "10", "25"));

    assertThat(stock(camara)).isEqualByComparingTo("20");
    assertThat(costo(camara)).isEqualByComparingTo("25");
    assertThat(compra.at("/lineas/0/regla").asText()).isEqualTo("SUBE");
    assertThat(compra.at("/lineas/0/costoAnteriorUsd/monto").asText()).isEqualTo("20.0000");
  }

  @Test
  void cp02_compraConPrecioMenor_promedia() throws Exception {
    hayDiezCamarasAVeinte();

    JsonNode compra = comprar(proveedor, "USD", linea(camara, "10", "15"));

    assertThat(stock(camara)).isEqualByComparingTo("20");
    assertThat(costo(camara)).isEqualByComparingTo("17.50");
    assertThat(compra.at("/lineas/0/regla").asText()).isEqualTo("PROMEDIO");
  }

  @Test
  void cp03_compraConPrecioIgual_aplicaPromedio() throws Exception {
    hayDiezCamarasAVeinte();

    JsonNode compra = comprar(proveedor, "USD", linea(camara, "10", "20"));

    assertThat(costo(camara)).isEqualByComparingTo("20");
    assertThat(compra.at("/lineas/0/regla").asText()).isEqualTo("PROMEDIO");
  }

  @Test
  void cp04_compraEnCopMasBarata_promediaEnUsd() throws Exception {
    hayDiezCamarasAVeinte();

    JsonNode compra = comprar(proveedor, "COP", linea(camara, "10", "76000"));

    assertThat(stock(camara)).isEqualByComparingTo("20");
    assertThat(costo(camara)).isEqualByComparingTo("19.50");
    assertThat(compra.at("/moneda").asText()).isEqualTo("COP");
    assertThat(compra.at("/tasas/trm").asText()).isEqualTo("4000.000000");
    assertThat(compra.at("/total/monto").asText()).isEqualTo("760000.0000");
    assertThat(compra.at("/totalUsd/monto").asText()).isEqualTo("190.0000");
    Map<String, Object> historial =
        jdbc.queryForMap(
            "select moneda_factura, tasa_factura, costo_factura from historial_costo"
                + " where producto_id = ? order by id desc limit 1",
            camara);
    assertThat(historial.get("moneda_factura")).isEqualTo("COP");
    assertThat((BigDecimal) historial.get("tasa_factura")).isEqualByComparingTo("4000");
    assertThat((BigDecimal) historial.get("costo_factura")).isEqualByComparingTo("76000");
  }

  @Test
  void cp05_compraEnCopMasCara_sube() throws Exception {
    hayDiezCamarasAVeinte();

    comprar(proveedor, "COP", linea(camara, "10", "88000"));

    assertThat(costo(camara)).isEqualByComparingTo("22");
  }

  @Test
  void cp06_compraEnVes_convierteConLaTasaDelBolivar() throws Exception {
    hayDiezCamarasAVeinte();

    JsonNode compra = comprar(proveedor, "VES", linea(camara, "10", "950"));

    assertThat(compra.at("/lineas/0/costoUnitarioUsd/monto").asText()).isEqualTo("19.0000");
    assertThat(compra.at("/tasas/tasaVes").asText()).isEqualTo("50.000000");
    assertThat(costo(camara)).isEqualByComparingTo("19.50");
  }

  @Test
  void cp07_productoSinStock_tomaElCostoDeLaFactura() throws Exception {
    // Stock 0 con costo anterior US$ 20 (como queda tras vender todo).
    jdbc.update("update producto set stock = 0, costo_actual_usd = 20 where id = ?", camara);

    JsonNode compra = comprar(proveedor, "USD", linea(camara, "5", "18"));

    assertThat(stock(camara)).isEqualByComparingTo("5");
    assertThat(costo(camara)).isEqualByComparingTo("18");
    assertThat(compra.at("/lineas/0/regla").asText()).isEqualTo("SIN_STOCK");
  }

  @Test
  void cp13_tresCamarasConDosSeriales_noSeGuarda() throws Exception {
    long conSerial = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);

    enviar(
            post("/api/v1/compras"),
            compra(proveedor, "USD", List.of(linea(conSerial, "3", "20", List.of("A1", "A2")))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("SERIALES_NO_COINCIDEN"));

    assertThat(stock(conSerial)).isEqualByComparingTo("0");
    assertThat(jdbc.queryForObject("select count(*) from compra", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from movimiento_inventario", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from serial", Long.class)).isZero();
  }

  @Test
  void cp16_anularUltimaCompraSinSalidas_revierteStockYCosto() throws Exception {
    hayDiezCamarasAVeinte();
    JsonNode compra = comprar(proveedor, "USD", linea(camara, "10", "25"));
    assertThat(compra.get("anulable").asBoolean()).isTrue();

    enviar(post("/api/v1/compras/{id}/anular", compra.get("id").asLong()), motivo("Factura errada"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ANULADA"))
        .andExpect(jsonPath("$.anulable").value(false))
        .andExpect(jsonPath("$.anulacion.motivo").value("Factura errada"))
        .andExpect(jsonPath("$.anulacion.usuario").value("Jose"));

    assertThat(stock(camara)).isEqualByComparingTo("10");
    assertThat(costo(camara)).isEqualByComparingTo("20");
    assertThat(
            jdbc.queryForObject(
                "select tipo from movimiento_inventario where producto_id = ?"
                    + " order by id desc limit 1",
                String.class,
                camara))
        .isEqualTo("ANULACION_COMPRA");
  }

  @Test
  void laCompraAnteriorNoSePuedeAnularSiHayOtraDespues() throws Exception {
    JsonNode primera = comprar(proveedor, "USD", linea(camara, "10", "20"));
    comprar(proveedor, "USD", linea(camara, "5", "25"));

    sinCuerpo(get("/api/v1/compras/{id}", primera.get("id").asLong()))
        .andExpect(jsonPath("$.anulable").value(false))
        .andExpect(jsonPath("$.motivoNoAnulable", containsString("ajuste")));
    enviar(post("/api/v1/compras/{id}/anular", primera.get("id").asLong()), motivo("Error"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("COMPRA_NO_ANULABLE"));
    assertThat(stock(camara)).isEqualByComparingTo("15");
  }

  @Test
  void anularDosVeces_respondeQueYaEstaAnulada() throws Exception {
    JsonNode compra = comprar(proveedor, "USD", linea(camara, "2", "20"));
    long id = compra.get("id").asLong();
    enviar(post("/api/v1/compras/{id}/anular", id), motivo("Error")).andExpect(status().isOk());

    enviar(post("/api/v1/compras/{id}/anular", id), motivo("Error"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("COMPRA_YA_ANULADA"));
    assertThat(stock(camara)).isEqualByComparingTo("0");
  }

  @Test
  void anularConSeriales_quedanAnuladosYSePuedenVolverARegistrar() throws Exception {
    long conSerial = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    JsonNode compra =
        comprar(proveedor, "USD", linea(conSerial, "2", "20", List.of(" abc1 ", "ABC2")));
    assertThat(compra.at("/lineas/0/seriales/0").asText()).isEqualTo("ABC1");

    enviar(post("/api/v1/compras/{id}/anular", compra.get("id").asLong()), motivo("Error"))
        .andExpect(status().isOk());
    assertThat(jdbc.queryForList("select distinct estado from serial", String.class))
        .containsExactly("ANULADO");

    comprar(proveedor, "USD", linea(conSerial, "2", "20", List.of("ABC1", "ABC2")));
    assertThat(stock(conSerial)).isEqualByComparingTo("2");
  }

  @Test
  void serialYaRegistrado_noSeGuarda() throws Exception {
    long conSerial = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(conSerial, "1", "20", List.of("X1")));

    enviar(
            post("/api/v1/compras"),
            compra(proveedor, "USD", List.of(linea(conSerial, "1", "20", List.of("x1")))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("SERIAL_DUPLICADO"));
    assertThat(stock(conSerial)).isEqualByComparingTo("1");
  }

  @Test
  void laMismaIdempotencyKey_creaUnaSolaCompra() throws Exception {
    Map<String, Object> datos = compra(proveedor, "USD", List.of(linea(camara, "3", "20")));
    long primera =
        leer(mvc.perform(
                    post("/api/v1/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "clave-1")
                        .contentType("application/json")
                        .content(cuerpo(datos)))
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    long segunda =
        leer(mvc.perform(
                    post("/api/v1/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "clave-1")
                        .contentType("application/json")
                        .content(cuerpo(datos)))
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();

    assertThat(segunda).isEqualTo(primera);
    assertThat(jdbc.queryForObject("select count(*) from compra", Long.class)).isEqualTo(1);
    assertThat(stock(camara)).isEqualByComparingTo("3");
  }

  @Test
  void productoRepetidoFechaFuturaYCostoCero_seRechazan() throws Exception {
    enviar(
            post("/api/v1/compras"),
            compra(proveedor, "USD", List.of(linea(camara, "1", "20"), linea(camara, "1", "21"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("COMPRA_PRODUCTO_REPETIDO"));

    Map<String, Object> futura = compra(proveedor, "USD", List.of(linea(camara, "1", "20")));
    futura.put("fecha", "2026-10-02");
    enviar(post("/api/v1/compras"), futura)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("COMPRA_FECHA_FUTURA"));

    enviar(post("/api/v1/compras"), compra(proveedor, "USD", List.of(linea(camara, "1", "0"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACION"));
  }

  @Test
  void cantidadConDecimalesEnUnidad_seRechaza() throws Exception {
    enviar(post("/api/v1/compras"), compra(proveedor, "USD", List.of(linea(camara, "1.5", "20"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"));
    assertThat(jdbc.queryForObject("select count(*) from compra", Long.class)).isZero();
  }

  @Test
  void compraEnCopSinTrm_respondeTasaNoDisponible() throws Exception {
    jdbc.update("delete from tasa_cambio where par = 'USD_COP'");

    enviar(post("/api/v1/compras"), compra(proveedor, "COP", List.of(linea(camara, "1", "80000"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("TASA_NO_DISPONIBLE"));
  }

  @Test
  void compraConFechaAnterior_usaLaUltimaTasaAnteriorYLaGuarda() throws Exception {
    registrarTasa("USD_COP", HOY.minusDays(5), "3900");
    Map<String, Object> datos = compra(proveedor, "COP", List.of(linea(camara, "1", "39000")));
    datos.put("fecha", "2026-09-28");

    JsonNode compra = leer(enviar(post("/api/v1/compras"), datos).andExpect(status().isCreated()));

    assertThat(compra.at("/fecha").asText()).isEqualTo("2026-09-28");
    assertThat(compra.at("/tasas/fechaTrm").asText()).isEqualTo("2026-09-26");
    assertThat(compra.at("/tasas/trm").asText()).isEqualTo("3900.000000");
    assertThat(compra.at("/tasas/tasaVes").isNull()).isTrue();
    assertThat(costo(camara)).isEqualByComparingTo("10");
  }

  @Test
  void vistaPrevia_muestraElCambioDeCostoSinGuardar() throws Exception {
    hayDiezCamarasAVeinte();
    Map<String, Object> datos = compra(proveedor, "COP", List.of(linea(camara, "10", "76000")));

    enviar(post("/api/v1/compras/vista-previa"), datos)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasas.trm").value("4000.000000"))
        .andExpect(jsonPath("$.avisos", hasSize(0)))
        .andExpect(jsonPath("$.lineas[0].stockActual").value("10"))
        .andExpect(jsonPath("$.lineas[0].costoActualUsd.monto").value("20.0000"))
        .andExpect(jsonPath("$.lineas[0].costoNuevoUsd.monto").value("19.5000"))
        .andExpect(jsonPath("$.lineas[0].regla").value("PROMEDIO"))
        .andExpect(jsonPath("$.lineas[0].costoUnitarioUsd.monto").value("19.0000"))
        .andExpect(jsonPath("$.lineas[0].subtotal.cop.monto").value("760000.0000"))
        .andExpect(jsonPath("$.lineas[0].subtotal.usd.monto").value("190.000000"))
        .andExpect(jsonPath("$.total.ves.monto").value("9500.000000"));

    assertThat(costo(camara)).isEqualByComparingTo("20");
    assertThat(jdbc.queryForObject("select count(*) from compra", Long.class)).isEqualTo(1);
  }

  @Test
  void vistaPrevia_avisaSiLaTasaNoEsDeLaFecha() throws Exception {
    jdbc.update("delete from tasa_cambio");
    registrarTasa("USD_COP", HOY.minusDays(1), "4100");
    Map<String, Object> datos = compra(proveedor, "USD", List.of(linea(camara, "1", "20")));

    enviar(post("/api/v1/compras/vista-previa"), datos)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avisos", hasSize(2)))
        .andExpect(jsonPath("$.avisos[0]", containsString("30/09/2026")))
        .andExpect(jsonPath("$.avisos[1]", containsString("bolívar")));
  }

  @Test
  void listado_filtraYTotalizaSinLasAnuladas() throws Exception {
    long cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    long otro = crearProveedor("Importadora Caracas");
    comprar(proveedor, "USD", linea(camara, "2", "20"));
    Map<String, Object> mixta =
        compra(
            proveedor, "COP", List.of(linea(cable, "100.5", "1000"), linea(camara, "1", "80000")));
    enviar(post("/api/v1/compras"), mixta).andExpect(status().isCreated());
    JsonNode anulada = comprar(otro, "VES", linea(cable, "10", "50"));
    enviar(post("/api/v1/compras/{id}/anular", anulada.get("id").asLong()), motivo("Error"))
        .andExpect(status().isOk());

    sinCuerpo(get("/api/v1/compras"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.desde").value("2026-10-01"))
        .andExpect(jsonPath("$.hasta").value("2026-10-31"))
        .andExpect(jsonPath("$.compras.contenido", hasSize(3)))
        .andExpect(jsonPath("$.compras.contenido[0].consecutivo").value("C-0003"))
        .andExpect(jsonPath("$.compras.contenido[0].estado").value("ANULADA"))
        .andExpect(
            jsonPath("$.compras.contenido[1].proveedor.nombre").value("Distribuidora Andina"))
        .andExpect(
            jsonPath("$.compras.contenido[1].productos")
                .value("Producto UTP × 100.5 m, Producto CAM-1 × 1 und"))
        .andExpect(jsonPath("$.compras.contenido[1].registradaPor").value("Jose"))
        .andExpect(jsonPath("$.totalesPorMoneda", hasSize(2)))
        .andExpect(jsonPath("$.totalesPorMoneda[0].total.moneda").value("COP"))
        .andExpect(jsonPath("$.totalesPorMoneda[0].total.monto").value("180500.0000"))
        .andExpect(jsonPath("$.totalesPorMoneda[1].total.moneda").value("USD"))
        .andExpect(jsonPath("$.totalUsd.monto").value("85.1250"));

    sinCuerpo(get("/api/v1/compras").param("incluirAnuladas", "false"))
        .andExpect(jsonPath("$.compras.contenido", hasSize(2)));
    sinCuerpo(get("/api/v1/compras").param("proveedorId", String.valueOf(otro)))
        .andExpect(jsonPath("$.compras.contenido", hasSize(1)))
        .andExpect(jsonPath("$.totalesPorMoneda", hasSize(0)))
        .andExpect(jsonPath("$.totalUsd.monto").value("0"));
    sinCuerpo(get("/api/v1/compras").param("productoId", String.valueOf(cable)))
        .andExpect(jsonPath("$.compras.contenido", hasSize(2)));
    sinCuerpo(get("/api/v1/compras").param("desde", "2026-09-01").param("hasta", "2026-09-30"))
        .andExpect(jsonPath("$.compras.contenido", hasSize(0)));
  }

  @Test
  void facturaEnPdfYEnImagen_seAdjuntaReemplazaYQuita() throws Exception {
    JsonNode compra = comprar(proveedor, "USD", linea(camara, "1", "20"));
    long id = compra.get("id").asLong();
    assertThat(compra.get("facturaUrl").isNull()).isTrue();
    byte[] pdf = "%PDF-1.7 factura".getBytes(StandardCharsets.US_ASCII);
    byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};

    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/compras/{id}/factura", id)
                .file(new MockMultipartFile("archivo", "f.pdf", "application/pdf", pdf))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.facturaUrl", containsString(".pdf")));
    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/compras/{id}/factura", id)
                .file(new MockMultipartFile("archivo", "f.jpg", "image/jpeg", jpeg))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.facturaUrl", containsString(".jpg")));
    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/compras/{id}/factura", id)
                .file(new MockMultipartFile("archivo", "f.txt", "text/plain", "hola".getBytes()))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ARCHIVO_TIPO_NO_PERMITIDO"));

    sinCuerpo(delete("/api/v1/compras/{id}/factura", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.facturaUrl").value(nullValue()));
  }

  @Test
  void detalle_deCompraInexistente_respondeNoEncontrado() throws Exception {
    sinCuerpo(get("/api/v1/compras/999999")).andExpect(status().isNotFound());
  }

  @Test
  void proveedorInexistenteYProductoInactivo_seRechazan() throws Exception {
    enviar(post("/api/v1/compras"), compra(999999, "USD", List.of(linea(camara, "1", "20"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("PROVEEDOR_NO_EXISTE"));

    sinCuerpo(post("/api/v1/productos/{id}/desactivar", camara)).andExpect(status().isOk());
    enviar(post("/api/v1/compras"), compra(proveedor, "USD", List.of(linea(camara, "1", "20"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("PRODUCTO_INACTIVO"));
  }

  @Test
  void elConsecutivoEmpiezaEnC0001() throws Exception {
    JsonNode compra = comprar(proveedor, "USD", linea(camara, "1", "20"));
    assertThat(compra.get("consecutivo").asText()).isEqualTo("C-0001");
    assertThat(compra.at("/proveedor/nombre").asText()).startsWith("Distribuidora");
    sinCuerpo(get("/api/v1/compras/{id}", compra.get("id").asLong()))
        .andExpect(jsonPath("$.registradaPor", startsWith("Jose")));
  }

  private static Map<String, Object> motivo(String motivo) {
    return Map.of("motivo", motivo);
  }
}
