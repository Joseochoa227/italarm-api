package co.italarm.api.terceros.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Documentos de un cliente que registran otros módulos (ventas ahora; instalaciones desde la Fase
 * 4). Así los clientes muestran sus movimientos sin depender de esos módulos (RF-76, RF-77).
 */
public interface MovimientosCliente {

  /** Cantidad de documentos no anulados y fecha del último, por cliente (P-36). */
  Map<Long, Resumen> resumen(Collection<Long> clienteIds);

  /** Todos los documentos del cliente, incluidos los anulados. */
  List<Movimiento> historial(Long clienteId);

  record Resumen(long cantidad, LocalDate ultimo) {}

  /**
   * Un documento del historial del cliente.
   *
   * @param tipo VENTA (INSTALACION desde la Fase 4)
   * @param descripcion resumen de productos o del trabajo
   * @param estado ACTIVA o ANULADA
   */
  record Movimiento(
      String tipo,
      Long id,
      String consecutivo,
      LocalDate fecha,
      String descripcion,
      Dinero total,
      String estado) {}
}
