package co.italarm.api.tasas.aplicacion;

import java.time.LocalDate;

/** Fuente oficial de la TRM (BP-14). Se reemplaza por una simulada en las pruebas. */
public interface FuenteTrm {

  /**
   * TRM vigente en la fecha indicada. Los fines de semana y festivos rige la del último día hábil.
   *
   * @throws FuenteTrmNoDisponibleException si la fuente no responde o no tiene la TRM de la fecha
   */
  TrmPublicada consultar(LocalDate fecha);
}
