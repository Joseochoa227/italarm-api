package co.italarm.api.terceros.infraestructura;

import co.italarm.api.terceros.dominio.Cliente;
import co.italarm.api.terceros.dominio.Proveedor;
import co.italarm.api.terceros.dominio.TipoCliente;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros de los listados de clientes (RF-76) y proveedores (RF-37). */
public final class EspecificacionesTerceros {

  private EspecificacionesTerceros() {}

  public static Specification<Cliente> clientes(TipoCliente tipo, String buscar) {
    Specification<Cliente> especificacion = (raiz, consulta, cb) -> cb.conjunction();
    if (tipo != null) {
      especificacion = especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("tipo"), tipo));
    }
    if (buscar != null && !buscar.isBlank()) {
      String patron = patron(buscar);
      String digitos = buscar.replaceAll("\\D", "");
      especificacion =
          especificacion.and(
              (raiz, consulta, cb) ->
                  cb.or(
                      cb.like(cb.lower(raiz.get("nombre")), patron, '\\'),
                      cb.like(cb.lower(raiz.get("numeroDocumento")), patron, '\\'),
                      cb.like(cb.lower(raiz.get("ciudad")), patron, '\\'),
                      digitos.length() >= 4
                          ? cb.like(raiz.get("telefono"), "%" + digitos + "%")
                          : cb.disjunction()));
    }
    return especificacion;
  }

  public static Specification<Proveedor> proveedores(String buscar) {
    if (buscar == null || buscar.isBlank()) {
      return (raiz, consulta, cb) -> cb.conjunction();
    }
    String patron = patron(buscar);
    return (raiz, consulta, cb) ->
        cb.or(
            cb.like(cb.lower(raiz.get("nombre")), patron, '\\'),
            cb.like(cb.lower(raiz.get("nit")), patron, '\\'),
            cb.like(cb.lower(raiz.get("ciudad")), patron, '\\'));
  }

  private static String patron(String buscar) {
    String escapado =
        buscar
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return "%" + escapado + "%";
  }
}
