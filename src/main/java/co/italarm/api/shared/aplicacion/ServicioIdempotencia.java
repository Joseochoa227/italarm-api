package co.italarm.api.shared.aplicacion;

import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotenciaInvalidaException;
import co.italarm.api.shared.dominio.RegistroIdempotencia;
import co.italarm.api.shared.infraestructura.IdempotenciaRepositorio;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotency-Key (RT-07, BF-10): un doble toque en el celular no crea dos documentos.
 *
 * <p>La creación reserva la clave al empezar, dentro de su transacción. Si llega la misma clave
 * mientras la primera está en curso, la base de datos la detiene en el índice único hasta que la
 * primera termina; entonces se devuelve el documento que creó la primera.
 */
@Service
public class ServicioIdempotencia {

  private static final String RESTRICCION = "uq_idempotencia";

  private final IdempotenciaRepositorio registros;
  private final Clock reloj;

  public ServicioIdempotencia(IdempotenciaRepositorio registros, Clock reloj) {
    this.registros = registros;
    this.reloj = reloj;
  }

  /**
   * Ejecuta la creación una sola vez por clave. Sin clave, simplemente la ejecuta. Devuelve el id
   * del documento.
   */
  public Long ejecutar(ClaveIdempotencia clave, Supplier<Long> creacion) {
    if (clave == null) {
      return creacion.get();
    }
    validar(clave);
    Optional<Long> existente = documento(clave);
    if (existente.isPresent()) {
      return existente.get();
    }
    try {
      return creacion.get();
    } catch (DataIntegrityViolationException e) {
      if (e.getCause() instanceof ConstraintViolationException violacion
          && RESTRICCION.equals(violacion.getConstraintName())) {
        return documento(clave).orElseThrow(() -> e);
      }
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public Optional<Long> documento(ClaveIdempotencia clave) {
    return registros
        .findByUsuarioIdAndOperacionAndClave(clave.usuarioId(), clave.operacion(), clave.valor())
        .map(RegistroIdempotencia::getDocumentoId);
  }

  /** Reserva la clave dentro de la transacción de la creación. Sin clave no hace nada. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void reservar(ClaveIdempotencia clave) {
    if (clave != null) {
      registros.saveAndFlush(
          new RegistroIdempotencia(
              clave.usuarioId(), clave.operacion(), clave.valor(), reloj.instant()));
    }
  }

  /** Asocia el documento creado a la clave reservada. Sin clave no hace nada. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void asociar(ClaveIdempotencia clave, Long documentoId) {
    if (clave != null) {
      registros
          .findByUsuarioIdAndOperacionAndClave(clave.usuarioId(), clave.operacion(), clave.valor())
          .orElseThrow(() -> new IllegalStateException("La clave no estaba reservada"))
          .asociar(documentoId);
    }
  }

  private static void validar(ClaveIdempotencia clave) {
    if (clave.valor().isBlank() || clave.valor().length() > ClaveIdempotencia.LONGITUD_MAXIMA) {
      throw new ClaveIdempotenciaInvalidaException();
    }
  }
}
