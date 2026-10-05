package co.italarm.api.documentos.aplicacion;

/** Genera los PDF de los documentos (BP-14: la implementa la infraestructura). */
public interface GeneradorPdf {

  byte[] generar(DocumentoPdf documento);
}
