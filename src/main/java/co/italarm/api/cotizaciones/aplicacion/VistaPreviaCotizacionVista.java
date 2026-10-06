package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.ClienteDocumentoVista;
import co.italarm.api.comercial.aplicacion.LineaVistaPrevia;
import co.italarm.api.comercial.aplicacion.ResumenCobroVista;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.shared.dominio.Moneda;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que quedaría en la cotización, sin guardar nada (RF-81, RF-84). Es el valor oficial que
 * muestra el frontend y con el que dibuja la vista previa del PDF (RF-85). El stock es solo
 * informativo: la cotización no aparta material (RF-86).
 *
 * @param tipo VENTA o INSTALACION
 * @param vence último día de validez (P-47)
 * @param lineas con el costo a las tasas de hoy y de la última compra (RF-81, CP-09)
 * @param resumen con la utilidad estimada
 */
public record VistaPreviaCotizacionVista(
    String tipo,
    LocalDate fecha,
    int validezDias,
    LocalDate vence,
    ClienteDocumentoVista cliente,
    Moneda moneda,
    TasasDocumentoVista tasas,
    List<String> avisos,
    List<LineaVistaPrevia> lineas,
    ResumenCobroVista resumen) {}
