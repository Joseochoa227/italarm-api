package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.ClienteDocumentoVista;
import co.italarm.api.comercial.aplicacion.ResumenCobroVista;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Detalle de una cotización (sección 3.11).
 *
 * @param consecutivo "COT-0001", o "COT-0001 v2" desde la segunda versión (P-46)
 * @param tipo VENTA o INSTALACION
 * @param diasParaVencer solo en Borrador o En evaluación (RF-90); vacío en los demás estados
 * @param porVencer en evaluación con 3 días o menos (RF-91)
 * @param descripcion descripción del trabajo, en las de instalación
 * @param resumen cobro y utilidad estimada en las tres monedas, con las tasas de la cotización
 * @param estado BORRADOR, EN_EVALUACION, APROBADA, CONVERTIDA, RECHAZADA o VENCIDA
 * @param documentoGenerado venta o instalación en que se convirtió (RF-95), o vacío
 * @param versionesAnteriores de la más reciente a la más antigua (RF-88)
 */
public record CotizacionVista(
    Long id,
    String consecutivo,
    int numeroVersion,
    String tipo,
    LocalDate fecha,
    int validezDias,
    LocalDate vence,
    Long diasParaVencer,
    boolean porVencer,
    ClienteDocumentoVista cliente,
    Moneda moneda,
    TasasDocumentoVista tasas,
    String descripcion,
    List<Linea> lineas,
    String descuentoTipo,
    BigDecimal descuentoValor,
    ResumenCobroVista resumen,
    Dinero total,
    Dinero utilidad,
    BigDecimal porcentajeUtilidad,
    String observaciones,
    Set<Moneda> monedasComprobante,
    String estado,
    Instant enviadaEn,
    Instant aprobadaEn,
    Rechazo rechazo,
    LocalDate vencidaEl,
    DocumentoGenerado documentoGenerado,
    List<VersionAnterior> versionesAnteriores,
    String registradaPor,
    Instant registradaEn,
    long version) {

  /**
   * Producto cotizado.
   *
   * @param costoUnitarioUsd costo vigente al cotizar
   */
  public record Linea(
      Long productoId,
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      Dinero precioUnitario,
      Dinero precioSugerido,
      Dinero subtotal,
      Dinero costoUnitarioUsd) {}

  /**
   * @param motivo PRECIO, COMPETENCIA u OTRO, o vacío (P-51)
   */
  public record Rechazo(String motivo, String detalle, Instant fecha) {}

  /**
   * @param tipo VENTA o INSTALACION
   * @param consecutivo por ejemplo "I-0007"
   */
  public record DocumentoGenerado(String tipo, Long id, String consecutivo, Instant fecha) {}

  /** Copia de una versión anterior (RF-88): lo que se le había enviado al cliente. */
  public record VersionAnterior(
      int numeroVersion,
      LocalDate fecha,
      LocalDate vence,
      String descripcion,
      Dinero manoDeObra,
      Dinero descuento,
      Dinero total,
      List<LineaAnterior> lineas,
      String reemplazadaPor,
      Instant reemplazadaEn) {}

  public record LineaAnterior(
      Long productoId,
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      Dinero precioUnitario,
      Dinero subtotal) {}
}
