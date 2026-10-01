package co.italarm.api.shared.dominio;

/**
 * Error de una fila de un archivo cargado (RF-150).
 *
 * @param hoja nombre de la hoja, por ejemplo "Inventario inicial"
 * @param fila número de fila como lo muestra Excel (la 1 es el encabezado)
 */
public record ErrorCarga(String hoja, int fila, String mensaje) {}
