package co.italarm.api.instalaciones.infraestructura;

import co.italarm.api.instalaciones.dominio.Instalacion;
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

public interface InstalacionRepositorio
    extends JpaRepository<Instalacion, Long>, JpaSpecificationExecutor<Instalacion> {

  @EntityGraph(attributePaths = {"lineas", "tecnicos"})
  Optional<Instalacion> findConLineasById(Long id);

  /** Bloquea la instalación para anularla una sola vez aunque lleguen dos solicitudes a la vez. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Instalacion i where i.id = :id")
  Optional<Instalacion> bloquear(@Param("id") Long id);

  /** Instalaciones no anuladas por cliente: filas {@code [clienteId, cantidad, última fecha]}. */
  @Query(
      "select i.clienteId, count(i), max(i.fecha) from Instalacion i where i.clienteId in :clientes"
          + " and i.estado = co.italarm.api.instalaciones.dominio.EstadoInstalacion.ACTIVA"
          + " group by i.clienteId")
  List<Object[]> resumenPorCliente(@Param("clientes") Collection<Long> clienteIds);

  List<Instalacion> findByClienteIdOrderByFechaDescIdDesc(Long clienteId);
}
