package co.italarm.api.configuracion.infraestructura;

import co.italarm.api.configuracion.dominio.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepositorio extends JpaRepository<Configuracion, Integer> {}
