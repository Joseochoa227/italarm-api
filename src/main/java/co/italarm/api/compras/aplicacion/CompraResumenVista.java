package co.italarm.api.compras.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Compra en el listado (RF-46).
 *
 * @param productos resumen, por ejemplo "Cámara IP × 3, Cable UTP × 100 m y 2 más"
 */
public record CompraResumenVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    CompraVista.Referencia proveedor,
    String numeroFactura,
    Moneda moneda,
    TasasCompraVista tasas,
    Dinero total,
    Dinero totalUsd,
    String estado,
    String productos,
    String facturaUrl,
    String registradaPor,
    Instant registradaEn) {}
