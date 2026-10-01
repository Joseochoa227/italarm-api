package co.italarm.api.inventario.infraestructura;

import co.italarm.api.inventario.dominio.Ajuste;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** Filtros de los listados del inventario. */
public final class EspecificacionesInventario {

  private EspecificacionesInventario() {}

  /** Ajustes por producto y rango de fechas; cada filtro es opcional. */
  public static Specification<Ajuste> ajustes(Long productoId, LocalDate desde, LocalDate hasta) {
    Specification<Ajuste> especificacion = (raiz, consulta, cb) -> cb.conjunction();
    if (productoId != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("productoId"), productoId));
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
    return especificacion;
  }
}
