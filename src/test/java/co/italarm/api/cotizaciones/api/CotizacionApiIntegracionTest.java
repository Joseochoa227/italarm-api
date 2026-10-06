package co.italarm.api.cotizaciones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.cotizaciones.aplicacion.ServicioCotizaciones;
import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;

class CotizacionApiIntegracionTest extends PruebaInventario {

  @Autowired private ServicioCotizaciones servicio;

  private long proveedor;
  private long cliente;
  private long otroCliente;
  private long camara;
  private long cable;

  @BeforeEach
  void preparar() throws Exception {
    proveedor = crearProveedor("Distribuidora Andina");
    cliente = crearCliente("Ana Gómez", "CLIENTE_FINAL");
    otroCliente = crearCliente("Luis Pérez", "INSTALADOR");
    camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
  }

  private static Map<String, Object> item(long productoId, String cantidad, String precio) {
    Map<String, Object> linea = new HashMap<>();
    linea.put("productoId", productoId);
    linea.put("cantidad", cantidad);
    linea.put("precioUnitario", precio);
    return linea;
  }

  private Map<String, Object> cotizacion(
      String tipo, long clienteId, List<Map<String, Object>> lineas, String manoDeObra) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("tipo", tipo);
    datos.put("clienteId", clienteId);
    datos.put("moneda", "USD");
    datos.put("lineas", new ArrayList<>(lineas));
    datos.put("manoDeObra", manoDeObra);
    if ("INSTALACION".equals(tipo)) {
      datos.put("descripcion", "Instalar 4 cámaras en la bodega");
    }
    return datos;
  }

  private long crear(Map<String, Object> datos) throws Exception {
    return leer(enviar(post("/api/v1/cotizaciones"), datos).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private long instalacionAprobada(List<Map<String, Object>> lineas) throws Exception {
    long id = crear(cotizacion("INSTALACION", cliente, lineas, "100"));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/aprobar", id)).andExpect(status().isOk());
    return id;
  }

  private void comprarCamaras(int cantidad) throws Exception {
    List<String> seriales = new ArrayList<>();
    for (int i = 1; i <= cantidad; i++) {
      seriales.add("SN-" + i);
    }
    comprar(proveedor, "USD", linea(camara, String.valueOf(cantidad), "20", seriales));
  }

  private String estado(long id) {
    return jdbc.queryForObject("select estado from cotizacion where id = ?", String.class, id);
  }

  @Test
  void registrar_quedaEnBorradorConVencimientoYUtilidadEstimada() throws Exception {
    comprarCamaras(2);
    comprar(proveedor, "USD", linea(cable, "200", "0.5"));
    Map<String, Object> datos =
        cotizacion(
            "INSTALACION",
            cliente,
            List.of(item(camara, "4", "45"), item(cable, "120", null)),
            "100");
    datos.put("descuentoTipo", "VALOR");
    datos.put("descuentoValor", "10");
    datos.put("monedasComprobante", List.of("COP"));

    long id = crear(datos);

    sinCuerpo(get("/api/v1/cotizaciones/{id}", id))
        .andExpect(jsonPath("$.consecutivo").value("COT-0001"))
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.tipo").value("INSTALACION"))
        .andExpect(jsonPath("$.fecha").value("2026-10-01"))
        .andExpect(jsonPath("$.validezDias").value(15))
        .andExpect(jsonPath("$.vence").value("2026-10-16"))
        .andExpect(jsonPath("$.diasParaVencer").value(15))
        .andExpect(jsonPath("$.porVencer").value(false))
        .andExpect(jsonPath("$.lineas", hasSize(2)))
        .andExpect(jsonPath("$.lineas[1].precioUnitario.monto").value("40.0000"))
        .andExpect(jsonPath("$.lineas[0].costoUnitarioUsd.monto").value("20.0000"))
        .andExpect(jsonPath("$.resumen.material.usd.monto").value("4980.0000"))
        .andExpect(jsonPath("$.total.monto").value("5070.0000"))
        .andExpect(jsonPath("$.utilidad.monto").value("4930.0000"))
        .andExpect(jsonPath("$.monedasComprobante[0]").value("COP"))
        .andExpect(jsonPath("$.registradaPor").value("Jose"));
    assertThat(stock(camara)).isEqualByComparingTo("2");
  }

  @Test
  void cp09_costoConTasaDeHoyYDeLaUltimaCompra() throws Exception {
    comprar(proveedor, "USD", linea(cable, "10", "20"));
    comprar(proveedor, "COP", linea(cable, "10", "76000"));
    assertThat(costo(cable)).isEqualByComparingTo("19.50");
    registrarTasa("USD_COP", HOY, "4200");
    Map<String, Object> datos = cotizacion("VENTA", cliente, List.of(item(cable, "1", null)), null);
    datos.put("moneda", "COP");

    enviar(post("/api/v1/cotizaciones/vista-previa"), datos)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vence").value("2026-10-16"))
        .andExpect(jsonPath("$.lineas[0].costoUnitarioHoy.cop.monto").value("81900.000000"))
        .andExpect(
            jsonPath("$.lineas[0].costoUnitarioUltimaCompra.cop.monto").value("78000.000000"))
        .andExpect(jsonPath("$.lineas[0].ultimaCompra.consecutivo").value("C-0002"))
        .andExpect(jsonPath("$.lineas[0].precioSugerido.monto").value(168000));
  }

  @Test
  void cp26_cotizacionPor4De5Camaras_noDescuentaStock() throws Exception {
    comprarCamaras(5);
    long id = crear(cotizacion("VENTA", cliente, List.of(item(camara, "4", "40")), null));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/enviar", id))
        .andExpect(jsonPath("$.estado").value("EN_EVALUACION"));

    assertThat(stock(camara)).isEqualByComparingTo("5");
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", otroCliente);
    venta.put("moneda", "USD");
    venta.put(
        "lineas",
        List.of(
            Map.of(
                "productoId",
                camara,
                "seriales",
                List.of("SN-1", "SN-2", "SN-3", "SN-4", "SN-5"))));
    enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated());
    assertThat(stock(camara)).isEqualByComparingTo("0");
  }

  @Test
  void cp21_enviadaCon15DiasYPasan16_quedaVencida() throws Exception {
    long enviada = crear(cotizacion("VENTA", cliente, List.of(item(cable, "10", "1")), null));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/enviar", enviada));
    long aprobada = crear(cotizacion("VENTA", cliente, List.of(item(cable, "10", "1")), null));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/aprobar", aprobada));

    reloj.fijar(Instant.parse("2026-10-16T15:00:00Z"));
    assertThat(servicio.vencer()).isZero();
    assertThat(estado(enviada)).isEqualTo("EN_EVALUACION");

    reloj.fijar(Instant.parse("2026-10-17T15:00:00Z"));
    assertThat(servicio.vencer()).isEqualTo(1);
    sinCuerpo(get("/api/v1/cotizaciones/{id}", enviada))
        .andExpect(jsonPath("$.estado").value("VENCIDA"))
        .andExpect(jsonPath("$.vencidaEl").value("2026-10-17"))
        .andExpect(jsonPath("$.diasParaVencer").value(nullValue()));
    assertThat(estado(aprobada)).isEqualTo("APROBADA");
  }

  @Test
  void porVencer_resaltaYFiltraLasEnEvaluacionConTresDiasOMenos() throws Exception {
    long pronto = crear(cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), null));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/enviar", pronto));
    Map<String, Object> larga = cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), null);
    larga.put("validezDias", 30);
    long tarde = crear(larga);
    sinCuerpo(post("/api/v1/cotizaciones/{id}/enviar", tarde));
    crear(cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), null));

    reloj.fijar(Instant.parse("2026-10-13T15:00:00Z"));
    sinCuerpo(get("/api/v1/cotizaciones?porVencer=true"))
        .andExpect(jsonPath("$.contenido", hasSize(1)))
        .andExpect(jsonPath("$.contenido[0].id").value(pronto))
        .andExpect(jsonPath("$.contenido[0].diasParaVencer").value(3))
        .andExpect(jsonPath("$.contenido[0].porVencer").value(true));
    sinCuerpo(get("/api/v1/cotizaciones?estado=BORRADOR"))
        .andExpect(jsonPath("$.contenido", hasSize(1)))
        .andExpect(jsonPath("$.contenido[0].consecutivo").value("COT-0003"));
    sinCuerpo(get("/api/v1/cotizaciones?clienteId={c}", cliente))
        .andExpect(jsonPath("$.totalElementos").value(3))
        .andExpect(jsonPath("$.contenido[0].consecutivo").value("COT-0003"));
  }

  @Test
  void cp22_convertirCotizacionAprobadaEnInstalacion_quedaConvertidaYEnlazada() throws Exception {
    comprarCamaras(6);
    comprar(proveedor, "USD", linea(cable, "200", "0.5"));
    long id = instalacionAprobada(List.of(item(camara, "4", "45"), item(cable, "120", "1.5")));

    JsonNode conversion =
        leer(
            sinCuerpo(get("/api/v1/cotizaciones/{id}/conversion", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("INSTALACION"))
                .andExpect(jsonPath("$.clienteId").value(cliente))
                .andExpect(jsonPath("$.direccion").value("Calle 10 # 5-20"))
                .andExpect(jsonPath("$.moneda").value("USD"))
                .andExpect(jsonPath("$.manoDeObra").value("100.0000"))
                .andExpect(jsonPath("$.descripcion").value("Instalar 4 cámaras en la bodega"))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(4))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value("45.0000"))
                .andExpect(jsonPath("$.lineas[0].controlaSerial").value(true))
                .andExpect(jsonPath("$.lineas[1].cantidad").value(120))
                .andExpect(jsonPath("$.lineas[1].precioUnitario").value("1.5000"))
                .andExpect(jsonPath("$.avisos", hasSize(0)))
                .andExpect(jsonPath("$.puedeGuardar").value(true)));

    Map<String, Object> instalacion = new HashMap<>();
    instalacion.put("cotizacionId", id);
    instalacion.put("clienteId", conversion.get("clienteId").asLong());
    instalacion.put("descripcion", conversion.get("descripcion").asText());
    instalacion.put("moneda", conversion.get("moneda").asText());
    instalacion.put("manoDeObra", conversion.get("manoDeObra").asText());
    instalacion.put("tecnicos", List.of(1));
    instalacion.put(
        "lineas",
        List.of(
            Map.of(
                "productoId", camara,
                "seriales", List.of("SN-1", "SN-2", "SN-3", "SN-4"),
                "precioUnitario", conversion.at("/lineas/0/precioUnitario").asText()),
            Map.of(
                "productoId",
                cable,
                "cantidad",
                "120",
                "precioUnitario",
                conversion.at("/lineas/1/precioUnitario").asText())));
    JsonNode guardada =
        leer(
            enviar(post("/api/v1/instalaciones"), instalacion)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cotizacion.id").value(id))
                .andExpect(jsonPath("$.cotizacion.consecutivo").value("COT-0001"))
                .andExpect(jsonPath("$.total.monto").value("460.0000")));

    sinCuerpo(get("/api/v1/cotizaciones/{id}", id))
        .andExpect(jsonPath("$.estado").value("CONVERTIDA"))
        .andExpect(jsonPath("$.documentoGenerado.tipo").value("INSTALACION"))
        .andExpect(jsonPath("$.documentoGenerado.id").value(guardada.get("id").asLong()))
        .andExpect(jsonPath("$.documentoGenerado.consecutivo").value("I-0001"));
    sinCuerpo(get("/api/v1/cotizaciones"))
        .andExpect(jsonPath("$.contenido[0].documentoGenerado").value("I-0001"));
    assertThat(stock(camara)).isEqualByComparingTo("2");

    sinCuerpo(get("/api/v1/cotizaciones/{id}/conversion", id))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("COTIZACION_NO_CONVERTIBLE"));
  }

  @Test
  void cp23_convertir4CamarasHabiendo3_noSeGuarda() throws Exception {
    comprarCamaras(3);
    long id = instalacionAprobada(List.of(item(camara, "4", "45")));

    sinCuerpo(get("/api/v1/cotizaciones/{id}/conversion", id))
        .andExpect(jsonPath("$.puedeGuardar").value(false))
        .andExpect(jsonPath("$.avisos[0].tipo").value("STOCK"))
        .andExpect(jsonPath("$.avisos[0].mensaje", containsString("quedan 3")));

    Map<String, Object> instalacion = new HashMap<>();
    instalacion.put("cotizacionId", id);
    instalacion.put("clienteId", cliente);
    instalacion.put("descripcion", "Instalar 4 cámaras");
    instalacion.put("moneda", "USD");
    instalacion.put("manoDeObra", "100");
    instalacion.put("tecnicos", List.of(1));
    instalacion.put("lineas", List.of(Map.of("productoId", camara, "cantidad", "4")));
    enviar(post("/api/v1/instalaciones"), instalacion)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

    assertThat(estado(id)).isEqualTo("APROBADA");
    assertThat(jdbc.queryForObject("select count(*) from instalacion", Integer.class)).isZero();
    assertThat(stock(camara)).isEqualByComparingTo("3");
  }

  private Map<String, Object> ventaDe(long cotizacionId, long clienteId) {
    Map<String, Object> venta = new HashMap<>();
    venta.put("cotizacionId", cotizacionId);
    venta.put("clienteId", clienteId);
    venta.put("moneda", "USD");
    venta.put("lineas", List.of(item(cable, "10", "2")));
    return venta;
  }

  @Test
  void cp24_anularVentaQueVieneDeCotizacion_cotizacionVuelveAAprobada() throws Exception {
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    long id = crear(cotizacion("VENTA", cliente, List.of(item(cable, "10", "2")), null));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/aprobar", id));
    long venta =
        leer(enviar(post("/api/v1/ventas"), ventaDe(id, cliente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cotizacion.consecutivo").value("COT-0001")))
            .get("id")
            .asLong();
    assertThat(estado(id)).isEqualTo("CONVERTIDA");

    enviar(post("/api/v1/ventas/{id}/anular", venta), Map.of("motivo", "El cliente desistió"))
        .andExpect(status().isOk());

    sinCuerpo(get("/api/v1/cotizaciones/{id}", id))
        .andExpect(jsonPath("$.estado").value("APROBADA"))
        .andExpect(jsonPath("$.documentoGenerado").value(nullValue()));
    enviar(post("/api/v1/ventas"), ventaDe(id, cliente)).andExpect(status().isCreated());
    sinCuerpo(get("/api/v1/cotizaciones/{id}", id))
        .andExpect(jsonPath("$.estado").value("CONVERTIDA"))
        .andExpect(jsonPath("$.documentoGenerado.consecutivo").value("V-0002"));
  }

  @Test
  void anularInstalacionQueVieneDeCotizacion_cotizacionVuelveAAprobada() throws Exception {
    long id = crear(cotizacion("INSTALACION", cliente, List.of(), "80"));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/aprobar", id));
    Map<String, Object> instalacion = new HashMap<>();
    instalacion.put("cotizacionId", id);
    instalacion.put("clienteId", cliente);
    instalacion.put("descripcion", "Mantenimiento");
    instalacion.put("moneda", "USD");
    instalacion.put("manoDeObra", "80");
    instalacion.put("tecnicos", List.of(2));
    long instalacionId =
        leer(enviar(post("/api/v1/instalaciones"), instalacion).andExpect(status().isCreated()))
            .get("id")
            .asLong();
    assertThat(estado(id)).isEqualTo("CONVERTIDA");

    enviar(
            post("/api/v1/instalaciones/{id}/anular", instalacionId),
            Map.of("motivo", "Se registró dos veces"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cotizacion.consecutivo").value("COT-0001"));
    assertThat(estado(id)).isEqualTo("APROBADA");
  }

  @Test
  void conversionNoPermitida_noEstaAprobadaEsDeOtroTipoOdeOtroCliente() throws Exception {
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    long borrador = crear(cotizacion("VENTA", cliente, List.of(item(cable, "10", "2")), null));
    enviar(post("/api/v1/ventas"), ventaDe(borrador, cliente))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("COTIZACION_NO_CONVERTIBLE"))
        .andExpect(jsonPath("$.detail", containsString("márcala como aprobada")));
    sinCuerpo(get("/api/v1/cotizaciones/{id}/conversion", borrador))
        .andExpect(status().isUnprocessableEntity());

    sinCuerpo(post("/api/v1/cotizaciones/{id}/aprobar", borrador));
    enviar(post("/api/v1/ventas"), ventaDe(borrador, otroCliente))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail", containsString("otro cliente")));

    long instalacion = instalacionAprobada(List.of(item(cable, "10", "2")));
    enviar(post("/api/v1/ventas"), ventaDe(instalacion, cliente))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail", containsString("es de instalación")));

    assertThat(jdbc.queryForObject("select count(*) from venta", Integer.class)).isZero();
    assertThat(stock(cable)).isEqualByComparingTo("100");
  }

  @Test
  void transicionesNoPermitidas() throws Exception {
    long id = crear(cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), null));
    enviar(
            post("/api/v1/cotizaciones/{id}/rechazar", id),
            Map.of("motivo", "PRECIO", "detalle", "Muy caro"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("RECHAZADA"))
        .andExpect(jsonPath("$.rechazo.motivo").value("PRECIO"))
        .andExpect(jsonPath("$.rechazo.detalle").value("Muy caro"));

    sinCuerpo(post("/api/v1/cotizaciones/{id}/aprobar", id))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"))
        .andExpect(jsonPath("$.detail").value("No se puede aprobar una cotización rechazada."));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/enviar", id))
        .andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/rechazar", id))
        .andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));
    Map<String, Object> edicion =
        cotizacion("VENTA", cliente, List.of(item(cable, "2", "1")), null);
    edicion.put("version", 1);
    enviar(put("/api/v1/cotizaciones/{id}", id), edicion)
        .andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));
  }

  @Test
  void editarEnEvaluacion_guardaNuevaVersionYConservaLaAnterior() throws Exception {
    long id = crear(cotizacion("VENTA", cliente, List.of(item(cable, "10", "2")), null));
    JsonNode enviada = leer(sinCuerpo(post("/api/v1/cotizaciones/{id}/enviar", id)));

    reloj.fijar(Instant.parse("2026-10-05T15:00:00Z"));
    registrarTasa("USD_COP", HOY.plusDays(4), "4100");
    Map<String, Object> edicion =
        cotizacion(
            "VENTA", cliente, List.of(item(cable, "20", "1.8"), item(camara, "1", "40")), null);
    edicion.put("version", enviada.get("version").asLong());
    enviar(put("/api/v1/cotizaciones/{id}", id), edicion)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consecutivo").value("COT-0001 v2"))
        .andExpect(jsonPath("$.numeroVersion").value(2))
        .andExpect(jsonPath("$.estado").value("EN_EVALUACION"))
        .andExpect(jsonPath("$.fecha").value("2026-10-05"))
        .andExpect(jsonPath("$.vence").value("2026-10-20"))
        .andExpect(jsonPath("$.tasas.trm").value("4100.000000"))
        .andExpect(jsonPath("$.total.monto").value("76.0000"))
        .andExpect(jsonPath("$.versionesAnteriores", hasSize(1)))
        .andExpect(jsonPath("$.versionesAnteriores[0].numeroVersion").value(1))
        .andExpect(jsonPath("$.versionesAnteriores[0].fecha").value("2026-10-01"))
        .andExpect(jsonPath("$.versionesAnteriores[0].total.monto").value("20.0000"))
        .andExpect(jsonPath("$.versionesAnteriores[0].lineas[0].cantidad").value(10))
        .andExpect(jsonPath("$.versionesAnteriores[0].reemplazadaPor").value("Jose"));

    edicion.put("version", enviada.get("version").asLong());
    enviar(put("/api/v1/cotizaciones/{id}", id), edicion)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("MODIFICADO_POR_OTRO_USUARIO"));
  }

  @Test
  void editarEnBorrador_noCreaVersion() throws Exception {
    long id = crear(cotizacion("VENTA", cliente, List.of(item(cable, "10", "2")), null));
    Map<String, Object> edicion =
        cotizacion("VENTA", cliente, List.of(item(cable, "12", "2")), null);
    edicion.put("version", 0);
    enviar(put("/api/v1/cotizaciones/{id}", id), edicion)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consecutivo").value("COT-0001"))
        .andExpect(jsonPath("$.lineas[0].cantidad").value(12))
        .andExpect(jsonPath("$.versionesAnteriores", hasSize(0)));
    edicion.remove("version");
    enviar(put("/api/v1/cotizaciones/{id}", id), edicion).andExpect(status().isBadRequest());
  }

  @Test
  void duplicar_creaUnaNuevaEnBorradorConLosPreciosCotizados() throws Exception {
    long id = crear(cotizacion("INSTALACION", cliente, List.of(item(cable, "30", "1.7")), "90"));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/rechazar", id));

    JsonNode copia =
        leer(
            mvc.perform(
                    post("/api/v1/cotizaciones/{id}/duplicar", id)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "dup-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consecutivo").value("COT-0002"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.lineas[0].precioUnitario.monto").value("1.7000"))
                .andExpect(jsonPath("$.resumen.manoDeObra.usd.monto").value("90.0000"))
                .andExpect(jsonPath("$.descripcion").value("Instalar 4 cámaras en la bodega")));
    mvc.perform(
            post("/api/v1/cotizaciones/{id}/duplicar", id)
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", "dup-1"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(copia.get("id").asLong()));
    assertThat(jdbc.queryForObject("select count(*) from cotizacion", Integer.class)).isEqualTo(2);
  }

  @Test
  void registrarConLaMismaClave_noDuplica() throws Exception {
    Map<String, Object> datos = cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), null);
    long primera =
        leer(mvc.perform(
                    post("/api/v1/cotizaciones")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "cot-1")
                        .contentType("application/json")
                        .content(cuerpo(datos)))
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    mvc.perform(
            post("/api/v1/cotizaciones")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", "cot-1")
                .contentType("application/json")
                .content(cuerpo(datos)))
        .andExpect(jsonPath("$.id").value(primera));
    assertThat(jdbc.queryForObject("select count(*) from cotizacion", Integer.class)).isEqualTo(1);
  }

  @Test
  void validaciones() throws Exception {
    enviar(
            post("/api/v1/cotizaciones"),
            cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("COTIZACION_MANO_OBRA_EN_VENTA"));
    enviar(post("/api/v1/cotizaciones"), cotizacion("VENTA", cliente, List.of(), null))
        .andExpect(jsonPath("$.codigo").value("COTIZACION_SIN_LINEAS"));
    Map<String, Object> sinDescripcion =
        cotizacion("INSTALACION", cliente, List.of(item(cable, "1", "1")), "10");
    sinDescripcion.remove("descripcion");
    enviar(post("/api/v1/cotizaciones"), sinDescripcion)
        .andExpect(jsonPath("$.codigo").value("COTIZACION_SIN_DESCRIPCION"));
    Map<String, Object> validez =
        cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), null);
    validez.put("validezDias", 10);
    enviar(post("/api/v1/cotizaciones"), validez)
        .andExpect(jsonPath("$.codigo").value("COTIZACION_VALIDEZ_INVALIDA"));
    enviar(
            post("/api/v1/cotizaciones"),
            cotizacion(
                "VENTA", cliente, List.of(item(cable, "1", "1"), item(cable, "2", "1")), null))
        .andExpect(jsonPath("$.codigo").value("COTIZACION_PRODUCTO_REPETIDO"));
    jdbc.update("update producto set activo = false where id = ?", cable);
    enviar(
            post("/api/v1/cotizaciones"),
            cotizacion("VENTA", cliente, List.of(item(cable, "1", "1")), null))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("PRODUCTO_INACTIVO"));
  }

  @Test
  void conversion_avisaSiCambiaronElPrecioYElCosto() throws Exception {
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    long id = crear(cotizacion("VENTA", cliente, List.of(item(cable, "10", "2")), null));
    sinCuerpo(post("/api/v1/cotizaciones/{id}/aprobar", id));
    jdbc.update("update producto set precio_cliente_final = 45 where id = ?", cable);
    comprar(proveedor, "USD", linea(cable, "100", "1.5"));

    sinCuerpo(get("/api/v1/cotizaciones/{id}/conversion", id))
        .andExpect(jsonPath("$.puedeGuardar").value(true))
        .andExpect(jsonPath("$.lineas[0].precioUnitario").value("2.0000"))
        .andExpect(jsonPath("$.avisos[0].tipo").value("PRECIO"))
        .andExpect(
            jsonPath("$.avisos[0].mensaje", containsString("Se mantiene el precio cotizado")))
        .andExpect(jsonPath("$.avisos[1].tipo").value("COSTO"));
  }

  @Test
  void pdfEnlaceYSeguimiento() throws Exception {
    long id = crear(cotizacion("INSTALACION", cliente, List.of(item(cable, "30", "1.5")), "90"));

    byte[] pdf =
        sinCuerpo(get("/api/v1/cotizaciones/{id}/comprobante", id))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String texto = texto(pdf);
    assertThat(texto)
        .contains("COT-0001")
        .contains("Válida hasta: 16/10/2026")
        .contains("Mano de obra · instalación y configuración")
        .contains("Trabajo a realizar: Instalar 4 cámaras en la bodega");
    assertThat(estado(id)).isEqualTo("BORRADOR");

    JsonNode enlace =
        leer(
            sinCuerpo(post("/api/v1/cotizaciones/{id}/enlace", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_EVALUACION"))
                .andExpect(jsonPath("$.mensaje", containsString("cotización COT-0001")))
                .andExpect(
                    jsonPath(
                        "$.whatsappUrl", startsWith("https://wa.me/573001234567?text=Hola+Ana"))));
    String ruta = URI.create(enlace.get("url").asText()).getPath();
    assertThat(
            texto(
                mvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsByteArray()))
        .contains("COT-0001");

    sinCuerpo(get("/api/v1/cotizaciones/{id}/seguimiento", id))
        .andExpect(
            jsonPath(
                "$.mensaje",
                containsString(
                    "para saber si pudiste revisar la cotización COT-0001 por US$ 135,00,"
                        + " válida hasta el 16/10/2026.")));

    sinCuerpo(post("/api/v1/cotizaciones/{id}/rechazar", id));
    assertThat(
            texto(
                sinCuerpo(get("/api/v1/cotizaciones/{id}/comprobante", id))
                    .andReturn()
                    .getResponse()
                    .getContentAsByteArray()))
        .contains("RECHAZADA");
  }

  private static String texto(byte[] pdf) throws Exception {
    PdfReader lector = new PdfReader(pdf);
    StringBuilder texto = new StringBuilder();
    for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
      texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina));
    }
    return texto.toString();
  }

  @Test
  void noExiste() throws Exception {
    sinCuerpo(get("/api/v1/cotizaciones/999")).andExpect(status().isNotFound());
  }
}
