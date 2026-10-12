package co.italarm.api.documentos.aplicacion;

import co.italarm.api.shared.dominio.ExportacionMuyGrandeException;
import co.italarm.api.shared.dominio.FechaNegocio;
import org.springframework.stereotype.Service;

/**
 * Exportaciones a Excel (RF-144, RNF-06): cada módulo arma su {@link LibroExcel} con lo mismo que
 * muestra su listado y este servicio lo convierte en el archivo para descargar.
 */
@Service
public class ExportadorExcel {

  /** P-62: máximo de filas por archivo. */
  public static final int LIMITE_FILAS = 20_000;

  public static final String TIPO =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final GeneradorExcel generador;
  private final NombreEmpresa empresa;
  private final FechaNegocio fechas;

  public ExportadorExcel(GeneradorExcel generador, NombreEmpresa empresa, FechaNegocio fechas) {
    this.generador = generador;
    this.empresa = empresa;
    this.fechas = fechas;
  }

  /** "ventas-2026-10-12.xlsx". */
  public ArchivoGenerado exportar(LibroExcel libro) {
    return new ArchivoGenerado(
        libro.nombre() + "-" + fechas.hoy() + ".xlsx",
        TIPO,
        generador.generar(libro, empresa.nombre(), fechas.ahora()));
  }

  /**
   * Las consultas de exportación piden {@code LIMITE_FILAS + 1} filas: si llegan más que el límite,
   * se pide acotar los filtros (P-62).
   */
  public static void exigirLimite(long filas) {
    if (filas > LIMITE_FILAS) {
      throw new ExportacionMuyGrandeException(LIMITE_FILAS);
    }
  }
}
