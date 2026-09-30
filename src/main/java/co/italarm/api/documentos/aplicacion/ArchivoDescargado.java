package co.italarm.api.documentos.aplicacion;

/** Archivo listo para entregar al navegador. */
public record ArchivoDescargado(byte[] contenido, String tipoContenido) {}
