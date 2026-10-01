package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.DocumentoRef;
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

/**
 * Movimiento del kárdex (RF-56). Solo se inserta: una anulación genera el movimiento contrario
 * (BP-10). El saldo es el stock del producto después del movimiento.
 */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "registrado_en", nullable = false, updatable = false)
  private Instant registradoEn;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, updatable = false, length = 25)
  private TipoMovimiento tipo;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "tipo", column = @Column(name = "documento_tipo", updatable = false)),
    @AttributeOverride(name = "id", column = @Column(name = "documento_id", updatable = false)),
    @AttributeOverride(
        name = "consecutivo",
        column = @Column(name = "documento_consecutivo", updatable = false))
  })
  private DocumentoRef documento;

  @Column(name = "entrada", nullable = false, updatable = false, precision = 14, scale = 3)
  private BigDecimal entrada;

  @Column(name = "salida", nullable = false, updatable = false, precision = 14, scale = 3)
  private BigDecimal salida;

  @Column(name = "saldo", nullable = false, updatable = false, precision = 14, scale = 3)
  private BigDecimal saldo;

  @Column(name = "costo_unitario_usd", updatable = false, precision = 19, scale = 4)
  private BigDecimal costoUnitarioUsd;

  @Column(name = "detalle", updatable = false, length = 300)
  private String detalle;

  @Column(name = "usuario_id", updatable = false)
  private Long usuarioId;

  protected MovimientoInventario() {}

  public static MovimientoInventario entrada(
      Long productoId,
      TipoMovimiento tipo,
      DocumentoRef documento,
      LocalDate fecha,
      BigDecimal cantidad,
      BigDecimal saldo,
      BigDecimal costoUnitarioUsd,
      String detalle,
      Long usuarioId,
      Instant ahora) {
    return crear(
        productoId,
        tipo,
        documento,
        fecha,
        cantidad,
        BigDecimal.ZERO,
        saldo,
        costoUnitarioUsd,
        detalle,
        usuarioId,
        ahora);
  }

  public static MovimientoInventario salida(
      Long productoId,
      TipoMovimiento tipo,
      DocumentoRef documento,
      LocalDate fecha,
      BigDecimal cantidad,
      BigDecimal saldo,
      BigDecimal costoUnitarioUsd,
      String detalle,
      Long usuarioId,
      Instant ahora) {
    return crear(
        productoId,
        tipo,
        documento,
        fecha,
        BigDecimal.ZERO,
        cantidad,
        saldo,
        costoUnitarioUsd,
        detalle,
        usuarioId,
        ahora);
  }

  private static MovimientoInventario crear(
      Long productoId,
      TipoMovimiento tipo,
      DocumentoRef documento,
      LocalDate fecha,
      BigDecimal entrada,
      BigDecimal salida,
      BigDecimal saldo,
      BigDecimal costoUnitarioUsd,
      String detalle,
      Long usuarioId,
      Instant ahora) {
    MovimientoInventario movimiento = new MovimientoInventario();
    movimiento.productoId = productoId;
    movimiento.tipo = tipo;
    movimiento.documento = documento;
    movimiento.fecha = fecha;
    movimiento.entrada = entrada;
    movimiento.salida = salida;
    movimiento.saldo = saldo;
    movimiento.costoUnitarioUsd = costoUnitarioUsd;
    movimiento.detalle = detalle;
    movimiento.usuarioId = usuarioId;
    movimiento.registradoEn = ahora;
    return movimiento;
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

  public TipoMovimiento getTipo() {
    return tipo;
  }

  public DocumentoRef getDocumento() {
    return documento;
  }

  public BigDecimal getEntrada() {
    return entrada;
  }

  public BigDecimal getSalida() {
    return salida;
  }

  public BigDecimal getSaldo() {
    return saldo;
  }

  public BigDecimal getCostoUnitarioUsd() {
    return costoUnitarioUsd;
  }

  public String getDetalle() {
    return detalle;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }
}
