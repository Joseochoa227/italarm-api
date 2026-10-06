package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.ClienteDocumentoVista;
import co.italarm.api.comercial.aplicacion.LineaVistaPrevia;
import co.italarm.api.comercial.aplicacion.ResumenCobroVista;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.shared.dominio.Moneda;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que pasaría al guardar la instalación, sin guardar nada (RF-108, RF-113, RF-119). Es el valor
 * oficial que muestra el frontend (BF-06).
 *
 * @param direccion la que se guardaría (la del cliente si no se escribe otra)
 * @param puedeGuardar false si alguna línea no tiene stock suficiente (RF-108)
 */
public record VistaPreviaInstalacionVista(
    LocalDate fecha,
    ClienteDocumentoVista cliente,
    String direccion,
    Moneda moneda,
    TasasDocumentoVista tasas,
    List<String> avisos,
    List<LineaVistaPrevia> lineas,
    ResumenCobroVista resumen,
    GarantiasInstalacionVista garantias,
    boolean puedeGuardar) {}
