package co.italarm.api.tasas.infraestructura;

import co.italarm.api.tasas.dominio.ParMoneda;
import co.italarm.api.tasas.dominio.TasaCambio;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TasaCambioRepositorio extends JpaRepository<TasaCambio, Long> {

  Optional<TasaCambio> findByParAndFecha(ParMoneda par, LocalDate fecha);

  /** Última tasa del par con fecha menor o igual a la indicada. */
  Optional<TasaCambio> findFirstByParAndFechaLessThanEqualOrderByFechaDesc(
      ParMoneda par, LocalDate fecha);

  /** Última tasa del par anterior a la fecha indicada. */
  Optional<TasaCambio> findFirstByParAndFechaLessThanOrderByFechaDesc(
      ParMoneda par, LocalDate fecha);

  @Query(
      "select t from TasaCambio t where (:par is null or t.par = :par)"
          + " and (cast(:desde as date) is null or t.fecha >= :desde)"
          + " and (cast(:hasta as date) is null or t.fecha <= :hasta)")
  Page<TasaCambio> buscar(
      @Param("par") ParMoneda par,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      Pageable pagina);
}
