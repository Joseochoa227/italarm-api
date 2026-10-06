package co.italarm.api.garantias.infraestructura;

import co.italarm.api.garantias.dominio.ReclamoGarantia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReclamoGarantiaRepositorio
    extends JpaRepository<ReclamoGarantia, Long>, JpaSpecificationExecutor<ReclamoGarantia> {}
