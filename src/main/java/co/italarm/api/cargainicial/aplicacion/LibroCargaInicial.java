package co.italarm.api.cargainicial.aplicacion;

import java.util.List;

/** Lectura y plantilla del Excel de la carga inicial (BP-14: la implementa la infraestructura). */
public interface LibroCargaInicial {

  /** Lee las hojas de la plantilla. Lanza un error de negocio si no es un .xlsx legible. */
  ArchivoCarga leer(byte[] contenido);

  /** Plantilla con encabezados e instrucciones; lista las categorías y unidades existentes. */
  byte[] plantilla(List<String> categorias, List<String> unidades);
}
