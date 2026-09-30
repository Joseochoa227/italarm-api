package co.italarm.api.shared.api;

import java.util.List;

/** Cada módulo declara sus restricciones de base de datos con su error de negocio. */
public interface RestriccionesModulo {

  List<RestriccionConocida> restricciones();
}
