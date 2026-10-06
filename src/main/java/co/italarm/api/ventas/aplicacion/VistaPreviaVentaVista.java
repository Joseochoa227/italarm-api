package co.italarm.api.ventas.aplicacion;

import co.italarm.api.comercial.aplicacion.ClienteDocumentoVista;
import co.italarm.api.comercial.aplicacion.LineaVistaPrevia;
import co.italarm.api.comercial.aplicacion.ResumenCobroVista;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.shared.dominio.Moneda;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que pasaría al guardar la venta, sin guardar nada (RF-98 a RF-101). Es el valor oficial que
 * muestra el frontend (BF-06).
 *
 * @param puedeGuardar false si alguna línea no tiene stock suficiente (RF-101)
 * @param avisos por ejemplo, que la tasa del bolívar no es la de hoy (RF-33)
 */
public record VistaPreviaVentaVista(
    LocalDate fecha,
    ClienteDocumentoVista cliente,
    Moneda moneda,
    TasasDocumentoVista tasas,
    List<String> avisos,
    List<LineaVistaPrevia> lineas,
    ResumenCobroVista resumen,
    boolean puedeGuardar) {}
