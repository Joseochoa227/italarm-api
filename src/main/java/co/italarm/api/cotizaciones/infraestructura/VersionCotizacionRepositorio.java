package co.italarm.api.cotizaciones.infraestructura;

import co.italarm.api.cotizaciones.dominio.VersionCotizacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionCotizacionRepositorio extends JpaRepository<VersionCotizacion, Long> {

  List<VersionCotizacion> findByCotizacionIdOrderByNumeroVersionDesc(Long cotizacionId);
}
