package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.DocumentoRef;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Cambio en el costo de un producto (RF-57).
 *
 * @param regla SUBE, PROMEDIO, SIN_STOCK, AJUSTE, INVENTARIO_INICIAL o ANULACION
 * @param costoFactura costo unitario en la moneda de la factura (solo compras)
 * @param tasaFactura tasa con que se convirtió la factura (vacía si fue en USD)
 */
public record CambioCostoVista(
    Long id,
    LocalDate fecha,
    DocumentoRef documento,
    String regla,
    Dinero costoAnteriorUsd,
    Dinero costoNuevoUsd,
    Dinero costoFactura,
    BigDecimal tasaFactura,
    Dinero costoFacturaUsd,
    String usuario,
    Instant registradoEn) {}
