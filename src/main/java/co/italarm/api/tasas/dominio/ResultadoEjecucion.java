package co.italarm.api.tasas.dominio;

/** Resultado de una ejecución de la tarea de la TRM. */
public enum ResultadoEjecucion {
  /** Se guardó la TRM oficial del día. */
  EXITO,
  /** La fuente no respondió o no tenía la TRM del día. */
  FALLO,
  /** La TRM oficial del día ya estaba guardada (idempotencia, BP-15). */
  OMITIDA
}
