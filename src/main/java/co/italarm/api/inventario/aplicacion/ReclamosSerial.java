package co.italarm.api.inventario.aplicacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Reclamos de garantía de un serial para su historial (RF-24). Los registra el módulo de garantías;
 * así el inventario no depende de él.
 */
public interface ReclamosSerial {

  List<Reclamo> deSerial(Long serialId);

  record Reclamo(Long id, LocalDate fecha, String problema, String solucion, boolean enGarantia) {}
}
