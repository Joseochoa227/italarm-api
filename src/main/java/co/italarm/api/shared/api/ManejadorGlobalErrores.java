package co.italarm.api.shared.api;

import co.italarm.api.shared.dominio.CodigoError;
import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce toda excepción de la API a Problem Details con código de negocio y mensaje en español
 * (BP-16, RT-05). Nunca expone detalles técnicos al cliente.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

  private final FabricaProblemas fabrica;
  private final Map<String, RestriccionConocida> restricciones = new HashMap<>();

  public ManejadorGlobalErrores(
      FabricaProblemas fabrica, ObjectProvider<RestriccionesModulo> modulos) {
    this.fabrica = fabrica;
    modulos
        .orderedStream()
        .forEach(
            modulo -> modulo.restricciones().forEach(r -> restricciones.put(r.restriccion(), r)));
  }

  @ExceptionHandler(NegocioException.class)
  public ResponseEntity<ProblemDetail> manejarNegocio(NegocioException ex) {
    ProblemDetail problema = fabrica.crear(ex.tipo(), ex.codigo(), ex.getMessage());
    return ResponseEntity.status(problema.getStatus()).body(problema);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> manejarConcurrencia(
      ObjectOptimisticLockingFailureException ex) {
    ProblemDetail problema =
        fabrica.crear(
            TipoError.CONFLICTO,
            CodigoError.MODIFICADO_POR_OTRO_USUARIO,
            "Otro usuario modificó este registro. Recarga la información e intenta de nuevo.");
    return ResponseEntity.status(problema.getStatus()).body(problema);
  }

  /** Segunda línea de defensa: la base de datos rechazó un dato duplicado o en uso (BP-09). */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> manejarIntegridad(DataIntegrityViolationException ex) {
    String nombre =
        ex.getCause() instanceof ConstraintViolationException violacion
            ? violacion.getConstraintName()
            : null;
    RestriccionConocida conocida = nombre == null ? null : restricciones.get(nombre);
    ProblemDetail problema;
    if (conocida != null) {
      problema = fabrica.crear(conocida.tipo(), conocida.codigo(), conocida.mensaje());
    } else {
      LOG.warn("Restricción de base de datos sin traducir: {}", nombre, ex);
      problema =
          fabrica.crear(
              TipoError.CONFLICTO,
              CodigoError.DATOS_EN_CONFLICTO,
              "La información entra en conflicto con otro registro. Recarga e intenta de nuevo.");
    }
    return ResponseEntity.status(problema.getStatus()).body(problema);
  }

  /** Un parámetro {@code sort} con un campo que no existe. */
  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<ProblemDetail> manejarOrdenInvalido(PropertyReferenceException ex) {
    ProblemDetail problema =
        fabrica.crear(
            TipoError.VALIDACION,
            CodigoError.VALIDACION,
            "No se puede ordenar por el campo '" + ex.getPropertyName() + "'.");
    return ResponseEntity.status(problema.getStatus()).body(problema);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccessDeniedException ex) {
    ProblemDetail problema = fabrica.crearParaEstado(HttpStatus.FORBIDDEN);
    return ResponseEntity.status(problema.getStatus()).body(problema);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> manejarInesperado(Exception ex) {
    LOG.error("Error inesperado atendiendo la petición", ex);
    ProblemDetail problema = fabrica.crearParaEstado(HttpStatus.INTERNAL_SERVER_ERROR);
    return ResponseEntity.status(problema.getStatus()).body(problema);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<ErrorCampo> errores = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errores.add(new ErrorCampo(error.getField(), error.getDefaultMessage()));
    }
    ex.getBindingResult()
        .getGlobalErrors()
        .forEach(error -> errores.add(new ErrorCampo(null, error.getDefaultMessage())));
    ProblemDetail problema =
        fabrica.crear(
            status, TipoError.VALIDACION, CodigoError.VALIDACION, "Revisa los datos ingresados.");
    problema.setProperty(FabricaProblemas.PROPIEDAD_ERRORES, errores);
    return createResponseEntity(problema, headers, status, request);
  }

  /** Reemplaza el cuerpo de las excepciones estándar de Spring MVC por el formato propio. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    Object cuerpo =
        body instanceof ProblemDetail problema
                && problema.getProperties() != null
                && problema.getProperties().containsKey(FabricaProblemas.PROPIEDAD_CODIGO)
            ? problema
            : fabrica.crearParaEstado(statusCode);
    if (statusCode.is5xxServerError()) {
      LOG.error("Error interno atendiendo la petición", ex);
    }
    return createResponseEntity(cuerpo, headers, statusCode, request);
  }
}
