package co.italarm.api.inventario.infraestructura;

import co.italarm.api.inventario.dominio.Ajuste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AjusteRepositorio
    extends JpaRepository<Ajuste, Long>, JpaSpecificationExecutor<Ajuste> {}
