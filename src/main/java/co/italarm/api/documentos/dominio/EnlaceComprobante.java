package co.italarm.api.documentos.dominio;

import co.italarm.api.shared.dominio.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Enlace público del PDF de un documento, para enviarlo por WhatsApp (RF-134, P-33). Solo se guarda
 * el hash del token; el enlace deja de funcionar al vencer.
 */
@Entity
@Table(name = "enlace_comprobante")
public class EnlaceComprobante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "documento_tipo", nullable = false, updatable = false, length = 20)
  private TipoDocumento documentoTipo;

  @Column(name = "documento_id", nullable = false, updatable = false)
  private Long documentoId;

  @Column(name = "vence_en", nullable = false, updatable = false)
  private Instant venceEn;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn;

  @Column(name = "creado_por", updatable = false)
  private Long creadoPor;

  protected EnlaceComprobante() {}

  public EnlaceComprobante(
      String tokenHash,
      TipoDocumento documentoTipo,
      Long documentoId,
      Instant creadoEn,
      Instant venceEn,
      Long creadoPor) {
    this.tokenHash = tokenHash;
    this.documentoTipo = documentoTipo;
    this.documentoId = documentoId;
    this.creadoEn = creadoEn;
    this.venceEn = venceEn;
    this.creadoPor = creadoPor;
  }

  public boolean vigenteEn(Instant momento) {
    return momento.isBefore(venceEn);
  }

  public TipoDocumento getDocumentoTipo() {
    return documentoTipo;
  }

  public Long getDocumentoId() {
    return documentoId;
  }

  public Instant getVenceEn() {
    return venceEn;
  }
}
