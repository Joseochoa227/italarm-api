package co.italarm.api.reportes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exportaciones a Excel de los listados (RNF-06, P-62). */
class ExportacionesIntegracionTest extends PruebaInventario {

  /** Empresa, título, una línea de filtros, generado y una fila en blanco. */
  private static final int ENCABEZADO = 5;

  private long cliente;
  private long cable;
  private long camara;

  @BeforeEach
  void preparar() throws Exception {
    long proveedor = crearProveedor("Distribuidora Andina");
    cliente = crearCliente("Ana Gómez", "CLIENTE_FINAL");
    cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    camara = crearProducto("CAM-S", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(cable, "100", "0.5"));
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("S-1", "S-2")));

    Map<String, Object> venta = new HashMap<>();
    venta.put("clienteId", cliente);
    venta.put("moneda", "COP");
    venta.put(
        "lineas",
        List.of(
            Map.of("productoId", cable, "cantidad", "10", "precioUnitario", "8000"),
            Map.of("productoId", camara, "seriales", List.of("S-1"))));
    enviar(post("/api/v1/ventas"), venta).andExpect(status().isCreated());

    Map<String, Object> instalacion = new HashMap<>();
    instalacion.put("clienteId", cliente);
    instalacion.put("descripcion", "Mantenimiento");
    instalacion.put("tecnicos", List.of(1, 2));
    instalacion.put("moneda", "USD");
    instalacion.put("manoDeObra", "100");
    enviar(post("/api/v1/instalaciones"), instalacion).andExpect(status().isCreated());

    Map<String, Object> cotizacion = new HashMap<>();
    cotizacion.put("tipo", "VENTA");
    cotizacion.put("clienteId", cliente);
    cotizacion.put("moneda", "USD");
    cotizacion.put("lineas", List.of(Map.of("productoId", cable, "cantidad", "5")));
    enviar(post("/api/v1/cotizaciones"), cotizacion).andExpect(status().isCreated());
  }

  private Sheet hoja(String ruta, String archivo) throws Exception {
    byte[] contenido =
        sinCuerpo(get(ruta))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        "attachment; filename=\"" + archivo + "-2026-10-01.xlsx\""))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido));
    return libro.getSheetAt(0);
  }

  private static String celda(Sheet hoja, int fila, int columna) {
    return hoja.getRow(fila).getCell(columna).getStringCellValue();
  }

  @Test
  void ventasComprasInstalacionesYCotizaciones() throws Exception {
    Sheet ventas = hoja("/api/v1/ventas/excel", "ventas");
    assertThat(celda(ventas, 2, 0)).isEqualTo("Período: 01/10/2026 al 31/10/2026");
    assertThat(celda(ventas, ENCABEZADO, 0)).isEqualTo("Consecutivo");
    assertThat(celda(ventas, ENCABEZADO + 1, 0)).isEqualTo("V-0001");
    assertThat(ventas.getRow(ENCABEZADO + 1).getCell(4).getNumericCellValue())
        .isEqualTo(80000 + 160000);
    assertThat(ventas.getLastRowNum()).isEqualTo(ENCABEZADO + 1);

    Sheet compras = hoja("/api/v1/compras/excel?proveedorId=999", "compras");
    assertThat(compras.getLastRowNum()).isEqualTo(ENCABEZADO);
    assertThat(hoja("/api/v1/compras/excel", "compras").getLastRowNum()).isEqualTo(ENCABEZADO + 2);

    Sheet instalaciones = hoja("/api/v1/instalaciones/excel", "instalaciones");
    assertThat(celda(instalaciones, ENCABEZADO + 1, 4)).isEqualTo("Jose, Victor");

    Sheet cotizaciones = hoja("/api/v1/cotizaciones/excel", "cotizaciones");
    // Sin filtros no hay línea de filtros: el encabezado sube una fila.
    assertThat(celda(cotizaciones, 2, 0)).startsWith("Generado el");
    assertThat(celda(cotizaciones, ENCABEZADO - 1, 0)).isEqualTo("Consecutivo");
    assertThat(celda(cotizaciones, ENCABEZADO, 0)).isEqualTo("COT-0001");
  }

  @Test
  void inventarioKardexYGarantias() throws Exception {
    Sheet inventario = hoja("/api/v1/inventario/excel", "inventario");
    assertThat(celda(inventario, ENCABEZADO + 1, 0)).isEqualTo("CAM-S");
    assertThat(celda(inventario, inventario.getLastRowNum(), 0)).isEqualTo("Total");
    assertThat(inventario.getRow(inventario.getLastRowNum()).getCell(9).getNumericCellValue())
        .isEqualTo(20 + 45);

    Sheet kardex = hoja("/api/v1/inventario/productos/" + cable + "/kardex/excel", "kardex-UTP");
    assertThat(celda(kardex, ENCABEZADO + 1, 2)).isEqualTo("C-0001");
    assertThat(kardex.getRow(ENCABEZADO + 2).getCell(6).getNumericCellValue()).isEqualTo(90);

    Sheet garantias = hoja("/api/v1/garantias/excel", "garantias");
    // Sin filtros: encabezado en la fila 4, y la mano de obra de la instalación y el serial vendido.
    assertThat(garantias.getLastRowNum()).isEqualTo(ENCABEZADO - 1 + 2);
  }

  @Test
  void sinSesion_noExporta() throws Exception {
    mvc.perform(get("/api/v1/ventas/excel")).andExpect(status().isUnauthorized());
  }
}
