package co.italarm.api.cargainicial.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.ErrorCarga;
import java.util.List;

/**
 * Resultado de validar el archivo sin guardar (RF-150).
 *
 * @param valido true si no hay ningún error y se puede confirmar
 * @param errores por hoja y fila
 */
public record ResultadoCargaVista(boolean valido, List<ErrorCarga> errores, Resumen resumen) {

  /**
   * Lo que se cargaría.
   *
   * @param productosConStock filas de la hoja Inventario inicial
   * @param valorUsd suma de cantidad × costo de esas filas
   */
  public record Resumen(
      int productos, int clientes, int proveedores, int productosConStock, Dinero valorUsd) {}
}
