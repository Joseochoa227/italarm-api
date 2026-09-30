package co.italarm.api.shared.api;

import co.italarm.api.shared.dominio.CodigoError;
import co.italarm.api.shared.dominio.TipoError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Construye las respuestas de error en formato Problem Details (RFC 9457) con el código de negocio
 * y el identificador de correlación (RT-05).
 */
@Component
public class FabricaProblemas {

  public static final String PROPIEDAD_CODIGO = "codigo";
  public static final String PROPIEDAD_CORRELACION = "correlationId";
  public static final String PROPIEDAD_ERRORES = "errores";

  private final ObjectMapper objectMapper;

  public FabricaProblemas(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ProblemDetail crear(TipoError tipo, String codigo, String detalle) {
    return crear(estadoDe(tipo), tipo, codigo, detalle);
  }

  public ProblemDetail crear(HttpStatusCode estado, TipoError tipo, String codigo, String detalle) {
    ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
    problema.setTitle(tipo.titulo());
    problema.setProperty(PROPIEDAD_CODIGO, codigo);
    String correlacion = MDC.get(FiltroCorrelacion.CLAVE_MDC);
    if (correlacion != null) {
      problema.setProperty(PROPIEDAD_CORRELACION, correlacion);
    }
    return problema;
  }

  /** Problema genérico según el estado HTTP, para los errores que no son de negocio. */
  public ProblemDetail crearParaEstado(HttpStatusCode estado) {
    return switch (estado.value()) {
      case 400 ->
          crear(estado, TipoError.VALIDACION, CodigoError.VALIDACION, "La petición no es válida.");
      case 401 ->
          crear(
              estado,
              TipoError.NO_AUTENTICADO,
              CodigoError.NO_AUTENTICADO,
              "Debes iniciar sesión para continuar.");
      case 403 ->
          crear(
              estado,
              TipoError.ACCESO_DENEGADO,
              CodigoError.ACCESO_DENEGADO,
              "No tienes permiso para realizar esta acción.");
      case 404 ->
          crear(
              estado,
              TipoError.NO_ENCONTRADO,
              CodigoError.RECURSO_NO_ENCONTRADO,
              "El recurso solicitado no existe.");
      case 405 ->
          crear(
              estado,
              TipoError.VALIDACION,
              CodigoError.METODO_NO_PERMITIDO,
              "La operación no está permitida sobre este recurso.");
      case 413 ->
          crear(
              estado,
              TipoError.VALIDACION,
              CodigoError.ARCHIVO_DEMASIADO_GRANDE,
              "El archivo supera el tamaño máximo permitido.");
      case 406, 415 ->
          crear(
              estado,
              TipoError.VALIDACION,
              CodigoError.FORMATO_NO_SOPORTADO,
              "El formato de la petición no es compatible.");
      default ->
          estado.is5xxServerError()
              ? crear(
                  estado,
                  TipoError.INTERNO,
                  CodigoError.ERROR_INTERNO,
                  "Ocurrió un error inesperado. Intenta de nuevo.")
              : crear(
                  estado,
                  TipoError.VALIDACION,
                  CodigoError.VALIDACION,
                  "La petición no es válida.");
    };
  }

  /** Escribe el problema directamente en la respuesta, para los filtros fuera de Spring MVC. */
  public void escribir(HttpServletResponse response, ProblemDetail problema) throws IOException {
    response.setStatus(problema.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getOutputStream(), problema);
  }

  public static HttpStatus estadoDe(TipoError tipo) {
    return switch (tipo) {
      case VALIDACION -> HttpStatus.BAD_REQUEST;
      case NO_AUTENTICADO -> HttpStatus.UNAUTHORIZED;
      case ACCESO_DENEGADO -> HttpStatus.FORBIDDEN;
      case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
      case CONFLICTO -> HttpStatus.CONFLICT;
      case REGLA_NEGOCIO -> HttpStatus.UNPROCESSABLE_ENTITY;
      case INTERNO -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
