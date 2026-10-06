package co.italarm.api.cotizaciones.infraestructura;

import co.italarm.api.cotizaciones.dominio.Cotizacion;
import co.italarm.api.cotizaciones.dominio.EstadoCotizacion;
import co.italarm.api.cotizaciones.dominio.ReglasCotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de cotizaciones (RF-89, RF-91). Cada filtro es opcional. */
public final class EspecificacionesCotizaciones {

  private EspecificacionesCotizaciones() {}

  /**
   * @param porVencer solo las en evaluación que vencen entre hoy y dentro de 3 días (RF-91)
   */
  public static Specification<Cotizacion> filtrar(
      EstadoCotizacion estado,
      Long clienteId,
      TipoCotizacion tipo,
      LocalDate desde,
      LocalDate hasta,
      boolean porVencer,
      LocalDate hoy) {
    Specification<Cotizacion> especificacion = (raiz, consulta, cb) -> cb.conjunction();
    if (estado != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado));
    }
    if (clienteId != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("clienteId"), clienteId));
    }
    if (tipo != null) {
      especificacion = especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("tipo"), tipo));
    }
    if (desde != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get("fecha"), desde));
    }
    if (hasta != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get("fecha"), hasta));
    }
    if (porVencer) {
      LocalDate limite = hoy.plusDays(ReglasCotizacion.DIAS_POR_VENCER);
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) ->
                  cb.and(
                      cb.equal(raiz.get("estado"), EstadoCotizacion.EN_EVALUACION),
                      cb.between(raiz.get("vence"), hoy, limite)));
    }
    return especificacion;
  }
}
