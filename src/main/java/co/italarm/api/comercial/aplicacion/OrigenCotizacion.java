package co.italarm.api.comercial.aplicacion;

import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.TipoDocumento;
import java.util.Optional;

/**
 * Enlace entre una cotización y la venta o instalación que se genera de ella (RF-93 a RF-95,
 * RF-74). Lo implementa el módulo de cotizaciones, para que ventas e instalaciones no dependan de
 * él (sección 10.1).
 */
public interface OrigenCotizacion {

  /**
   * Bloquea la cotización hasta el final de la transacción y verifica que se pueda convertir en el
   * documento: Aprobada (RN-14), del mismo tipo y del mismo cliente (P-54).
   *
   * @param tipo VENTA o INSTALACION
   */
  void validarConversion(Long cotizacionId, TipoDocumento tipo, Long clienteId);

  /** Deja la cotización Convertida y enlazada al documento recién guardado (RF-95). */
  void convertir(Long cotizacionId, DocumentoRef documento, Long clienteId);

  /** Se anuló el documento generado: la cotización vuelve a Aprobada (RF-74, CP-24). */
  void revertir(Long cotizacionId, Long documentoId);

  /** Id y consecutivo de la cotización, para ir del documento a ella (RF-95). */
  Optional<CotizacionOrigenVista> origen(Long cotizacionId);
}
