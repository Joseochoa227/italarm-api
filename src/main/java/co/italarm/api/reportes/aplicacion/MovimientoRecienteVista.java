package co.italarm.api.reportes.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.time.Instant;

/**
 * Movimiento reciente de Inicio (RF-13, P-61). Al tocarlo se abre su detalle con {@code tipo} e
 * {@code id}.
 *
 * @param tipo VENTA, INSTALACION, COMPRA, AJUSTE o INVENTARIO_INICIAL
 * @param detalle cliente, proveedor, o el producto ajustado
 * @param valor total del documento en su moneda; los ajustes y la carga inicial, a costo en USD
 */
public record MovimientoRecienteVista(
    String tipo,
    Long id,
    String consecutivo,
    String detalle,
    String usuario,
    Instant registradoEn,
    Dinero valor,
    boolean anulado) {}
