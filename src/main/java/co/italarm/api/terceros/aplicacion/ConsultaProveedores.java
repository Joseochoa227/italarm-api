package co.italarm.api.terceros.aplicacion;

import co.italarm.api.terceros.dominio.Proveedor;
import co.italarm.api.terceros.infraestructura.ProveedorRepositorio;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nombres de proveedores para otros módulos (compras). */
@Service
public class ConsultaProveedores {

  private final ProveedorRepositorio proveedores;

  public ConsultaProveedores(ProveedorRepositorio proveedores) {
    this.proveedores = proveedores;
  }

  @Transactional(readOnly = true)
  public Map<Long, String> nombres(Collection<Long> ids) {
    return proveedores.findAllById(ids).stream()
        .collect(Collectors.toMap(Proveedor::getId, Proveedor::getNombre));
  }
}
