package co.italarm.api.documentos.infraestructura;

import static co.italarm.api.documentos.aplicacion.LibroExcel.fila;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.ExportacionMuyGrandeException;
import co.italarm.api.shared.dominio.Moneda;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class GeneradorExcelPoiTest {

  @Test
  void escribeEncabezadoYCeldasTipadas() throws Exception {
    LibroExcel libro =
        new LibroExcel(
            "prueba",
            List.of(
                new LibroExcel.Hoja(
                    "Ventas: octubre/2026 con un nombre muy largo",
                    "Ventas",
                    List.of("Período: 01/10/2026 al 31/10/2026"),
                    List.of("Texto", "Número", "COP", "USD", "VES", "Fecha", "Hora", "Vacía"),
                    List.of(
                        fila(
                            LibroExcel.texto("V-0001"),
                            LibroExcel.numero(new BigDecimal("12.5")),
                            LibroExcel.monto(new BigDecimal("80000"), Moneda.COP),
                            LibroExcel.dinero(new Dinero(new BigDecimal("19.5"), Moneda.USD)),
                            LibroExcel.monto(new BigDecimal("975"), Moneda.VES),
                            LibroExcel.fecha(LocalDate.of(2026, 10, 1)),
                            LibroExcel.fechaHora(Instant.parse("2026-10-01T15:30:00Z")),
                            LibroExcel.texto(null))))));

    byte[] contenido =
        new GeneradorExcelPoi().generar(libro, "ITALARM", Instant.parse("2026-10-01T15:00:00Z"));

    try (Workbook excel = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
      Sheet hoja = excel.getSheetAt(0);
      assertThat(hoja.getSheetName()).hasSizeLessThanOrEqualTo(31);
      assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ITALARM");
      assertThat(hoja.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Ventas");
      assertThat(hoja.getRow(3).getCell(0).getStringCellValue())
          .isEqualTo("Generado el 01/10/2026 10:00");
      assertThat(hoja.getRow(5).getCell(2).getStringCellValue()).isEqualTo("COP");
      Row datos = hoja.getRow(6);
      assertThat(datos.getCell(0).getStringCellValue()).isEqualTo("V-0001");
      assertThat(datos.getCell(1).getNumericCellValue()).isEqualTo(12.5);
      assertThat(datos.getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
      assertThat(datos.getCell(2).getCellStyle().getDataFormatString()).isEqualTo("\"$\" #,##0");
      assertThat(datos.getCell(3).getCellStyle().getDataFormatString())
          .isEqualTo("\"US$\" #,##0.00");
      assertThat(datos.getCell(4).getCellStyle().getDataFormatString())
          .isEqualTo("\"Bs\" #,##0.00");
      assertThat(datos.getCell(5).getLocalDateTimeCellValue())
          .isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
      assertThat(datos.getCell(6).getLocalDateTimeCellValue())
          .isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 30));
      assertThat(datos.getCell(7)).isNull();
    }
  }

  @Test
  void limiteDeFilas() {
    ExportadorExcel.exigirLimite(ExportadorExcel.LIMITE_FILAS);
    assertThatThrownBy(() -> ExportadorExcel.exigirLimite(ExportadorExcel.LIMITE_FILAS + 1))
        .isInstanceOf(ExportacionMuyGrandeException.class)
        .hasMessage("La exportación pasa de 20.000 filas. Acota las fechas o los filtros.");
  }
}
