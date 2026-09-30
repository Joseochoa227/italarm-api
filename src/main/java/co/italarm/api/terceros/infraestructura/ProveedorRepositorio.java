package co.italarm.api.terceros.infraestructura;

import co.italarm.api.terceros.dominio.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProveedorRepositorio
    extends JpaRepository<Proveedor, Long>, JpaSpecificationExecutor<Proveedor> {}
