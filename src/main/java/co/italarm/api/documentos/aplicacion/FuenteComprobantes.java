package co.italarm.api.documentos.aplicacion;

import co.italarm.api.shared.dominio.TipoDocumento;

/**
 * Módulo que sabe generar el PDF de un tipo de documento (ventas ahora; instalaciones y
 * cotizaciones después), para servirlo desde un enlace público.
 */
public interface FuenteComprobantes {

  TipoDocumento tipo();

  /** PDF del documento; lanza un error de negocio si no existe. */
  ArchivoGenerado pdf(Long documentoId);
}
