package co.italarm.api.instalaciones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class InstalacionApiIntegracionTest extends PruebaInventario {

  private long proveedor;
  private long cliente;
  private long camara;
  private long cable;

  @BeforeEach
  void preparar() throws Exception {
    proveedor = crearProveedor("Distribuidora Andina");
    cliente = crearCliente("Ana Gómez", "CLIENTE_FINAL");
    camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
  }

  private static Map<String, Object> material(
      long productoId, String cantidad, List<String> seriales, String precio) {
    Map<String, Object> linea = material(productoId, cantidad, seriales);
    linea.put("precioUnitario", precio);
    return linea;
  }

  private static Map<String, Object> material(
      long productoId, String cantidad, List<String> seriales) {
    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", productoId);
    linea.put("cantidad", cantidad);
    linea.put("seriales", seriales);
    return linea;
  }

  private Map<String, Object> instalacion(List<Map<String, Object>> lineas, String manoDeObra) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("clienteId", cliente);
    datos.put("descripcion", "Instalación de 2 cámaras en la entrada");
    datos.put("tecnicos", List.of(1, 2));
    datos.put("moneda", "USD");
    datos.put("lineas", new ArrayList<>(lineas));
    datos.put("manoDeObra", manoDeObra);
    return datos;
  }

  private JsonNode registrar(Map<String, Object> datos) throws Exception {
    return leer(enviar(post("/api/v1/instalaciones"), datos).andExpect(status().isCreated()));
  }

  private ResultActions subirFoto(long id, String grupo, MockMultipartFile archivo)
      throws Exception {
    return mvc.perform(
        multipart("/api/v1/instalaciones/{id}/fotos", id)
            .file(archivo)
            .param("grupo", grupo)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  private static byte[] jpeg() throws Exception {
    BufferedImage imagen = new BufferedImage(30, 20, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen, "jpg", salida);
    return salida.toByteArray();
  }

  @Test
  void cp15_instalacionCon60mDeCableHabiendo50_noSeGuarda() throws Exception {
    comprar(proveedor, "USD", linea(cable, "50", "0.5"));

    enviar(
            post("/api/v1/instalaciones/vista-previa"),
            instalacion(List.of(material(cable, "60", null)), "40"))
        .andExpect(jsonPath("$.puedeGuardar").value(false))
        .andExpect(jsonPath("$.lineas[0].avisoStock").value("Stock insuficiente · quedan 50 m"));
    enviar(post("/api/v1/instalaciones"), instalacion(List.of(material(cable, "60", null)), "40"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

    assertThat(stock(cable)).isEqualByComparingTo("50");
    assertThat(jdbc.queryForObject("select count(*) from instalacion", Long.class)).isZero();
  }

  @Test
  void cp14_serialInstalado_noEstaDisponibleParaVender() throws Exception {
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("S1", "S2")));
    registrar(instalacion(List.of(material(camara, null, List.of("S1"))), "40"));

    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", camara);
    linea.put("seriales", List.of("S1"));
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", cliente);
    venta.put("moneda", "USD");
    venta.put("lineas", List.of(linea));
    enviar(post("/api/v1/ventas"), venta)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("SERIAL_NO_DISPONIBLE"));
    sinCuerpo(
            get("/api/v1/inventario/productos/{id}/seriales", camara).param("estado", "EN_BODEGA"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].numero").value("S2"));
  }

  @Test
  void registrar_guardaCobroConManoDeObraGarantiasYSeriales() throws Exception {
    comprar(proveedor, "USD", linea(camara, "3", "30", List.of("S1", "S2", "S3")));
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    Map<String, Object> datos =
        instalacion(
            List.of(
                material(camara, null, List.of("S1", "S2"), "25"),
                material(cable, "20", null, "2.5")),
            "50");
    datos.put("descuentoTipo", "VALOR");
    datos.put("descuentoValor", "10");
    datos.put("garantiaManoObraMeses", 2);
    datos.put("monedasComprobante", List.of("COP"));

    enviar(post("/api/v1/instalaciones/vista-previa"), datos)
        .andExpect(jsonPath("$.direccion").value("Calle 10 # 5-20"))
        .andExpect(jsonPath("$.garantias.venceManoObra").value("2026-12-01"))
        .andExpect(jsonPath("$.garantias.venceEquipos").value("2027-01-01"))
        .andExpect(jsonPath("$.resumen.total.usd.monto").value("140.0000"));
    JsonNode instalacion = registrar(datos);

    assertThat(instalacion.at("/consecutivo").asText()).isEqualTo("I-0001");
    assertThat(instalacion.at("/tecnicos/0/nombre").asText()).isEqualTo("Jose");
    assertThat(instalacion.at("/tecnicos/1/nombre").asText()).isEqualTo("Victor");
    assertThat(instalacion.at("/resumen/material/usd/monto").asText()).isEqualTo("100.0000");
    assertThat(instalacion.at("/resumen/manoDeObra/usd/monto").asText()).isEqualTo("50.0000");
    assertThat(instalacion.at("/total/monto").asText()).isEqualTo("140.0000");
    assertThat(instalacion.at("/resumen/costo/usd/monto").asText()).isEqualTo("70.0000");
    assertThat(instalacion.at("/utilidad/monto").asText()).isEqualTo("70.0000");
    assertThat(instalacion.at("/garantias/manoObraMeses").asInt()).isEqualTo(2);
    assertThat(instalacion.at("/garantias/estadoManoObra").asText()).isEqualTo("VIGENTE");
    assertThat(instalacion.at("/garantias/condiciones").asText()).contains("descargas eléctricas");
    assertThat(instalacion.at("/lineas/0/seriales/0/vencimientoGarantia").asText())
        .isEqualTo("2027-01-01");
    assertThat(stock(camara)).isEqualByComparingTo("1");
    assertThat(stock(cable)).isEqualByComparingTo("80");
    assertThat(
            jdbc.queryForList(
                "select distinct tipo from movimiento_inventario where documento_tipo = 'INSTALACION'",
                String.class))
        .containsExactly("INSTALACION");
    assertThat(
            jdbc.queryForList(
                "select estado from serial where numero in ('S1', 'S2')", String.class))
        .containsOnly("INSTALADO");
  }

  @Test
  void soloManoDeObraConFechaAnterior_yValidaciones() throws Exception {
    registrarTasa("USD_COP", HOY.minusDays(3), "3950");
    Map<String, Object> datos = instalacion(List.of(), "200000");
    datos.put("moneda", "COP");
    datos.put("fecha", "2026-09-28");
    datos.put("direccion", "Finca La Esperanza");
    JsonNode instalacion = registrar(datos);
    assertThat(instalacion.at("/fecha").asText()).isEqualTo("2026-09-28");
    assertThat(instalacion.at("/direccion").asText()).isEqualTo("Finca La Esperanza");
    assertThat(instalacion.at("/tasas/trm").asText()).isEqualTo("3950.000000");
    assertThat(instalacion.at("/garantias/venceManoObra").asText()).isEqualTo("2026-12-28");
    assertThat(instalacion.at("/resumen/costo/cop/monto").asText()).isEqualTo("0.0000");

    Map<String, Object> vacia = instalacion(List.of(), "0");
    enviar(post("/api/v1/instalaciones"), vacia)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("INSTALACION_VACIA"));
    Map<String, Object> futura = instalacion(List.of(), "10");
    futura.put("fecha", "2026-10-02");
    enviar(post("/api/v1/instalaciones"), futura)
        .andExpect(jsonPath("$.codigo").value("INSTALACION_FECHA_FUTURA"));
    Map<String, Object> tecnico = instalacion(List.of(), "10");
    tecnico.put("tecnicos", List.of(999));
    enviar(post("/api/v1/instalaciones"), tecnico)
        .andExpect(jsonPath("$.codigo").value("TECNICO_NO_EXISTE"));
    Map<String, Object> garantia = instalacion(List.of(), "10");
    garantia.put("garantiaManoObraMeses", 4);
    enviar(post("/api/v1/instalaciones"), garantia)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACION"));
  }

  @Test
  void fotos_porGrupo_validadasYQuitables_ySeConservanAlAnular() throws Exception {
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    long id = registrar(instalacion(List.of(material(cable, "10", null)), "40")).get("id").asLong();
    MockMultipartFile foto = new MockMultipartFile("archivo", "f.jpg", "image/jpeg", jpeg());

    subirFoto(id, "ANTES", foto).andExpect(status().isOk());
    subirFoto(id, "ANTES", foto).andExpect(status().isOk());
    JsonNode detalle = leer(subirFoto(id, "DESPUES", foto).andExpect(status().isOk()));
    assertThat(detalle.at("/fotos/antes")).hasSize(2);
    assertThat(detalle.at("/fotos/durante")).hasSize(0);
    assertThat(detalle.at("/fotos/despues/0/url").asText()).contains("instalaciones");

    subirFoto(
            id,
            "DURANTE",
            new MockMultipartFile(
                "archivo",
                "falsa.jpg",
                "image/jpeg",
                "no es imagen".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ARCHIVO_TIPO_NO_PERMITIDO"));
    subirFoto(
            id,
            "DURANTE",
            new MockMultipartFile("archivo", "g.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]))
        .andExpect(status().isBadRequest());

    long fotoId = detalle.at("/fotos/antes/0/id").asLong();
    sinCuerpo(delete("/api/v1/instalaciones/{id}/fotos/{foto}", id, fotoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fotos.antes", hasSize(1)));

    enviar(post("/api/v1/instalaciones/{id}/anular", id), Map.of("motivo", "Cliente canceló"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ANULADA"))
        .andExpect(jsonPath("$.garantias.estadoManoObra").value(nullValue()))
        .andExpect(jsonPath("$.fotos.antes", hasSize(1)));
    assertThat(stock(cable)).isEqualByComparingTo("100");
    subirFoto(id, "DESPUES", foto).andExpect(status().isOk());
  }

  @Test
  void maximo30FotosPorGrupo() throws Exception {
    long id = registrar(instalacion(List.of(), "40")).get("id").asLong();
    for (int i = 0; i < 30; i++) {
      jdbc.update(
          "insert into foto_instalacion (instalacion_id, grupo, clave) values (?, 'ANTES', ?)",
          id,
          "instalaciones/" + id + "/antes-" + i + ".jpg");
    }
    subirFoto(id, "ANTES", new MockMultipartFile("archivo", "f.jpg", "image/jpeg", jpeg()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("FOTOS_MAXIMAS"));
  }

  @Test
  void anular_devuelveMaterialYSeriales_unaSolaVez() throws Exception {
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("S1", "S2")));
    long id =
        registrar(instalacion(List.of(material(camara, null, List.of("S1", "S2"))), "40"))
            .get("id")
            .asLong();

    enviar(post("/api/v1/instalaciones/{id}/anular", id), Map.of("motivo", "Error"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anulacion.usuario").value("Jose"));
    enviar(post("/api/v1/instalaciones/{id}/anular", id), Map.of("motivo", "Error"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("INSTALACION_YA_ANULADA"));

    assertThat(stock(camara)).isEqualByComparingTo("2");
    assertThat(jdbc.queryForList("select distinct estado from serial", String.class))
        .containsExactly("EN_BODEGA");
    Map<String, Object> kardex =
        jdbc.queryForMap(
            "select sum(entrada) - sum(salida) as neto from movimiento_inventario"
                + " where producto_id = ?",
            camara);
    assertThat((BigDecimal) kardex.get("neto")).isEqualByComparingTo(stock(camara));
  }

  @Test
  void listadoEdicionEIdempotencia() throws Exception {
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    long otro = crearCliente("Juan Instalador", "INSTALADOR");
    String cuerpo = cuerpo(instalacion(List.of(material(cable, "10", null)), "40"));
    JsonNode primera = null;
    for (int i = 0; i < 2; i++) {
      primera =
          leer(
              mvc.perform(
                      post("/api/v1/instalaciones")
                          .header(HttpHeaders.AUTHORIZATION, bearer(token))
                          .header("Idempotency-Key", "inst-1")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(cuerpo))
                  .andExpect(status().isCreated())
                  .andExpect(jsonPath("$.consecutivo").value("I-0001")));
    }
    assertThat(stock(cable)).isEqualByComparingTo("90");
    Map<String, Object> soloVictor = instalacion(List.of(), "100");
    soloVictor.put("clienteId", otro);
    soloVictor.put("tecnicos", List.of(2));
    registrar(soloVictor);

    sinCuerpo(get("/api/v1/instalaciones"))
        .andExpect(jsonPath("$.instalaciones.contenido", hasSize(2)))
        .andExpect(jsonPath("$.instalaciones.contenido[1].tecnicos").value("Jose, Victor"))
        .andExpect(jsonPath("$.instalaciones.contenido[0].estadoGarantia").value("VIGENTE"))
        .andExpect(jsonPath("$.totalesPorMoneda[0].manoDeObra.monto").value("140.0000"))
        .andExpect(jsonPath("$.totalUsd.monto").value("540.0000"));
    sinCuerpo(get("/api/v1/instalaciones").param("tecnicoId", "1"))
        .andExpect(jsonPath("$.instalaciones.contenido", hasSize(1)));
    sinCuerpo(get("/api/v1/instalaciones").param("clienteId", String.valueOf(otro)))
        .andExpect(jsonPath("$.instalaciones.contenido", hasSize(1)));
    sinCuerpo(get("/api/v1/instalaciones").param("estadoGarantia", "POR_VENCER"))
        .andExpect(jsonPath("$.instalaciones.contenido", hasSize(0)));

    long id = primera.get("id").asLong();
    Map<String, Object> edicion = new HashMap<>();
    edicion.put("direccion", "Carrera 5 # 10-20");
    edicion.put("tecnicos", List.of(2));
    edicion.put("condicionesGarantia", "No cubre humedad.");
    edicion.put("version", primera.get("version").asLong());
    enviar(put("/api/v1/instalaciones/{id}", id), edicion)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.direccion").value("Carrera 5 # 10-20"))
        .andExpect(jsonPath("$.descripcion").value("Instalación de 2 cámaras en la entrada"))
        .andExpect(jsonPath("$.tecnicos", hasSize(1)))
        .andExpect(jsonPath("$.garantias.condiciones").value("No cubre humedad."));
    enviar(put("/api/v1/instalaciones/{id}", id), edicion).andExpect(status().isConflict());
    sinCuerpo(get("/api/v1/instalaciones/999999")).andExpect(status().isNotFound());
  }

  @Test
  void ventaEInstalacionSimultaneasDeLaUltimaUnidad_soloUnaSeGuarda() throws Exception {
    comprar(proveedor, "USD", linea(cable, "10", "0.5"));
    Map<String, Object> lineaVenta = new HashMap<>();
    lineaVenta.put("productoId", cable);
    lineaVenta.put("cantidad", "10");
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", cliente);
    venta.put("moneda", "USD");
    venta.put("lineas", List.of(lineaVenta));
    String cuerpoVenta = cuerpo(venta);
    String cuerpoInstalacion = cuerpo(instalacion(List.of(material(cable, "10", null)), "40"));
    CountDownLatch salida = new CountDownLatch(1);
    Callable<Integer> vender = () -> enviarEnParalelo(salida, "/api/v1/ventas", cuerpoVenta);
    Callable<Integer> instalar =
        () -> enviarEnParalelo(salida, "/api/v1/instalaciones", cuerpoInstalacion);
    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> resultados = List.of(hilos.submit(vender), hilos.submit(instalar));
      salida.countDown();
      List<Integer> estados = new ArrayList<>();
      for (Future<Integer> resultado : resultados) {
        estados.add(resultado.get());
      }
      assertThat(estados).containsExactlyInAnyOrder(201, 422);
    } finally {
      hilos.shutdownNow();
    }
    assertThat(stock(cable)).isEqualByComparingTo("0");
  }

  private int enviarEnParalelo(CountDownLatch salida, String ruta, String cuerpo) throws Exception {
    salida.await();
    return mvc.perform(
            post(ruta)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void comprobanteConManoDeObraSerialesYGarantias_yEnlacePublico() throws Exception {
    comprar(proveedor, "USD", linea(camara, "1", "20", List.of("HK-9")));
    long id =
        registrar(instalacion(List.of(material(camara, null, List.of("HK-9"))), "50"))
            .get("id")
            .asLong();

    byte[] pdf =
        sinCuerpo(get("/api/v1/instalaciones/{id}/comprobante", id))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String texto = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
    assertThat(texto)
        .contains("COMPROBANTE DE INSTALACIÓN")
        .contains("I-0001")
        .contains("Ana Gómez")
        .contains("Serial HK-9")
        .contains("Mano de obra · instalación y configuración")
        .contains("Garantía de mano de obra: 3 meses, hasta el 01/01/2027")
        .contains("Garantía de los equipos con serial: hasta el 01/01/2027")
        .contains("Condiciones de la garantía")
        .contains("Trabajo realizado: Instalación de 2 cámaras en la entrada")
        .contains("Técnicos: Jose, Victor");

    JsonNode enlace =
        leer(
            sinCuerpo(post("/api/v1/instalaciones/{id}/enlace", id))
                .andExpect(jsonPath("$.mensaje", containsString("instalación I-0001")))
                .andExpect(jsonPath("$.whatsappUrl", startsWith("https://wa.me/573001234567"))));
    mvc.perform(get(URI.create(enlace.get("url").asText()).getPath())).andExpect(status().isOk());
  }

  @Test
  void elClienteSumaSusInstalacionesYLosTecnicosSonLosUsuariosActivos() throws Exception {
    registrar(instalacion(List.of(), "40"));

    sinCuerpo(get("/api/v1/clientes/{id}", cliente))
        .andExpect(jsonPath("$.cantidadMovimientos").value(1));
    sinCuerpo(get("/api/v1/clientes/{id}/historial", cliente))
        .andExpect(jsonPath("$.instalaciones").value(1))
        .andExpect(jsonPath("$.movimientos[0].tipo").value("INSTALACION"))
        .andExpect(
            jsonPath("$.movimientos[0].descripcion")
                .value("Instalación de 2 cámaras en la entrada"));
    sinCuerpo(get("/api/v1/usuarios/tecnicos"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].nombre").value("Jose"));
  }
}
