package co.italarm.api.garantias.infraestructura;

import co.italarm.api.garantias.dominio.ReclamoGarantia;
import org.springframework.data.jpa.domain.Specification;

/** Filtros de los reclamos de garantía. Cada filtro es opcional. */
public final class EspecificacionesReclamos {

  private EspecificacionesReclamos() {}

  public static Specification<ReclamoGarantia> filtrar(
      Long instalacionId, Long serialId, Long clienteId) {
    Specification<ReclamoGarantia> especificacion = (raiz, consulta, cb) -> cb.conjunction();
    if (instalacionId != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> cb.equal(raiz.get("instalacionId"), instalacionId));
    }
    if (serialId != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("serialId"), serialId));
    }
    if (clienteId != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("clienteId"), clienteId));
    }
    return especificacion;
  }
}
