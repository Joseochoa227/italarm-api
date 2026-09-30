package co.italarm.api.catalogo.infraestructura;

import co.italarm.api.catalogo.dominio.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductoRepositorio
    extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

  /** Categoría y unidad en la misma consulta, sin N+1 (BP-19). */
  @Override
  @EntityGraph(attributePaths = {"categoria", "unidadMedida"})
  Page<Producto> findAll(Specification<Producto> especificacion, Pageable pagina);

  @EntityGraph(attributePaths = {"categoria", "unidadMedida"})
  Optional<Producto> findConRelacionesById(Long id);

  boolean existsByCodigoIgnoreCase(String codigo);

  boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

  boolean existsByCategoriaId(Long categoriaId);

  boolean existsByUnidadMedidaId(Long unidadMedidaId);

  /** Cantidad de productos por categoría: filas {@code [categoriaId, cantidad]}. */
  @Query("select p.categoria.id, count(p) from Producto p group by p.categoria.id")
  List<Object[]> contarPorCategoria();
}
