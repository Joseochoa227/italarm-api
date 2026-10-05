package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.DocumentoRef;
import java.time.LocalDate;

/**
 * Número de serie (RF-22, RF-24).
 *
 * @param estado EN_BODEGA, VENDIDO, INSTALADO, DADO_DE_BAJA o ANULADO
 * @param documentoSalida venta, instalación o ajuste con que salió; vacío si sigue en bodega
 * @param vencimientoGarantia fin de la garantía del equipo si salió en una venta o instalación
 */
public record SerialVista(
    Long id,
    String numero,
    String estado,
    ProductoReferencia producto,
    LocalDate fechaEntrada,
    DocumentoRef documentoEntrada,
    DocumentoRef documentoSalida,
    LocalDate vencimientoGarantia) {}
