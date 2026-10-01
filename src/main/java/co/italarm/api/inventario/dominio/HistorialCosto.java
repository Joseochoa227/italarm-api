package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.Moneda;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Cambio de costo de un producto (RF-57, RF-67). Solo se inserta. */
@Entity
@Table(name = "historial_costo")
public class HistorialCosto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "registrado_en", nullable = false, updatable = false)
  private Instant registradoEn;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "tipo", column = @Column(name = "documento_tipo", updatable = false)),
    @AttributeOverride(name = "id", column = @Column(name = "documento_id", updatable = false)),
    @AttributeOverride(
        name = "consecutivo",
        column = @Column(name = "documento_consecutivo", updatable = false))
  })
  private DocumentoRef documento;

  @Enumerated(EnumType.STRING)
  @Column(name = "moneda_factura", updatable = false, length = 3)
  private Moneda monedaFactura;

  @Column(name = "tasa_factura", updatable = false, precision = 19, scale = 6)
  private BigDecimal tasaFactura;

  @Column(name = "costo_factura", updatable = false, precision = 19, scale = 4)
  private BigDecimal costoFactura;

  @Column(name = "costo_factura_usd", updatable = false, precision = 19, scale = 6)
  private BigDecimal costoFacturaUsd;

  @Column(name = "costo_anterior", updatable = false, precision = 19, scale = 4)
  private BigDecimal costoAnterior;

  @Column(name = "costo_nuevo", updatable = false, precision = 19, scale = 4)
  private BigDecimal costoNuevo;

  @Enumerated(EnumType.STRING)
  @Column(name = "regla", nullable = false, updatable = false, length = 20)
  private ReglaCosto regla;

  @Column(name = "usuario_id", updatable = false)
  private Long usuarioId;

  protected HistorialCosto() {}

  /** Cambio de costo por una compra, con la moneda y la tasa de la factura. */
  public static HistorialCosto deCompra(
      Long productoId,
      DocumentoRef compra,
      LocalDate fecha,
      Moneda moneda,
      BigDecimal tasa,
      BigDecimal costoFactura,
      BigDecimal costoFacturaUsd,
      ResultadoCosto resultado,
      Long usuarioId,
      Instant ahora) {
    HistorialCosto historial =
        sinFactura(
            productoId,
            compra,
            fecha,
            resultado.costoAnterior(),
            resultado.costoNuevo(),
            resultado.regla(),
            usuarioId,
            ahora);
    historial.monedaFactura = moneda;
    historial.tasaFactura = tasa;
    historial.costoFactura = costoFactura;
    historial.costoFacturaUsd = costoFacturaUsd;
    return historial;
  }

  /** Cambio de costo sin factura: ajuste, inventario inicial o anulación. */
  public static HistorialCosto sinFactura(
      Long productoId,
      DocumentoRef documento,
      LocalDate fecha,
      BigDecimal costoAnterior,
      BigDecimal costoNuevo,
      ReglaCosto regla,
      Long usuarioId,
      Instant ahora) {
    HistorialCosto historial = new HistorialCosto();
    historial.productoId = productoId;
    historial.documento = documento;
    historial.fecha = fecha;
    historial.costoAnterior = costoAnterior;
    historial.costoNuevo = costoNuevo;
    historial.regla = regla;
    historial.usuarioId = usuarioId;
    historial.registradoEn = ahora;
    return historial;
  }

  public Long getId() {
    return id;
  }

  public Long getProductoId() {
    return productoId;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public Instant getRegistradoEn() {
    return registradoEn;
  }

  public DocumentoRef getDocumento() {
    return documento;
  }

  public Moneda getMonedaFactura() {
    return monedaFactura;
  }

  public BigDecimal getTasaFactura() {
    return tasaFactura;
  }

  public BigDecimal getCostoFactura() {
    return costoFactura;
  }

  public BigDecimal getCostoFacturaUsd() {
    return costoFacturaUsd;
  }

  public BigDecimal getCostoAnterior() {
    return costoAnterior;
  }

  public BigDecimal getCostoNuevo() {
    return costoNuevo;
  }

  public ReglaCosto getRegla() {
    return regla;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }
}
