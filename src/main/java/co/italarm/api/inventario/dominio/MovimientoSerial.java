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
import java.time.Instant;
import java.time.LocalDate;

/** Evento del historial de un serial (RF-24). Solo se inserta. */
@Entity
@Table(name = "movimiento_serial")
public class MovimientoSerial {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "serial_id", nullable = false, updatable = false)
  private Long serialId;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "registrado_en", nullable = false, updatable = false)
  private Instant registradoEn;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, updatable = false, length = 25)
  private TipoMovimientoSerial tipo;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "tipo", column = @Column(name = "documento_tipo", updatable = false)),
    @AttributeOverride(name = "id", column = @Column(name = "documento_id", updatable = false)),
    @AttributeOverride(
        name = "consecutivo",
        column = @Column(name = "documento_consecutivo", updatable = false))
  })
  private DocumentoRef documento;

  @Column(name = "detalle", updatable = false, length = 300)
  private String detalle;

  @Column(name = "usuario_id", updatable = false)
  private Long usuarioId;

  protected MovimientoSerial() {}

  public static MovimientoSerial de(
      Long serialId,
      TipoMovimientoSerial tipo,
      DocumentoRef documento,
      LocalDate fecha,
      String detalle,
      Long usuarioId,
      Instant ahora) {
    MovimientoSerial movimiento = new MovimientoSerial();
    movimiento.serialId = serialId;
    movimiento.tipo = tipo;
    movimiento.documento = documento;
    movimiento.fecha = fecha;
    movimiento.detalle =
        detalle == null || detalle.length() <= 300 ? detalle : detalle.substring(0, 300);
    movimiento.usuarioId = usuarioId;
    movimiento.registradoEn = ahora;
    return movimiento;
  }

  public Long getId() {
    return id;
  }

  public Long getSerialId() {
    return serialId;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public Instant getRegistradoEn() {
    return registradoEn;
  }

  public TipoMovimientoSerial getTipo() {
    return tipo;
  }

  public DocumentoRef getDocumento() {
    return documento;
  }

  public String getDetalle() {
    return detalle;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }
}
