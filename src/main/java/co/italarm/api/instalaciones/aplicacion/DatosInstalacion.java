package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Instalación a registrar o a previsualizar (RF-107 a RF-119).
 *
 * @param fecha puede ser anterior a hoy, nunca futura; vacía es hoy (P-38)
 * @param direccion vacía = la del cliente (RF-107)
 * @param tecnicos ids de usuarios activos, al menos uno (P-37)
 * @param garantiaManoObraMeses 1 a 3; vacío = el de Configuración (P-39)
 * @param condicionesGarantia vacías = las de Configuración (RF-115)
 * @param monedasComprobante otras monedas en que el PDF muestra los totales (P-34)
 * @param cotizacionId cotización aprobada de la que sale la instalación (RF-95), o vacío
 */
public record DatosInstalacion(
    Long clienteId,
    LocalDate fecha,
    String direccion,
    String descripcion,
    List<Long> tecnicos,
    Moneda moneda,
    List<LineaMaterial> lineas,
    BigDecimal manoDeObra,
    TipoDescuento descuentoTipo,
    BigDecimal descuentoValor,
    Integer garantiaManoObraMeses,
    String condicionesGarantia,
    String observaciones,
    Set<Moneda> monedasComprobante,
    Long cotizacionId) {}
