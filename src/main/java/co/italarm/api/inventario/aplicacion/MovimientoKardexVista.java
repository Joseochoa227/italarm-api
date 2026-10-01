package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.DocumentoRef;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Movimiento del kárdex (RF-56).
 *
 * @param tipo COMPRA, ANULACION_COMPRA, AJUSTE_ENTRADA, AJUSTE_SALIDA o INVENTARIO_INICIAL
 * @param detalle motivo del ajuste, o vacío
 * @param saldo stock después del movimiento
 * @param costoUnitarioUsd costo vigente después del movimiento
 */
public record MovimientoKardexVista(
    Long id,
    LocalDate fecha,
    String tipo,
    String tipoEtiqueta,
    String detalle,
    DocumentoRef documento,
    BigDecimal entrada,
    BigDecimal salida,
    BigDecimal saldo,
    Dinero costoUnitarioUsd,
    String usuario,
    Instant registradoEn) {}
