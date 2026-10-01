package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.TipoDocumento;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Documento que originó un movimiento: tipo, id y consecutivo (por ejemplo C-0001). */
@Embeddable
public record DocumentoRef(
    @Enumerated(EnumType.STRING) TipoDocumento tipo, Long id, String consecutivo) {}
