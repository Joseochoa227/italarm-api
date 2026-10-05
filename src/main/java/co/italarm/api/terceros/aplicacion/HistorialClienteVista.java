package co.italarm.api.terceros.aplicacion;

import java.util.List;

/**
 * Historial de un cliente (RF-77): sus documentos del más reciente al más antiguo, con las
 * cantidades de compras de material e instalaciones no anuladas.
 */
public record HistorialClienteVista(
    Long clienteId,
    String nombre,
    long compras,
    long instalaciones,
    List<MovimientosCliente.Movimiento> movimientos) {}
