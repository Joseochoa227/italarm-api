package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Formulario de venta o de instalación precargado con la cotización aprobada (RF-94), y los avisos
 * de lo que cambió desde que se cotizó (RF-96). El usuario completa los seriales y, en las
 * instalaciones, técnicos, fecha, fotos y garantía; al guardar envía {@code cotizacionId}.
 *
 * @param tipo VENTA o INSTALACION: a qué formulario va
 * @param direccion la del cliente, para la instalación
 * @param puedeGuardar false si a alguna línea no le alcanza el stock (RF-96, CP-23)
 */
public record ConversionCotizacionVista(
    Long cotizacionId,
    String consecutivo,
    String tipo,
    Long clienteId,
    String cliente,
    String direccion,
    Moneda moneda,
    List<Linea> lineas,
    BigDecimal manoDeObra,
    String descripcion,
    TipoDescuento descuentoTipo,
    BigDecimal descuentoValor,
    String observaciones,
    Set<Moneda> monedasComprobante,
    List<Aviso> avisos,
    boolean puedeGuardar) {

  /**
   * Línea lista para el formulario, con el precio cotizado (RF-96).
   *
   * @param disponible stock actual
   */
  public record Linea(
      Long productoId,
      String codigo,
      String nombre,
      String abreviatura,
      boolean controlaSerial,
      BigDecimal cantidad,
      BigDecimal precioUnitario,
      BigDecimal disponible) {}

  /**
   * @param tipo PRECIO, COSTO, STOCK, INACTIVO o TASA
   * @param productoId vacío si el aviso no es de un producto
   */
  public record Aviso(String tipo, Long productoId, String mensaje) {}
}
