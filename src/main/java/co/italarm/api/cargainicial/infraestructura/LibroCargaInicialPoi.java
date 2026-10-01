package co.italarm.api.cargainicial.infraestructura;

import co.italarm.api.cargainicial.aplicacion.ArchivoCarga;
import co.italarm.api.cargainicial.aplicacion.LibroCargaInicial;
import co.italarm.api.cargainicial.dominio.ArchivoCargaInvalidoException;
import co.italarm.api.catalogo.aplicacion.CargaProductos;
import co.italarm.api.catalogo.aplicacion.FilaProducto;
import co.italarm.api.inventario.aplicacion.CargaInventario;
import co.italarm.api.inventario.aplicacion.FilaInventario;
import co.italarm.api.shared.dominio.ErrorCarga;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.terceros.aplicacion.CargaTerceros;
import co.italarm.api.terceros.aplicacion.FilaCliente;
import co.italarm.api.terceros.aplicacion.FilaProveedor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Plantilla y lectura del Excel de la carga inicial con Apache POI (RF-149, RF-150). */
@Component
public class LibroCargaInicialPoi implements LibroCargaInicial {

  static final int FILAS_MAXIMAS = 5000;

  static final List<String> COLUMNAS_PRODUCTOS =
      List.of(
          "Código",
          "Nombre",
          "Marca",
          "Modelo",
          "Categoría",
          "Unidad",
          "Controla serial (Sí/No)",
          "Precio instalador",
          "Precio cliente final",
          "Moneda del precio (USD/COP/VES)",
          "Stock mínimo",
          "Descripción");
  static final List<String> COLUMNAS_INVENTARIO =
      List.of(
          "Código del producto", "Cantidad", "Costo unitario USD", "Seriales (separados por coma)");
  static final List<String> COLUMNAS_CLIENTES =
      List.of(
          "Tipo (Instalador/Cliente final)",
          "Nombre o razón social",
          "Tipo de documento (CC/NIT)",
          "Número de documento",
          "Teléfono o WhatsApp",
          "Correo",
          "Dirección",
          "Ciudad");
  static final List<String> COLUMNAS_PROVEEDORES =
      List.of(
          "Nombre o razón social",
          "NIT",
          "Teléfono",
          "Correo",
          "Ciudad",
          "Moneda habitual (USD/COP/VES)");

  private static final String HOJA_INSTRUCCIONES = "Instrucciones";

  @Override
  public ArchivoCarga leer(byte[] contenido) {
    try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
      Lector lector = new Lector();
      Sheet productos = hoja(libro, CargaProductos.HOJA);
      Sheet inventario = hoja(libro, CargaInventario.HOJA);
      Sheet clientes = hoja(libro, CargaTerceros.HOJA_CLIENTES);
      Sheet proveedores = hoja(libro, CargaTerceros.HOJA_PROVEEDORES);
      if (productos == null && inventario == null && clientes == null && proveedores == null) {
        throw new ArchivoCargaInvalidoException(
            "El archivo no tiene las hojas de la plantilla (Productos, Inventario inicial,"
                + " Clientes, Proveedores). Descarga la plantilla y úsala.");
      }
      return new ArchivoCarga(
          lector.filas(productos, CargaProductos.HOJA, lector::producto),
          lector.filas(inventario, CargaInventario.HOJA, lector::inventario),
          lector.filas(clientes, CargaTerceros.HOJA_CLIENTES, lector::cliente),
          lector.filas(proveedores, CargaTerceros.HOJA_PROVEEDORES, lector::proveedor),
          lector.errores);
    } catch (IOException | RuntimeException e) {
      if (e instanceof ArchivoCargaInvalidoException invalido) {
        throw invalido;
      }
      throw new ArchivoCargaInvalidoException(
          "No se pudo leer el archivo. Verifica que sea un Excel (.xlsx) de la plantilla.");
    }
  }

  @Override
  public byte[] plantilla(List<String> categorias, List<String> unidades) {
    try (XSSFWorkbook libro = new XSSFWorkbook();
        ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
      CellStyle encabezado = libro.createCellStyle();
      Font negrita = libro.createFont();
      negrita.setBold(true);
      encabezado.setFont(negrita);

      instrucciones(libro.createSheet(HOJA_INSTRUCCIONES), encabezado, categorias, unidades);
      Sheet productos =
          hojaConEncabezado(libro, CargaProductos.HOJA, COLUMNAS_PRODUCTOS, encabezado);
      lista(productos, 6, "Sí", "No");
      lista(productos, 9, "USD", "COP", "VES");
      if (!categorias.isEmpty()) {
        lista(productos, 4, categorias.toArray(String[]::new));
      }
      if (!unidades.isEmpty()) {
        lista(productos, 5, unidades.toArray(String[]::new));
      }
      hojaConEncabezado(libro, CargaInventario.HOJA, COLUMNAS_INVENTARIO, encabezado);
      Sheet clientes =
          hojaConEncabezado(libro, CargaTerceros.HOJA_CLIENTES, COLUMNAS_CLIENTES, encabezado);
      lista(clientes, 0, "Instalador", "Cliente final");
      lista(clientes, 2, "CC", "NIT");
      Sheet proveedores =
          hojaConEncabezado(
              libro, CargaTerceros.HOJA_PROVEEDORES, COLUMNAS_PROVEEDORES, encabezado);
      lista(proveedores, 5, "USD", "COP", "VES");
      libro.write(salida);
      return salida.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo generar la plantilla", e);
    }
  }

  private static void instrucciones(
      Sheet hoja, CellStyle encabezado, List<String> categorias, List<String> unidades) {
    List<String> lineas = new ArrayList<>();
    lineas.add("Plantilla de carga inicial de ITALARM");
    lineas.add("");
    lineas.add("1. Llena las hojas que necesites desde la fila 2; la fila 1 es el encabezado.");
    lineas.add("2. Si hay un solo error, no se guarda nada: el sistema muestra la hoja y la fila.");
    lineas.add("3. Productos: el código es único. Categoría por nombre y unidad por abreviatura.");
    lineas.add(
        "   Ejemplo: CAM-HK-2MP | Cámara domo 2 MP | Hikvision | DS-2CE56D0T | Cámaras | und |"
            + " Sí | 20 | 25.5 | USD | 5 | Cámara para interior");
    lineas.add(
        "4. Inventario inicial: un producto por fila, con cantidad y costo unitario en USD. Los"
            + " seriales van en la misma fila, separados por coma, uno por unidad.");
    lineas.add("   Ejemplo: CAM-HK-2MP | 2 | 20 | ABC123, ABC124");
    lineas.add(
        "   El producto puede estar en la hoja Productos o existir ya, siempre que no tenga"
            + " movimientos.");
    lineas.add("5. Clientes: tipo Instalador o Cliente final. El documento es opcional y único.");
    lineas.add(
        "   Ejemplo: Instalador | Juan Pérez | CC | 1020304050 | 3001234567 | juan@correo.com |"
            + " Calle 10 # 5-20 | Cúcuta");
    lineas.add("6. Proveedores: la moneda habitual es USD, COP o VES.");
    lineas.add(
        "   Ejemplo: Distribuidora Andina | 900123456-1 | 6071234567 | ventas@andina.com | Bogotá |"
            + " USD");
    lineas.add("7. Las cantidades y precios pueden llevar punto o coma decimal.");
    lineas.add("");
    lineas.add("Categorías: " + String.join(", ", categorias));
    lineas.add("Unidades: " + String.join(", ", unidades));
    for (int i = 0; i < lineas.size(); i++) {
      Cell celda = hoja.createRow(i).createCell(0);
      celda.setCellValue(lineas.get(i));
      if (i == 0) {
        celda.setCellStyle(encabezado);
      }
    }
    hoja.setColumnWidth(0, 120 * 256);
  }

  private static Sheet hojaConEncabezado(
      Workbook libro, String nombre, List<String> columnas, CellStyle estilo) {
    Sheet hoja = libro.createSheet(nombre);
    Row fila = hoja.createRow(0);
    for (int i = 0; i < columnas.size(); i++) {
      Cell celda = fila.createCell(i);
      celda.setCellValue(columnas.get(i));
      celda.setCellStyle(estilo);
      hoja.setColumnWidth(i, Math.max(14, columnas.get(i).length() + 4) * 256);
    }
    hoja.createFreezePane(0, 1);
    return hoja;
  }

  /** Lista desplegable en una columna, de la fila 2 a la última permitida. */
  private static void lista(Sheet hoja, int columna, String... valores) {
    DataValidationHelper ayuda = hoja.getDataValidationHelper();
    var validacion =
        ayuda.createValidation(
            ayuda.createExplicitListConstraint(valores),
            new CellRangeAddressList(1, FILAS_MAXIMAS, columna, columna));
    validacion.setShowErrorBox(false);
    hoja.addValidationData(validacion);
  }

  private static Sheet hoja(Workbook libro, String nombre) {
    String buscado = sinTildes(nombre);
    for (Sheet hoja : libro) {
      if (sinTildes(hoja.getSheetName()).equals(buscado)) {
        return hoja;
      }
    }
    return null;
  }

  private static String sinTildes(String texto) {
    return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  /** Convierte las celdas de una fila; los errores de formato quedan con su hoja y fila. */
  private static final class Lector {

    private final DataFormatter formato = new DataFormatter(Locale.ROOT);
    private final List<ErrorCarga> errores = new ArrayList<>();
    private String hoja;
    private int fila;

    <T> List<T> filas(Sheet hojaExcel, String nombre, Function<Row, T> conversion) {
      List<T> resultado = new ArrayList<>();
      if (hojaExcel == null) {
        return resultado;
      }
      this.hoja = nombre;
      for (int i = 1; i <= hojaExcel.getLastRowNum(); i++) {
        Row row = hojaExcel.getRow(i);
        if (row == null || vacia(row)) {
          continue;
        }
        if (resultado.size() >= FILAS_MAXIMAS) {
          errores.add(
              new ErrorCarga(nombre, i + 1, "La hoja admite máximo " + FILAS_MAXIMAS + " filas."));
          break;
        }
        this.fila = i + 1;
        resultado.add(conversion.apply(row));
      }
      return resultado;
    }

    FilaProducto producto(Row row) {
      return new FilaProducto(
          fila,
          texto(row, 0),
          texto(row, 1),
          texto(row, 2),
          texto(row, 3),
          texto(row, 4),
          texto(row, 5),
          siNo(row, 6, "Controla serial"),
          numero(row, 7, "Precio instalador"),
          numero(row, 8, "Precio cliente final"),
          moneda(row, 9, "Moneda del precio"),
          numero(row, 10, "Stock mínimo"),
          texto(row, 11));
    }

    FilaInventario inventario(Row row) {
      String seriales = texto(row, 3);
      return new FilaInventario(
          fila,
          texto(row, 0),
          numero(row, 1, "Cantidad"),
          numero(row, 2, "Costo unitario USD"),
          seriales == null
              ? List.of()
              : Arrays.stream(seriales.split("[,;\\n]"))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .toList());
    }

    FilaCliente cliente(Row row) {
      return new FilaCliente(
          fila,
          texto(row, 0),
          texto(row, 1),
          texto(row, 2),
          texto(row, 3),
          texto(row, 4),
          texto(row, 5),
          texto(row, 6),
          texto(row, 7));
    }

    FilaProveedor proveedor(Row row) {
      return new FilaProveedor(
          fila,
          texto(row, 0),
          texto(row, 1),
          texto(row, 2),
          texto(row, 3),
          texto(row, 4),
          moneda(row, 5, "Moneda habitual"));
    }

    private boolean vacia(Row row) {
      for (Cell celda : row) {
        if (!formato.formatCellValue(celda).isBlank()) {
          return false;
        }
      }
      return true;
    }

    private String texto(Row row, int columna) {
      Cell celda = row.getCell(columna);
      if (celda == null) {
        return null;
      }
      String valor =
          tipo(celda) == CellType.NUMERIC
              ? NumberToTextConverter.toText(celda.getNumericCellValue())
              : formato.formatCellValue(celda).trim();
      return valor.isEmpty() ? null : valor;
    }

    private BigDecimal numero(Row row, int columna, String nombre) {
      Cell celda = row.getCell(columna);
      if (celda == null) {
        return null;
      }
      if (tipo(celda) == CellType.NUMERIC) {
        return new BigDecimal(NumberToTextConverter.toText(celda.getNumericCellValue()));
      }
      String valor = texto(row, columna);
      if (valor == null) {
        return null;
      }
      String normalizado = valor.replace(" ", "");
      if (normalizado.contains(",") && !normalizado.contains(".")) {
        normalizado = normalizado.replace(',', '.');
      }
      try {
        return new BigDecimal(normalizado);
      } catch (NumberFormatException e) {
        error(nombre + ": \"" + valor + "\" no es un número válido.");
        return null;
      }
    }

    private Boolean siNo(Row row, int columna, String nombre) {
      String valor = texto(row, columna);
      if (valor == null) {
        return null;
      }
      return switch (sinTildes(valor)) {
        case "si", "s", "x", "true", "1" -> Boolean.TRUE;
        case "no", "n", "false", "0" -> Boolean.FALSE;
        default -> {
          error(nombre + ": escribe Sí o No.");
          yield null;
        }
      };
    }

    private Moneda moneda(Row row, int columna, String nombre) {
      String valor = texto(row, columna);
      if (valor == null) {
        return null;
      }
      try {
        return Moneda.valueOf(valor.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        error(nombre + ": escribe USD, COP o VES.");
        return null;
      }
    }

    private void error(String mensaje) {
      errores.add(new ErrorCarga(hoja, fila, mensaje));
    }

    private static CellType tipo(Cell celda) {
      return celda.getCellType() == CellType.FORMULA
          ? celda.getCachedFormulaResultType()
          : celda.getCellType();
    }
  }
}
