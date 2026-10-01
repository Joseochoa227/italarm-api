package co.italarm.api.cargainicial.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class CargaInicialApiIntegracionTest extends PruebaInventario {

  /** Libro de prueba: por hoja, sus filas de datos (la fila 1 es el encabezado). */
  private final Map<String, List<Object[]>> hojas = new LinkedHashMap<>();

  private void fila(String hoja, Object... valores) {
    hojas.computeIfAbsent(hoja, h -> new ArrayList<>()).add(valores);
  }

  private byte[] libro() throws Exception {
    try (Workbook libro = new XSSFWorkbook();
        ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
      for (Map.Entry<String, List<Object[]>> hoja : hojas.entrySet()) {
        Sheet sheet = libro.createSheet(hoja.getKey());
        sheet.createRow(0).createCell(0).setCellValue("Encabezado");
        int i = 1;
        for (Object[] valores : hoja.getValue()) {
          Row row = sheet.createRow(i++);
          for (int c = 0; c < valores.length; c++) {
            if (valores[c] instanceof Integer entero) {
              row.createCell(c).setCellValue(entero);
            } else if (valores[c] != null) {
              row.createCell(c).setCellValue(valores[c].toString());
            }
          }
        }
      }
      libro.write(salida);
      return salida.toByteArray();
    }
  }

  private ResultActions subir(String ruta, byte[] contenido) throws Exception {
    return mvc.perform(
        multipart(ruta)
            .file(new MockMultipartFile("archivo", "inventario.xlsx", null, contenido))
            .header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  private static List<String> seriales(String prefijo, int cantidad) {
    List<String> lista = new ArrayList<>();
    for (int i = 1; i <= cantidad; i++) {
      lista.add(prefijo + i);
    }
    return lista;
  }

  @Test
  void cp28_cargaInicialDeDiezCamaras_creaII001() throws Exception {
    fila(
        "Productos",
        "CAM-HK",
        "Cámara domo 2 MP",
        "Hikvision",
        null,
        "Cámaras",
        "und",
        "Sí",
        20,
        "25,5",
        "USD",
        5,
        null);
    fila("Inventario inicial", "cam-hk", 10, 20, String.join(", ", seriales("HK", 10)));

    subir("/api/v1/carga-inicial", libro())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.consecutivo").value("II-001"))
        .andExpect(jsonPath("$.productosCreados").value(1))
        .andExpect(jsonPath("$.productosConStock").value(1))
        .andExpect(jsonPath("$.valorUsd.monto").value("200.0000"))
        .andExpect(jsonPath("$.archivo").value("inventario.xlsx"))
        .andExpect(jsonPath("$.registradaPor").value("Jose"));

    long id = jdbc.queryForObject("select id from producto where codigo = 'CAM-HK'", Long.class);
    assertThat(stock(id)).isEqualByComparingTo("10");
    assertThat(costo(id)).isEqualByComparingTo("20");
    Map<String, Object> movimiento =
        jdbc.queryForMap(
            "select tipo, documento_consecutivo, entrada, saldo from movimiento_inventario"
                + " where producto_id = ?",
            id);
    assertThat(movimiento)
        .containsEntry("tipo", "INVENTARIO_INICIAL")
        .containsEntry("documento_consecutivo", "II-001");
    assertThat(
            jdbc.queryForList(
                "select distinct estado from serial where producto_id = ?", String.class, id))
        .containsExactly("EN_BODEGA");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from serial where producto_id = ?", Long.class, id))
        .isEqualTo(10);
    assertThat(
            jdbc.queryForObject(
                "select regla from historial_costo where producto_id = ?", String.class, id))
        .isEqualTo("INVENTARIO_INICIAL");

    sinCuerpo(get("/api/v1/carga-inicial"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].consecutivo").value("II-001"));
  }

  @Test
  void cp29_serialRepetidoEnLaFila8_noGuardaNada() throws Exception {
    for (int i = 1; i <= 7; i++) {
      fila(
          "Productos",
          "CAM-" + i,
          "Cámara " + i,
          null,
          null,
          "Cámaras",
          "und",
          "Sí",
          20,
          25,
          "USD",
          null,
          null);
    }
    for (int i = 1; i <= 6; i++) {
      fila("Inventario inicial", "CAM-" + i, 2, 20, "S" + i + "A, S" + i + "B");
    }
    fila("Inventario inicial", "CAM-7", 2, 20, "R1, r1");

    subir("/api/v1/carga-inicial/validar", libro())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valido").value(false))
        .andExpect(jsonPath("$.errores", hasSize(1)))
        .andExpect(jsonPath("$.errores[0].hoja").value("Inventario inicial"))
        .andExpect(jsonPath("$.errores[0].fila").value(8))
        .andExpect(jsonPath("$.errores[0].mensaje", containsString("R1")))
        .andExpect(jsonPath("$.resumen.productos").value(7));

    subir("/api/v1/carga-inicial", libro())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CARGA_INICIAL_CON_ERRORES"))
        .andExpect(jsonPath("$.errores[0].fila").value(8));

    assertThat(jdbc.queryForObject("select count(*) from producto", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from inventario_inicial", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from serial", Long.class)).isZero();
  }

  @Test
  void validar_informaLosErroresDeCadaHojaYFila() throws Exception {
    long existente = crearProducto("EXISTE", CATEGORIA_CAMARAS, UNIDAD, false);
    comprar(crearProveedor("Andina"), "USD", linea(existente, "1", "10"));
    fila("Productos", "EXISTE", "Repetido", null, null, "Cámaras", "und", "No", 1, 1, null);
    fila("Productos", "NUEVO", "Nuevo", null, null, "Inexistente", "kg", "Tal vez", "abc", 1);
    fila("Productos", "OK", "Correcto", null, null, "Cable", "m", "No", 1, 2, "COP", "2.5");
    fila("Inventario inicial", "EXISTE", 1, 10);
    fila("Inventario inicial", "OK", "10,5", "0.3");
    fila("Inventario inicial", "NO-HAY", 1, 1);
    fila("Inventario inicial", "OK", 1, 1);
    fila("Inventario inicial", "NUEVO", 1, 1);
    fila("Clientes", "Instalador", "Juan", "CC", "123", "3001234567");
    fila("Clientes", "Instalador", "Pedro", "CC", "123", "3001234568");
    fila("Clientes", "Otro", null, "XX", null, null);
    fila("Proveedores", "Andina", null, null, null, null, "EUR");

    subir("/api/v1/carga-inicial/validar", libro())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valido").value(false))
        .andExpect(jsonPath("$.resumen.productos").value(3))
        .andExpect(jsonPath("$.resumen.clientes").value(3))
        .andExpect(jsonPath("$.errores[0].hoja").value("Productos"))
        .andExpect(jsonPath("$.errores[0].fila").value(2))
        .andExpect(
            jsonPath("$.errores[0].mensaje").value("Ya existe un producto con el código EXISTE."))
        .andExpect(jsonPath("$.errores[1].fila").value(3))
        .andExpect(jsonPath("$.errores[1].mensaje", containsString("Controla serial")));

    String respuesta =
        subir("/api/v1/carga-inicial/validar", libro())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(respuesta)
        .contains("Precio instalador: \\\"abc\\\" no es un número válido.")
        .contains("Corrige primero el producto EXISTE en la hoja Productos.")
        .contains("El producto NO-HAY no existe ni está en la hoja Productos.")
        .contains("El producto OK está repetido en la hoja.")
        .contains("Corrige primero el producto NUEVO en la hoja Productos.")
        .contains("El documento 123 está repetido en el archivo.")
        .contains("Tipo: escribe Instalador o Cliente final.")
        .contains("Moneda habitual: escribe USD, COP o VES.");
    assertThat(respuesta).doesNotContain("\"fila\":4,\"mensaje\"" + ":\"La cantidad");
  }

  @Test
  void productoExistenteSinMovimientos_seCargaYConMovimientosNo() throws Exception {
    long sinMovimientos = crearProducto("SIN-MOV", CATEGORIA_CABLE, METRO, false);
    long conMovimientos = crearProducto("CON-MOV", CATEGORIA_CABLE, METRO, false);
    comprar(crearProveedor("Andina"), "USD", linea(conMovimientos, "1", "10"));

    fila("Inventario inicial", "CON-MOV", 5, 1);
    subir("/api/v1/carga-inicial/validar", libro())
        .andExpect(jsonPath("$.errores[0].mensaje", containsString("ya tiene movimientos")));

    hojas.clear();
    fila("Inventario inicial", "SIN-MOV", "305,5", "0.25");
    fila("Clientes", "Cliente final", "Ana Gómez", null, null, "3001112233");
    fila("Proveedores", "Importadora Caracas", null, null, null, "Caracas", "VES");
    subir("/api/v1/carga-inicial", libro())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.clientesCreados").value(1))
        .andExpect(jsonPath("$.proveedoresCreados").value(1));

    assertThat(stock(sinMovimientos)).isEqualByComparingTo("305.5");
    assertThat(costo(sinMovimientos)).isEqualByComparingTo("0.25");
    assertThat(
            jdbc.queryForObject(
                "select telefono from cliente where nombre = 'Ana Gómez'", String.class))
        .isEqualTo("+573001112233");
    assertThat(
            jdbc.queryForObject(
                "select moneda_habitual from proveedor where nombre = 'Importadora Caracas'",
                String.class))
        .isEqualTo("VES");
  }

  @Test
  void laMismaIdempotencyKey_creaUnaSolaCarga() throws Exception {
    fila("Productos", "UTP", "Cable UTP", null, null, "Cable", "m", "No", 1, 2);
    fila("Inventario inicial", "UTP", 100, "0.3");
    byte[] contenido = libro();
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              multipart("/api/v1/carga-inicial")
                  .file(new MockMultipartFile("archivo", "a.xlsx", null, contenido))
                  .header(HttpHeaders.AUTHORIZATION, bearer(token))
                  .header("Idempotency-Key", "carga-1"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.consecutivo").value("II-001"));
    }
    assertThat(jdbc.queryForObject("select count(*) from inventario_inicial", Long.class))
        .isEqualTo(1);
  }

  @Test
  void archivoQueNoEsExcel_seRechaza() throws Exception {
    subir("/api/v1/carga-inicial/validar", "nombre,cantidad".getBytes())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ARCHIVO_TIPO_NO_PERMITIDO"));
    subir("/api/v1/carga-inicial/validar", new byte[] {'P', 'K', 3, 4, 0, 0})
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ARCHIVO_TIPO_NO_PERMITIDO"));
    hojas.put("Otra hoja", List.of());
    subir("/api/v1/carga-inicial/validar", libro())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail", containsString("no tiene las hojas")));
  }

  @Test
  void plantilla_tieneLasHojasYSePuedeSubirVacia() throws Exception {
    byte[] plantilla =
        sinCuerpo(get("/api/v1/carga-inicial/plantilla"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".xlsx")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(plantilla))) {
      assertThat(libro.getSheetName(0)).isEqualTo("Instrucciones");
      assertThat(libro.getSheet("Productos").getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("Código");
      assertThat(libro.getSheet("Inventario inicial")).isNotNull();
      assertThat(libro.getSheet("Clientes")).isNotNull();
      assertThat(libro.getSheet("Proveedores")).isNotNull();
    }
    subir("/api/v1/carga-inicial/validar", plantilla)
        .andExpect(jsonPath("$.valido").value(true))
        .andExpect(jsonPath("$.resumen.valorUsd.monto").value("0.0000"));
  }
}
