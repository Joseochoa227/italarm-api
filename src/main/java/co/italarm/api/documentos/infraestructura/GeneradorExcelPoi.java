package co.italarm.api.documentos.infraestructura;

import co.italarm.api.documentos.aplicacion.GeneradorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Excel con Apache POI en modo de flujo (SXSSF), para exportar miles de filas sin llenar la
 * memoria. Cada hoja empieza con la empresa, el título, los filtros y la fecha de generación; los
 * montos son números con el formato de su moneda (RNF-04).
 */
@Component
public class GeneradorExcelPoi implements GeneradorExcel {

  private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final int ANCHO_MAXIMO = 60 * 256;

  @Override
  public byte[] generar(LibroExcel libro, String empresa, Instant generadoEn) {
    try (SXSSFWorkbook excel = new SXSSFWorkbook(200);
        ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
      Estilos estilos = new Estilos(excel);
      for (LibroExcel.Hoja hoja : libro.hojas()) {
        escribir(excel, estilos, hoja, empresa, generadoEn);
      }
      excel.write(salida);
      excel.dispose();
      return salida.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo generar el Excel", e);
    }
  }

  private static void escribir(
      SXSSFWorkbook excel,
      Estilos estilos,
      LibroExcel.Hoja hoja,
      String empresa,
      Instant generadoEn) {
    SXSSFSheet hojaExcel = excel.createSheet(WorkbookUtil.createSafeSheetName(hoja.nombre()));
    int fila = 0;
    texto(hojaExcel.createRow(fila++), 0, empresa, estilos.titulo);
    texto(hojaExcel.createRow(fila++), 0, hoja.titulo(), estilos.titulo);
    for (String filtro : hoja.filtros()) {
      texto(hojaExcel.createRow(fila++), 0, filtro, null);
    }
    texto(
        hojaExcel.createRow(fila++),
        0,
        "Generado el " + GENERADO.format(generadoEn.atZone(FechaNegocio.ZONA)),
        null);
    fila++;
    Row encabezado = hojaExcel.createRow(fila++);
    List<String> columnas = hoja.columnas();
    int[] anchos = new int[columnas.size()];
    for (int c = 0; c < columnas.size(); c++) {
      texto(encabezado, c, columnas.get(c), estilos.encabezado);
      anchos[c] = columnas.get(c).length();
    }
    for (List<LibroExcel.Celda> datos : hoja.filas()) {
      Row row = hojaExcel.createRow(fila++);
      for (int c = 0; c < datos.size(); c++) {
        celda(row, c, datos.get(c), estilos);
        if (c < anchos.length) {
          anchos[c] = Math.max(anchos[c], caracteres(datos.get(c)));
        }
      }
    }
    // Ancho por la cantidad de caracteres: autoSizeColumn mide cada celda y es muy lento con miles
    // de filas.
    for (int c = 0; c < columnas.size(); c++) {
      hojaExcel.setColumnWidth(c, Math.min((anchos[c] + 2) * 256, ANCHO_MAXIMO));
    }
  }

  private static void celda(Row fila, int columna, LibroExcel.Celda valor, Estilos estilos) {
    if (valor == null) {
      return;
    }
    Cell celda = fila.createCell(columna);
    switch (valor) {
      case LibroExcel.Texto t -> celda.setCellValue(t.valor());
      case LibroExcel.Numero n -> {
        celda.setCellValue(n.valor().doubleValue());
        celda.setCellStyle(estilos.numero);
      }
      case LibroExcel.Monto m -> {
        celda.setCellValue(m.valor().doubleValue());
        celda.setCellStyle(estilos.monedas.get(m.moneda()));
      }
      case LibroExcel.Fecha f -> {
        celda.setCellValue(f.valor());
        celda.setCellStyle(estilos.fecha);
      }
      case LibroExcel.FechaHora h -> {
        celda.setCellValue(LocalDateTime.ofInstant(h.valor(), FechaNegocio.ZONA));
        celda.setCellStyle(estilos.fechaHora);
      }
    }
  }

  /** Caracteres aproximados que ocupa la celda en pantalla. */
  private static int caracteres(LibroExcel.Celda valor) {
    return switch (valor) {
      case null -> 0;
      case LibroExcel.Texto t -> t.valor().length();
      case LibroExcel.Numero n -> n.valor().toPlainString().length() + 2;
      case LibroExcel.Monto m -> m.valor().toBigInteger().toString().length() + 8;
      case LibroExcel.Fecha f -> 10;
      case LibroExcel.FechaHora h -> 16;
    };
  }

  private static void texto(Row fila, int columna, String valor, CellStyle estilo) {
    Cell celda = fila.createCell(columna);
    celda.setCellValue(valor);
    if (estilo != null) {
      celda.setCellStyle(estilo);
    }
  }

  /** Estilos compartidos por todas las hojas (Excel limita la cantidad de estilos). */
  private static final class Estilos {

    private final CellStyle titulo;
    private final CellStyle encabezado;
    private final CellStyle numero;
    private final CellStyle fecha;
    private final CellStyle fechaHora;
    private final Map<Moneda, CellStyle> monedas = new EnumMap<>(Moneda.class);

    private Estilos(Workbook excel) {
      DataFormat formatos = excel.createDataFormat();
      Font negrita = excel.createFont();
      negrita.setBold(true);
      titulo = excel.createCellStyle();
      titulo.setFont(negrita);
      encabezado = excel.createCellStyle();
      encabezado.setFont(negrita);
      numero = estilo(excel, formatos, "#,##0.###");
      fecha = estilo(excel, formatos, "dd/mm/yyyy");
      fechaHora = estilo(excel, formatos, "dd/mm/yyyy hh:mm");
      monedas.put(Moneda.COP, estilo(excel, formatos, "\"$\" #,##0"));
      monedas.put(Moneda.USD, estilo(excel, formatos, "\"US$\" #,##0.00"));
      monedas.put(Moneda.VES, estilo(excel, formatos, "\"Bs\" #,##0.00"));
    }

    private static CellStyle estilo(Workbook excel, DataFormat formatos, String formato) {
      CellStyle estilo = excel.createCellStyle();
      estilo.setDataFormat(formatos.getFormat(formato));
      return estilo;
    }
  }
}
