package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Cotización a registrar, editar o previsualizar (RF-80 a RF-84).
 *
 * @param validezDias 8, 15 o 30; vacío = el de Configuración (RF-83)
 * @param manoDeObra solo en las de instalación (RF-83)
 * @param descripcion descripción del trabajo, en las de instalación (RF-83)
 * @param monedasComprobante otras monedas en que el PDF muestra los totales (como P-34)
 */
public record DatosCotizacion(
    TipoCotizacion tipo,
    Long clienteId,
    Integer validezDias,
    Moneda moneda,
    List<LineaMaterial> lineas,
    BigDecimal manoDeObra,
    String descripcion,
    TipoDescuento descuentoTipo,
    BigDecimal descuentoValor,
    String observaciones,
    Set<Moneda> monedasComprobante) {}
