package co.italarm.api.reportes.aplicacion;

import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Utilidad por documento del período (RF-143) con los totales del filtro en USD.
 *
 * @param porcentajeUtilidad utilidad ÷ cobrado × 100
 */
public record ReporteUtilidadVista(
    LocalDate desde,
    LocalDate hasta,
    Pagina<UtilidadDocumentoVista> documentos,
    Dinero totalUsd,
    Dinero costoUsd,
    Dinero utilidadUsd,
    BigDecimal porcentajeUtilidad) {}
