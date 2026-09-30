package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.catalogo.dominio.UnidadDuplicadaException;
import co.italarm.api.catalogo.dominio.UnidadEnUsoException;
import co.italarm.api.catalogo.dominio.UnidadMedida;
import co.italarm.api.catalogo.infraestructura.ProductoRepositorio;
import co.italarm.api.catalogo.infraestructura.UnidadMedidaRepositorio;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.Textos;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Unidades de medida (RF-148). */
@Service
public class ServicioUnidadesMedida {

  private final UnidadMedidaRepositorio unidades;
  private final ProductoRepositorio productos;

  public ServicioUnidadesMedida(UnidadMedidaRepositorio unidades, ProductoRepositorio productos) {
    this.unidades = unidades;
    this.productos = productos;
  }

  @Transactional(readOnly = true)
  public List<UnidadMedidaVista> listar() {
    return unidades.findAllByOrderByNombreAsc().stream().map(UnidadMedidaVista::de).toList();
  }

  @Transactional
  public UnidadMedidaVista crear(String nombre, String abreviatura, boolean admiteDecimales) {
    exigirNoDuplicada(nombre, abreviatura, null);
    return UnidadMedidaVista.de(
        unidades.saveAndFlush(UnidadMedida.crear(nombre, abreviatura, admiteDecimales)));
  }

  @Transactional
  public UnidadMedidaVista actualizar(
      Long id, String nombre, String abreviatura, boolean admiteDecimales, long version) {
    UnidadMedida unidad = buscar(id);
    unidad.verificarVersion(version);
    exigirNoDuplicada(nombre, abreviatura, id);
    if (unidad.isAdmiteDecimales() != admiteDecimales && productos.existsByUnidadMedidaId(id)) {
      throw new UnidadEnUsoException(
          "No se puede cambiar si la unidad admite decimales: ya la usan productos.");
    }
    unidad.actualizar(nombre, abreviatura, admiteDecimales);
    unidades.flush();
    return UnidadMedidaVista.de(unidad);
  }

  @Transactional
  public void eliminar(Long id) {
    UnidadMedida unidad = buscar(id);
    if (productos.existsByUnidadMedidaId(id)) {
      throw new UnidadEnUsoException(
          "La unidad \"" + unidad.getNombre() + "\" la usan productos y no se puede eliminar.");
    }
    unidades.delete(unidad);
  }

  private void exigirNoDuplicada(String nombre, String abreviatura, Long id) {
    if (unidades.existeDuplicada(Textos.limpiar(nombre), Textos.limpiar(abreviatura), id)) {
      throw new UnidadDuplicadaException(
          "Ya existe una unidad de medida con ese nombre o abreviatura.");
    }
  }

  private UnidadMedida buscar(Long id) {
    return unidades
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La unidad de medida no existe."));
  }
}
