package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.EntidadAuditable;
import co.italarm.api.shared.dominio.Textos;
import co.italarm.api.shared.dominio.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Ajuste manual de inventario (RF-58 a RF-62). No se edita ni se anula (RF-70, P-24). */
@Entity
@Table(name = "ajuste")
public class Ajuste extends EntidadAuditable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "numero", nullable = false, updatable = false)
  private Long numero;

  @Column(name = "fecha", nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, updatable = false, length = 10)
  private TipoAjuste tipo;

  @Enumerated(EnumType.STRING)
  @Column(name = "motivo", nullable = false, updatable = false, length = 15)
  private MotivoAjuste motivo;

  @Column(name = "descripcion", updatable = false, length = 300)
  private String descripcion;

  @Column(name = "cantidad", nullable = false, updatable = false, precision = 14, scale = 3)
  private BigDecimal cantidad;

  @Column(
      name = "costo_unitario_usd",
      nullable = false,
      updatable = false,
      precision = 19,
      scale = 4)
  private BigDecimal costoUnitarioUsd;

  protected Ajuste() {}

  /**
   * @param cantidadConSigno positiva = entrada; negativa = salida
   */
  public static Ajuste crear(
      long numero,
      LocalDate fecha,
      Long productoId,
      MotivoAjuste motivo,
      String descripcion,
      BigDecimal cantidadConSigno,
      BigDecimal costoUnitarioUsd) {
    if (cantidadConSigno.signum() == 0) {
      throw new AjusteInvalidoException("La cantidad del ajuste no puede ser 0.");
    }
    String texto = Textos.limpiar(descripcion);
    if (motivo == MotivoAjuste.OTRO && texto == null) {
      throw new AjusteInvalidoException("Con el motivo Otro, describe el ajuste.");
    }
    Ajuste ajuste = new Ajuste();
    ajuste.numero = numero;
    ajuste.fecha = fecha;
    ajuste.productoId = productoId;
    ajuste.motivo = motivo;
    ajuste.descripcion = texto;
    ajuste.tipo = cantidadConSigno.signum() > 0 ? TipoAjuste.ENTRADA : TipoAjuste.SALIDA;
    ajuste.cantidad = cantidadConSigno.abs();
    ajuste.costoUnitarioUsd = costoUnitarioUsd;
    return ajuste;
  }

  public DocumentoRef documento() {
    return new DocumentoRef(TipoDocumento.AJUSTE, id, TipoDocumento.AJUSTE.consecutivo(numero));
  }

  /** Texto del motivo para el kárdex: "Conteo físico" u "Otro: …". */
  public String detalleMotivo() {
    return descripcion == null ? motivo.etiqueta() : motivo.etiqueta() + ": " + descripcion;
  }

  public Long getId() {
    return id;
  }

  public Long getNumero() {
    return numero;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public Long getProductoId() {
    return productoId;
  }

  public TipoAjuste getTipo() {
    return tipo;
  }

  public MotivoAjuste getMotivo() {
    return motivo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public BigDecimal getCantidad() {
    return cantidad;
  }

  public BigDecimal getCostoUnitarioUsd() {
    return costoUnitarioUsd;
  }
}
