package co.italarm.api.catalogo.infraestructura;

import co.italarm.api.catalogo.dominio.Producto;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del listado de productos (RF-51). */
public final class EspecificacionesProducto {

  private EspecificacionesProducto() {}

  public static Specification<Producto> filtrar(Long categoriaId, Boolean activo, String buscar) {
    return filtrar(categoriaId, activo, buscar, List.of());
  }

  /**
   * Como {@link #filtrar(Long, Boolean, String)}, y la búsqueda también encuentra los productos
   * indicados (por ejemplo, los que tienen un serial que coincide, RF-51).
   */
  public static Specification<Producto> filtrar(
      Long categoriaId, Boolean activo, String buscar, Collection<Long> ademasIds) {
    Specification<Producto> especificacion = (raiz, consulta, cb) -> cb.conjunction();
    if (categoriaId != null) {
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) -> cb.equal(raiz.get("categoria").get("id"), categoriaId));
    }
    if (activo != null) {
      especificacion =
          especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("activo"), activo));
    }
    if (buscar != null && !buscar.isBlank()) {
      String patron = "%" + escaparLike(buscar.trim().toLowerCase(Locale.ROOT)) + "%";
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) ->
                  cb.or(
                      cb.like(cb.lower(raiz.get("nombre")), patron, '\\'),
                      cb.like(cb.lower(raiz.get("codigo")), patron, '\\'),
                      cb.like(cb.lower(raiz.get("marca")), patron, '\\'),
                      ademasIds.isEmpty() ? cb.disjunction() : raiz.get("id").in(ademasIds)));
    }
    return especificacion;
  }

  /** Evita que % y _ escritos por el usuario actúen como comodines. */
  public static String escaparLike(String texto) {
    return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
