package co.italarm.api.documentos.aplicacion;

import java.time.Instant;

/** Genera los archivos de Excel (BP-14: la implementa la infraestructura con Apache POI). */
public interface GeneradorExcel {

  /**
   * @param empresa primera línea del encabezado de cada hoja
   * @param generadoEn fecha y hora de generación, para el encabezado
   */
  byte[] generar(LibroExcel libro, String empresa, Instant generadoEn);
}
