package co.italarm.api.reportes.api;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.reportes.aplicacion.ExportacionReportes;
import co.italarm.api.reportes.aplicacion.ReportePeriodoVista;
import co.italarm.api.reportes.aplicacion.ReporteSerialesVista;
import co.italarm.api.reportes.aplicacion.ReporteUtilidadVista;
import co.italarm.api.reportes.aplicacion.ServicioReportes;
import co.italarm.api.reportes.dominio.Periodo;
import co.italarm.api.reportes.dominio.TipoPeriodo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes", description = "Reportes por período y exportación a Excel (sección 3.16)")
public class ReporteControlador {

  static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final ServicioReportes servicio;
  private final ExportacionReportes exportacion;

  public ReporteControlador(ServicioReportes servicio, ExportacionReportes exportacion) {
    this.servicio = servicio;
    this.exportacion = exportacion;
  }

  @GetMapping("/periodo")
  @Operation(
      summary = "Reporte del período",
      description =
          "tipo SEMANA (lunes a domingo), MES o ANIO alrededor de fecha (hoy si falta), o"
              + " tipo=RANGO con desde y hasta. Sin parámetros, el mes en curso. Error:"
              + " PERIODO_INVALIDO (400).")
  public ReportePeriodoVista periodo(
      @RequestParam(required = false) TipoPeriodo tipo,
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta) {
    return servicio.reporte(servicio.periodo(tipo, fecha, desde, hasta));
  }

  @GetMapping(path = "/periodo/excel", produces = EXCEL)
  @Operation(summary = "Reporte del período en Excel, una hoja por bloque")
  public ResponseEntity<byte[]> periodoExcel(
      @RequestParam(required = false) TipoPeriodo tipo,
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta) {
    return descarga(exportacion.periodo(servicio.periodo(tipo, fecha, desde, hasta)));
  }

  @GetMapping("/utilidad")
  @Operation(
      summary = "Utilidad por venta e instalación",
      description =
          "Mismo período que /periodo. tipo: VENTA o INSTALACION (vacío, ambas). Orden por"
              + " fecha, o por utilidad con ordenarPorUtilidad=true.")
  public ReporteUtilidadVista utilidad(
      @RequestParam(required = false) TipoPeriodo periodo,
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @Parameter(description = "VENTA o INSTALACION") @RequestParam(required = false)
          TipoDocumentoUtilidad tipo,
      @RequestParam(required = false) Boolean ordenarPorUtilidad,
      @ParameterObject @PageableDefault(size = 20) Pageable pagina) {
    return servicio.utilidad(
        servicio.periodo(periodo, fecha, desde, hasta),
        tipo == null ? null : tipo.name(),
        Boolean.TRUE.equals(ordenarPorUtilidad),
        pagina);
  }

  @GetMapping(path = "/utilidad/excel", produces = EXCEL)
  @Operation(summary = "Utilidad por documento en Excel")
  public ResponseEntity<byte[]> utilidadExcel(
      @RequestParam(required = false) TipoPeriodo periodo,
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @RequestParam(required = false) TipoDocumentoUtilidad tipo,
      @RequestParam(required = false) Boolean ordenarPorUtilidad) {
    Periodo elegido = servicio.periodo(periodo, fecha, desde, hasta);
    return descarga(
        exportacion.utilidad(
            elegido, tipo == null ? null : tipo.name(), Boolean.TRUE.equals(ordenarPorUtilidad)));
  }

  @GetMapping("/seriales")
  @Operation(
      summary = "Seriales en bodega, vendidos e instalados",
      description = "Conteo por estado y listado paginado, del más reciente al más antiguo.")
  public ReporteSerialesVista seriales(
      @RequestParam(required = false) EstadoSerialReporte estado,
      @RequestParam(required = false) Long productoId,
      @ParameterObject @PageableDefault(size = 20) Pageable pagina) {
    return servicio.seriales(estado == null ? null : estado.name(), productoId, pagina);
  }

  @GetMapping(path = "/seriales/excel", produces = EXCEL)
  @Operation(summary = "Seriales en Excel")
  public ResponseEntity<byte[]> serialesExcel(
      @RequestParam(required = false) EstadoSerialReporte estado,
      @RequestParam(required = false) Long productoId) {
    return descarga(exportacion.seriales(estado == null ? null : estado.name(), productoId));
  }

  /** Tipos de documento del reporte de utilidad. */
  public enum TipoDocumentoUtilidad {
    VENTA,
    INSTALACION
  }

  /** Estados de un serial (RF-141). */
  public enum EstadoSerialReporte {
    EN_BODEGA,
    VENDIDO,
    INSTALADO,
    DADO_DE_BAJA,
    ANULADO
  }

  static ResponseEntity<byte[]> descarga(ArchivoGenerado archivo) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
        .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
        .body(archivo.contenido());
  }
}
