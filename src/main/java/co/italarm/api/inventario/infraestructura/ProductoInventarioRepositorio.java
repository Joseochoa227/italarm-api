package co.italarm.api.inventario.infraestructura;

import co.italarm.api.inventario.dominio.ProductoInventario;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoInventarioRepositorio extends JpaRepository<ProductoInventario, Long> {

  /**
   * Bloquea los productos (SELECT … FOR UPDATE) en orden de id para evitar bloqueos mutuos (BP-08).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from ProductoInventario p where p.id in :ids order by p.id")
  List<ProductoInventario> bloquear(@Param("ids") Collection<Long> ids);
}
