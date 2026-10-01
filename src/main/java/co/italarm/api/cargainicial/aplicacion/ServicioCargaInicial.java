package co.italarm.api.cargainicial.aplicacion;

import co.italarm.api.cargainicial.dominio.ReglasArchivoCarga;
import co.italarm.api.catalogo.aplicacion.CategoriaVista;
import co.italarm.api.catalogo.aplicacion.ServicioCategorias;
import co.italarm.api.catalogo.aplicacion.ServicioUnidadesMedida;
import co.italarm.api.catalogo.aplicacion.UnidadMedidaVista;
import co.italarm.api.inventario.aplicacion.CargaInicialVista;
import co.italarm.api.inventario.aplicacion.CargaInventario;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;

/** Carga inicial desde Excel (sección 3.18): plantilla, validación, confirmación e historial. */
@Service
public class ServicioCargaInicial {

  private final LibroCargaInicial libro;
  private final ValidacionCargaInicial validacion;
  private final RegistroCargaInicial registro;
  private final CargaInventario inventario;
  private final ServicioIdempotencia idempotencia;
  private final ServicioCategorias categorias;
  private final ServicioUnidadesMedida unidades;

  public ServicioCargaInicial(
      LibroCargaInicial libro,
      ValidacionCargaInicial validacion,
      RegistroCargaInicial registro,
      CargaInventario inventario,
      ServicioIdempotencia idempotencia,
      ServicioCategorias categorias,
      ServicioUnidadesMedida unidades) {
    this.libro = libro;
    this.validacion = validacion;
    this.registro = registro;
    this.inventario = inventario;
    this.idempotencia = idempotencia;
    this.categorias = categorias;
    this.unidades = unidades;
  }

  /** Plantilla .xlsx (RF-149). */
  public byte[] plantilla() {
    return libro.plantilla(
        categorias.listar().stream().map(CategoriaVista::nombre).toList(),
        unidades.listar().stream().map(UnidadMedidaVista::abreviatura).toList());
  }

  /** Revisa el archivo sin guardar nada (RF-150). */
  public ResultadoCargaVista validar(byte[] contenido) {
    ReglasArchivoCarga.validar(contenido);
    return validacion.validar(libro.leer(contenido));
  }

  /**
   * Guarda el archivo si no tiene errores (RF-151), una sola vez por {@code Idempotency-Key}.
   * Devuelve la carga realizada.
   */
  public CargaInicialVista confirmar(
      byte[] contenido, String nombreArchivo, Long usuarioId, ClaveIdempotencia clave) {
    ReglasArchivoCarga.validar(contenido);
    ArchivoCarga archivo = libro.leer(contenido);
    Long id =
        idempotencia.ejecutar(
            clave, () -> registro.registrar(archivo, nombreArchivo, usuarioId, clave));
    return listar().stream()
        .filter(c -> c.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new RecursoNoEncontradoException("La carga no existe."));
  }

  /** Cargas realizadas (RF-152). */
  public List<CargaInicialVista> listar() {
    return inventario.listar();
  }
}
