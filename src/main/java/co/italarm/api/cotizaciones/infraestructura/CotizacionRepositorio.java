package co.italarm.api.cotizaciones.infraestructura;

import co.italarm.api.cotizaciones.dominio.Cotizacion;
import co.italarm.api.cotizaciones.dominio.EstadoCotizacion;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotizacionRepositorio
    extends JpaRepository<Cotizacion, Long>, JpaSpecificationExecutor<Cotizacion> {

  @EntityGraph(attributePaths = "lineas")
  Optional<Cotizacion> findConLineasById(Long id);

  /** Bloquea la cotización para cambiar su estado una sola vez aunque lleguen dos solicitudes. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Cotizacion c where c.id = :id")
  Optional<Cotizacion> bloquear(@Param("id") Long id);

  /** Candidatas a vencer (RN-13): en los estados dados y con el vencimiento antes de hoy. */
  List<Cotizacion> findByEstadoInAndVenceBefore(
      Collection<EstadoCotizacion> estados, LocalDate hoy);
}
