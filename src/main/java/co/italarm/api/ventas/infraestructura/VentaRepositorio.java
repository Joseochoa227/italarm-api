package co.italarm.api.ventas.infraestructura;

import co.italarm.api.ventas.dominio.Venta;
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

public interface VentaRepositorio
    extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

  @EntityGraph(attributePaths = "lineas")
  Optional<Venta> findConLineasById(Long id);

  /** Ventas no anuladas por cliente: filas {@code [clienteId, cantidad, fecha de la última]}. */
  @Query(
      "select v.clienteId, count(v), max(v.fecha) from Venta v where v.clienteId in :clientes"
          + " and v.estado = co.italarm.api.ventas.dominio.EstadoVenta.ACTIVA"
          + " group by v.clienteId")
  List<Object[]> resumenPorCliente(@Param("clientes") Collection<Long> clienteIds);

  List<Venta> findByClienteIdOrderByFechaDescIdDesc(Long clienteId);

  /** Bloquea la venta para anularla una sola vez aunque lleguen dos solicitudes a la vez. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from Venta v where v.id = :id")
  Optional<Venta> bloquear(@Param("id") Long id);
}
