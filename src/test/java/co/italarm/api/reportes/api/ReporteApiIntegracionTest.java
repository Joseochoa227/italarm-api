package co.italarm.api.reportes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReporteApiIntegracionTest extends PruebaInventario {

  private long proveedor;
  private long cliente;
  private long camara;
  private long cable;
  private long ventaAnulada;

  /**
   * El 30 de septiembre: una compra de 100 m de cable a US$ 0,50 y una venta de US$ 20. El 1 de
   * octubre: venta de US$ 30, venta de $ 80.000 (US$ 20), una venta anulada, una instalación de
   * solo mano de obra por US$ 100 y una compra de 3 cámaras con serial.
   */
  @BeforeEach
  void preparar() throws Exception {
    proveedor = crearProveedor("Distribuidora Andina");
    cliente = crearCliente("Ana Gómez", "CLIENTE_FINAL");
    camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);

    reloj.fijar(Instant.parse("2026-09-30T15:00:00Z"));
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    vender("USD", cable, "10", "2");

    reloj.fijar(Instant.parse("2026-10-01T14:00:00Z"));
    comprar(proveedor, "USD", linea(camara, "3", "20", List.of("S-1", "S-2", "S-3")));
    paso();
    vender("USD", cable, "10", "3");
    paso();
    vender("COP", cable, "10", "8000");
    paso();
    ventaAnulada = vender("USD", cable, "10", "5");
    paso();
    enviar(post("/api/v1/ventas/{id}/anular", ventaAnulada), Map.of("motivo", "Error"))
        .andExpect(status().isOk());
    paso();
    Map<String, Object> instalacion = new HashMap<>();
    instalacion.put("clienteId", cliente);
    instalacion.put("descripcion", "Mantenimiento");
    instalacion.put("tecnicos", List.of(1));
    instalacion.put("moneda", "USD");
    instalacion.put("manoDeObra", "100");
    enviar(post("/api/v1/instalaciones"), instalacion).andExpect(status().isCreated());
    paso();
  }

  private void paso() {
    reloj.avanzar(Duration.ofMinutes(1));
  }

  private long vender(String moneda, long productoId, String cantidad, String precio)
      throws Exception {
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", cliente);
    venta.put("moneda", moneda);
    venta.put(
        "lineas",
        new ArrayList<>(
            List.of(
                Map.of("productoId", productoId, "cantidad", cantidad, "precioUnitario", precio))));
    return leer(enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  @Test
  void inicio_indicadoresDelMesSinAnuladasNiMesAnterior() throws Exception {
    jdbc.update("update producto set stock_minimo = 80 where id = ?", cable);

    sinCuerpo(get("/api/v1/inicio"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuario").value("Jose"))
        .andExpect(jsonPath("$.hoy").value("2026-10-01"))
        .andExpect(jsonPath("$.avisosTasas", hasSize(0)))
        .andExpect(jsonPath("$.indicadores.ventas.cantidad").value(2))
        .andExpect(jsonPath("$.indicadores.ventas.total.usd.monto").value("50.0000"))
        .andExpect(jsonPath("$.indicadores.ventas.total.cop.monto").value("200000.000000"))
        .andExpect(jsonPath("$.indicadores.ventas.porMoneda", hasSize(2)))
        .andExpect(jsonPath("$.indicadores.ventas.porMoneda[0].moneda").value("COP"))
        .andExpect(jsonPath("$.indicadores.ventas.porMoneda[0].monto").value("80000.0000"))
        .andExpect(jsonPath("$.indicadores.instalaciones.cantidad").value(1))
        .andExpect(jsonPath("$.indicadores.instalaciones.total.usd.monto").value("100.0000"))
        .andExpect(jsonPath("$.indicadores.utilidad.total.usd.monto").value("140.0000"))
        .andExpect(jsonPath("$.indicadores.utilidad.porcentaje").value("93.33"))
        .andExpect(jsonPath("$.indicadores.inventario.productos").value(2))
        .andExpect(jsonPath("$.indicadores.inventario.valor.usd.monto").value("95.0000"))
        .andExpect(jsonPath("$.stockBajo", hasSize(1)))
        .andExpect(jsonPath("$.stockBajo[0].stock").value(70))
        .andExpect(jsonPath("$.stockBajo[0].nivel").value("87.50"))
        .andExpect(jsonPath("$.totalStockBajo").value(1))
        .andExpect(jsonPath("$.ultimosMovimientos[0].tipo").value("INSTALACION"))
        .andExpect(jsonPath("$.ultimosMovimientos[0].consecutivo").value("I-0001"))
        .andExpect(jsonPath("$.ultimosMovimientos[0].usuario").value("Jose"))
        .andExpect(jsonPath("$.ultimosMovimientos[1].consecutivo").value("V-0004"))
        .andExpect(jsonPath("$.ultimosMovimientos[1].anulado").value(true))
        .andExpect(jsonPath("$.ultimosMovimientos[4].tipo").value("COMPRA"))
        .andExpect(jsonPath("$.ultimosMovimientos[4].detalle").value("Distribuidora Andina"))
        .andExpect(jsonPath("$.ultimosMovimientos[4].valor.monto").value("60.0000"));
  }

  @Test
  void inicio_avisaSiFaltaLaTasaDelBolivarYMuestraLosBloques() throws Exception {
    jdbc.update("delete from tasa_cambio where par = 'USD_VES' and fecha = ?", HOY);
    Map<String, Object> cotizacion = new HashMap<>();
    cotizacion.put("tipo", "VENTA");
    cotizacion.put("clienteId", cliente);
    cotizacion.put("moneda", "USD");
    cotizacion.put("lineas", List.of(Map.of("productoId", cable, "cantidad", "5")));
    long cotizacionId =
        leer(enviar(post("/api/v1/cotizaciones"), cotizacion).andExpect(status().isCreated()))
            .get("id")
            .asLong();
    sinCuerpo(post("/api/v1/cotizaciones/{id}/enviar", cotizacionId));

    sinCuerpo(get("/api/v1/inicio"))
        .andExpect(jsonPath("$.avisosTasas", hasSize(1)))
        .andExpect(jsonPath("$.avisosTasas[0].par").value("USD_VES"))
        .andExpect(jsonPath("$.cotizacionesEnEvaluacion[0].consecutivo").value("COT-0001"))
        .andExpect(jsonPath("$.cotizacionesEnEvaluacion[0].diasParaVencer").value(15))
        .andExpect(jsonPath("$.totalCotizacionesEnEvaluacion").value(1))
        .andExpect(jsonPath("$.totalGarantiasPorVencer").value(0));
  }

  @Test
  void reporteDelMes() throws Exception {
    sinCuerpo(get("/api/v1/reportes/periodo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("MES"))
        .andExpect(jsonPath("$.desde").value("2026-10-01"))
        .andExpect(jsonPath("$.anterior.desde").value("2026-09-01"))
        .andExpect(jsonPath("$.siguiente.desde").value("2026-11-01"))
        .andExpect(jsonPath("$.agrupacion").value("SEMANA"))
        .andExpect(jsonPath("$.serie", hasSize(5)))
        .andExpect(jsonPath("$.serie[0].etiqueta").value("01/10 al 04/10"))
        .andExpect(jsonPath("$.serie[0].ventas").value(2))
        .andExpect(jsonPath("$.serie[0].totalVentas.monto").value("50.0000"))
        .andExpect(jsonPath("$.serie[0].totalInstalaciones.monto").value("100.0000"))
        .andExpect(jsonPath("$.serie[1].ventas").value(0))
        .andExpect(jsonPath("$.masVendidos", hasSize(1)))
        .andExpect(jsonPath("$.masVendidos[0].codigo").value("UTP"))
        .andExpect(jsonPath("$.masVendidos[0].cantidad").value(20))
        .andExpect(jsonPath("$.masVendidos[0].unidad").value("m"))
        .andExpect(jsonPath("$.masVendidos[0].valor.monto").value("50.0000"))
        .andExpect(jsonPath("$.masInstalados", hasSize(0)))
        .andExpect(jsonPath("$.comprasPorProveedor[0].proveedor").value("Distribuidora Andina"))
        .andExpect(jsonPath("$.comprasPorProveedor[0].compras").value(1))
        .andExpect(jsonPath("$.totalCompras.monto").value("60.0000"))
        .andExpect(jsonPath("$.cotizaciones.total").value(0))
        .andExpect(jsonPath("$.indicadores.inventario.fecha").value("2026-10-01"));

    sinCuerpo(get("/api/v1/reportes/periodo?tipo=MES&fecha=2026-09-15"))
        .andExpect(jsonPath("$.indicadores.ventas.cantidad").value(1))
        .andExpect(jsonPath("$.indicadores.ventas.total.usd.monto").value("20.0000"))
        .andExpect(jsonPath("$.totalCompras.monto").value("50.0000"));
  }

  @Test
  void otrosPeriodos() throws Exception {
    sinCuerpo(get("/api/v1/reportes/periodo?tipo=SEMANA&fecha=2026-10-01"))
        .andExpect(jsonPath("$.desde").value("2026-09-28"))
        .andExpect(jsonPath("$.hasta").value("2026-10-04"))
        .andExpect(jsonPath("$.indicadores.ventas.cantidad").value(3));
    sinCuerpo(get("/api/v1/reportes/periodo?tipo=ANIO"))
        .andExpect(jsonPath("$.agrupacion").value("MES"))
        .andExpect(jsonPath("$.serie", hasSize(12)))
        .andExpect(jsonPath("$.serie[8].ventas").value(1))
        .andExpect(jsonPath("$.serie[9].ventas").value(2));
    sinCuerpo(get("/api/v1/reportes/periodo?tipo=RANGO&desde=2026-09-30&hasta=2026-10-01"))
        .andExpect(jsonPath("$.anterior").doesNotExist())
        .andExpect(jsonPath("$.indicadores.ventas.cantidad").value(3));
    sinCuerpo(get("/api/v1/reportes/periodo?desde=2026-10-05&hasta=2026-10-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
  }

  @Test
  void cotizacionesPorEstadoYPorcentajeDeAprobadas() throws Exception {
    for (String accion : List.of("aprobar", "aprobar", "rechazar", "enviar")) {
      Map<String, Object> cotizacion = new HashMap<>();
      cotizacion.put("tipo", "VENTA");
      cotizacion.put("clienteId", cliente);
      cotizacion.put("moneda", "USD");
      cotizacion.put("lineas", List.of(Map.of("productoId", cable, "cantidad", "1")));
      long id =
          leer(enviar(post("/api/v1/cotizaciones"), cotizacion).andExpect(status().isCreated()))
              .get("id")
              .asLong();
      sinCuerpo(post("/api/v1/cotizaciones/{id}/" + accion, id)).andExpect(status().isOk());
    }

    sinCuerpo(get("/api/v1/reportes/periodo"))
        .andExpect(jsonPath("$.cotizaciones.total").value(4))
        .andExpect(jsonPath("$.cotizaciones.porEstado[2].estado").value("APROBADA"))
        .andExpect(jsonPath("$.cotizaciones.porEstado[2].cantidad").value(2))
        .andExpect(jsonPath("$.cotizaciones.porcentajeAprobadas").value("66.67"));
  }

  @Test
  void utilidadPorDocumento() throws Exception {
    sinCuerpo(get("/api/v1/reportes/utilidad?ordenarPorUtilidad=true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentos.totalElementos").value(3))
        .andExpect(jsonPath("$.documentos.contenido[0].consecutivo").value("I-0001"))
        .andExpect(jsonPath("$.documentos.contenido[0].utilidadUsd.monto").value("100.0000"))
        .andExpect(jsonPath("$.documentos.contenido[1].consecutivo").value("V-0002"))
        .andExpect(jsonPath("$.documentos.contenido[1].costo.monto").value("5.0000"))
        .andExpect(jsonPath("$.totalUsd.monto").value("150.0000"))
        .andExpect(jsonPath("$.costoUsd.monto").value("10.0000"))
        .andExpect(jsonPath("$.utilidadUsd.monto").value("140.0000"))
        .andExpect(jsonPath("$.porcentajeUtilidad").value("93.33"));
    sinCuerpo(get("/api/v1/reportes/utilidad?tipo=VENTA"))
        .andExpect(jsonPath("$.documentos.totalElementos").value(2))
        .andExpect(jsonPath("$.documentos.contenido[0].consecutivo").value("V-0003"))
        .andExpect(jsonPath("$.documentos.contenido[0].total.moneda").value("COP"))
        .andExpect(jsonPath("$.totalUsd.monto").value("50.0000"));
  }

  @Test
  void seriales() throws Exception {
    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", cliente);
    venta.put("moneda", "USD");
    venta.put("lineas", List.of(Map.of("productoId", camara, "seriales", List.of("S-2"))));
    enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated());

    sinCuerpo(get("/api/v1/reportes/seriales"))
        .andExpect(jsonPath("$.porEstado[0].estado").value("EN_BODEGA"))
        .andExpect(jsonPath("$.porEstado[0].cantidad").value(2))
        .andExpect(jsonPath("$.porEstado[1].cantidad").value(1))
        .andExpect(jsonPath("$.seriales.totalElementos").value(3));
    sinCuerpo(get("/api/v1/reportes/seriales?estado=VENDIDO"))
        .andExpect(jsonPath("$.seriales.contenido", hasSize(1)))
        .andExpect(jsonPath("$.seriales.contenido[0].numero").value("S-2"))
        .andExpect(jsonPath("$.seriales.contenido[0].salida").value("V-0005"))
        .andExpect(jsonPath("$.seriales.contenido[0].vencimientoGarantia").value("2027-01-01"));
  }

  private Workbook excel(String ruta) throws Exception {
    byte[] contenido =
        sinCuerpo(get(ruta))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    return new XSSFWorkbook(new ByteArrayInputStream(contenido));
  }

  @Test
  void excelDelReporteDelPeriodoYDeUtilidad() throws Exception {
    try (Workbook libro = excel("/api/v1/reportes/periodo/excel")) {
      assertThat(libro.getNumberOfSheets()).isEqualTo(7);
      Sheet indicadores = libro.getSheet("Indicadores");
      assertThat(indicadores.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ITALARM");
      assertThat(indicadores.getRow(2).getCell(0).getStringCellValue())
          .isEqualTo("Período: 01/10/2026 al 31/10/2026");
      int encabezado = 6;
      assertThat(indicadores.getRow(encabezado).getCell(2).getStringCellValue()).isEqualTo("USD");
      assertThat(indicadores.getRow(encabezado + 1).getCell(0).getStringCellValue())
          .isEqualTo("Ventas de material");
      assertThat(indicadores.getRow(encabezado + 1).getCell(2).getCellType())
          .isEqualTo(CellType.NUMERIC);
      assertThat(indicadores.getRow(encabezado + 1).getCell(2).getNumericCellValue()).isEqualTo(50);
      assertThat(libro.getSheet("Más vendidos").getRow(6).getCell(0).getStringCellValue())
          .isEqualTo("UTP");
    }
    try (Workbook libro = excel("/api/v1/reportes/utilidad/excel")) {
      Sheet hoja = libro.getSheetAt(0);
      assertThat(hoja.getLastRowNum()).isEqualTo(5 + 3 + 1);
      assertThat(hoja.getRow(hoja.getLastRowNum()).getCell(0).getStringCellValue())
          .isEqualTo("Total");
      assertThat(hoja.getRow(hoja.getLastRowNum()).getCell(10).getNumericCellValue())
          .isEqualTo(140);
    }
    try (Workbook libro = excel("/api/v1/reportes/seriales/excel?estado=EN_BODEGA")) {
      assertThat(libro.getSheetAt(0).getLastRowNum()).isEqualTo(6 + 3);
    }
  }
}
