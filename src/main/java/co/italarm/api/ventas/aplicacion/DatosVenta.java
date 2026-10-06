package co.italarm.api.ventas.aplicacion;

import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Venta a registrar o a previsualizar (RF-97 a RF-100).
 *
 * @param monedasComprobante otras monedas en que el PDF muestra los totales (P-34)
 * @param cotizacionId cotización aprobada de la que sale la venta (RF-95), o vacío
 */
public record DatosVenta(
    Long clienteId,
    Moneda moneda,
    List<LineaMaterial> lineas,
    TipoDescuento descuentoTipo,
    BigDecimal descuentoValor,
    String observaciones,
    Set<Moneda> monedasComprobante,
    Long cotizacionId) {}
