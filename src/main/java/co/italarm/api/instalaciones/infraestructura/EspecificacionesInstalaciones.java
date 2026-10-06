package co.italarm.api.instalaciones.infraestructura;

import co.italarm.api.instalaciones.dominio.EstadoInstalacion;
import co.italarm.api.instalaciones.dominio.Instalacion;
import co.italarm.api.shared.dominio.EstadoGarantia;
import co.italarm.api.shared.dominio.Garantia;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de instalaciones (RF-121). Cada filtro es opcional. */
public final class EspecificacionesInstalaciones {

  private EspecificacionesInstalaciones() {}

  public static Specification<Instalacion> filtrar(
      Long clienteId,
      Long tecnicoId,
      LocalDate desde,
      LocalDate hasta,
      EstadoGarantia estadoGarantia,
      LocalDate hoy,
      boolean incluirAnuladas) {
    Specification<Instalacion> especificacion = (raiz, consulta, cb) -> cb.conjunction();
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
    if (clienteId != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("clienteId"), clienteId));
    }
    if (tecnicoId != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.isMember(tecnicoId, raiz.get("tecnicos")));
    }
    if (estadoGarantia != null) {
      LocalDate limite = hoy.plusDays(Garantia.DIAS_POR_VENCER);
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) ->
                  switch (estadoGarantia) {
                    case VENCIDA -> cb.lessThan(raiz.get("venceManoObra"), hoy);
                    case POR_VENCER -> cb.between(raiz.get("venceManoObra"), hoy, limite);
                    case VIGENTE -> cb.greaterThan(raiz.get("venceManoObra"), limite);
                  });
    }
    if (!incluirAnuladas || estadoGarantia != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), EstadoInstalacion.ACTIVA));
    }
    return especificacion;
  }
}
