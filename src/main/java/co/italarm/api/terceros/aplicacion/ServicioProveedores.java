package co.italarm.api.terceros.aplicacion;

import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.terceros.dominio.DatosProveedor;
import co.italarm.api.terceros.dominio.Proveedor;
import co.italarm.api.terceros.infraestructura.EspecificacionesTerceros;
import co.italarm.api.terceros.infraestructura.ProveedorRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Proveedores (RF-37): crear, editar y consultar. No se eliminan (P-11). */
@Service
public class ServicioProveedores {

  private final ProveedorRepositorio proveedores;

  public ServicioProveedores(ProveedorRepositorio proveedores) {
    this.proveedores = proveedores;
  }

  @Transactional(readOnly = true)
  public Page<ProveedorVista> buscar(String buscar, Pageable pagina) {
    return proveedores
        .findAll(EspecificacionesTerceros.proveedores(buscar), pagina)
        .map(ProveedorVista::de);
  }

  @Transactional(readOnly = true)
  public ProveedorVista detalle(Long id) {
    return ProveedorVista.de(buscarProveedor(id));
  }

  @Transactional
  public ProveedorVista crear(DatosProveedor datos) {
    return ProveedorVista.de(proveedores.saveAndFlush(Proveedor.crear(datos)));
  }

  @Transactional
  public ProveedorVista actualizar(Long id, DatosProveedor datos, long version) {
    Proveedor proveedor = buscarProveedor(id);
    proveedor.verificarVersion(version);
    proveedor.actualizar(datos);
    proveedores.flush();
    return ProveedorVista.de(proveedor);
  }

  private Proveedor buscarProveedor(Long id) {
    return proveedores
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El proveedor no existe."));
  }
}
