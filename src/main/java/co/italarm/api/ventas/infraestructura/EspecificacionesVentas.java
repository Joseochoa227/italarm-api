package co.italarm.api.ventas.infraestructura;

import co.italarm.api.ventas.dominio.EstadoVenta;
import co.italarm.api.ventas.dominio.LineaVenta;
import co.italarm.api.ventas.dominio.Venta;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de ventas (RF-105). Cada filtro es opcional. */
public final class EspecificacionesVentas {

  private EspecificacionesVentas() {}

  public static Specification<Venta> filtrar(
      Long clienteId, Long productoId, LocalDate desde, LocalDate hasta, boolean incluirAnuladas) {
    Specification<Venta> especificacion = (raiz, consulta, cb) -> cb.conjunction();
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
    if (productoId != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> {
                Subquery<Long> lineas = consulta.subquery(Long.class);
                Root<LineaVenta> linea = lineas.from(LineaVenta.class);
                lineas
                    .select(linea.get("id"))
                    .where(
                        cb.equal(linea.get("venta"), raiz),
                        cb.equal(linea.get("productoId"), productoId));
                return cb.exists(lineas);
              });
    }
    if (!incluirAnuladas) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), EstadoVenta.ACTIVA));
    }
    return especificacion;
  }
}
