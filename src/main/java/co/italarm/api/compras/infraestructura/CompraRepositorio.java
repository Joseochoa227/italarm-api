package co.italarm.api.compras.infraestructura;

import co.italarm.api.compras.dominio.Compra;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraRepositorio
    extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

  @EntityGraph(attributePaths = "lineas")
  Optional<Compra> findConLineasById(Long id);

  /** Última compra no anulada de cada producto: filas {@code [productoId, compraId]} (P-32). */
  @Query(
      "select l.productoId, max(c.id) from LineaCompra l join l.compra c"
          + " where l.productoId in :productos"
          + " and c.estado = co.italarm.api.compras.dominio.EstadoCompra.ACTIVA"
          + " group by l.productoId")
  List<Object[]> ultimaCompraPorProducto(@Param("productos") Collection<Long> productoIds);

  /** Bloquea la compra para anularla una sola vez aunque lleguen dos solicitudes a la vez. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Compra c where c.id = :id")
  Optional<Compra> bloquear(@Param("id") Long id);
}
