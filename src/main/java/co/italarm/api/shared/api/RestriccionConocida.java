package co.italarm.api.shared.api;

import co.italarm.api.shared.dominio.TipoError;

/**
 * Error de negocio que corresponde a una restricción de la base de datos (BP-09). Los servicios
 * validan antes de guardar; esto cubre el caso en que dos usuarios guardan al mismo tiempo.
 *
 * @param restriccion nombre de la restricción o índice en PostgreSQL
 */
public record RestriccionConocida(
    String restriccion, TipoError tipo, String codigo, String mensaje) {}
