package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.catalogo.dominio.Categoria;
import co.italarm.api.catalogo.dominio.DatosProducto;
import co.italarm.api.catalogo.dominio.Producto;
import co.italarm.api.catalogo.dominio.UnidadMedida;
import co.italarm.api.catalogo.infraestructura.CategoriaRepositorio;
import co.italarm.api.catalogo.infraestructura.ProductoRepositorio;
import co.italarm.api.catalogo.infraestructura.UnidadMedidaRepositorio;
import co.italarm.api.shared.dominio.ErrorCarga;
import co.italarm.api.shared.dominio.NegocioException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Hoja Productos de la carga inicial (sección 3.18): validar sin guardar y crear. */
@Service
public class CargaProductos {

  public static final String HOJA = "Productos";

  private final ProductoRepositorio productos;
  private final CategoriaRepositorio categorias;
  private final UnidadMedidaRepositorio unidades;
  private final Validator validador;

  public CargaProductos(
      ProductoRepositorio productos,
      CategoriaRepositorio categorias,
      UnidadMedidaRepositorio unidades,
      Validator validador) {
    this.productos = productos;
    this.categorias = categorias;
    this.unidades = unidades;
    this.validador = validador;
  }

  /**
   * Errores de la hoja y los productos nuevos que se crearían (por código, sin id), para validar la
   * hoja de inventario inicial con ellos.
   */
  public record Validacion(
      List<ErrorCarga> errores,
      Map<String, DatosProductoInventario> nuevos,
      Set<String> codigosConErrores) {}

  @Transactional(readOnly = true)
  public Validacion validar(List<FilaProducto> filas) {
    Referencias referencias = referencias();
    List<ErrorCarga> errores = new ArrayList<>();
    Map<String, DatosProductoInventario> nuevos = new LinkedHashMap<>();
    Set<String> existentes =
        productos
            .findByCodigoIn(
                filas.stream()
                    .filter(f -> f.codigo() != null && !f.codigo().isBlank())
                    .map(f -> Producto.normalizarCodigo(f.codigo()))
                    .toList())
            .stream()
            .map(Producto::getCodigo)
            .collect(Collectors.toSet());
    Set<String> vistos = new HashSet<>();
    Set<String> conErrores = new HashSet<>();
    for (FilaProducto fila : filas) {
      List<String> mensajes = mensajesDe(fila, referencias, existentes, vistos);
      if (mensajes.isEmpty()) {
        Producto producto = construir(fila, referencias, mensajes);
        if (producto != null) {
          nuevos.put(
              producto.getCodigo(),
              new DatosProductoInventario(
                  null,
                  producto.getCodigo(),
                  producto.getNombre(),
                  true,
                  producto.isControlaSerial(),
                  producto.getUnidadMedida().isAdmiteDecimales(),
                  producto.getUnidadMedida().getAbreviatura(),
                  producto.getCategoria().getId(),
                  producto.getCategoria().getNombre(),
                  producto.getMarca()));
        }
      }
      if (!mensajes.isEmpty() && fila.codigo() != null && !fila.codigo().isBlank()) {
        conErrores.add(Producto.normalizarCodigo(fila.codigo()));
      }
      mensajes.forEach(m -> errores.add(new ErrorCarga(HOJA, fila.fila(), m)));
    }
    return new Validacion(errores, nuevos, conErrores);
  }

  /** Crea los productos de la hoja (ya validada) y devuelve su id por código. */
  @Transactional(propagation = Propagation.MANDATORY)
  public Map<String, Long> crear(List<FilaProducto> filas) {
    Referencias referencias = referencias();
    Map<String, Long> ids = new HashMap<>();
    for (FilaProducto fila : filas) {
      List<String> mensajes = new ArrayList<>();
      Producto producto = construir(fila, referencias, mensajes);
      if (producto == null) {
        throw new IllegalStateException("La fila " + fila.fila() + " no estaba validada");
      }
      Producto guardado = productos.save(producto);
      ids.put(guardado.getCodigo(), guardado.getId());
    }
    productos.flush();
    return ids;
  }

  private List<String> mensajesDe(
      FilaProducto fila, Referencias referencias, Set<String> existentes, Set<String> vistos) {
    List<String> mensajes =
        validador.validate(fila).stream()
            .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toCollection(ArrayList::new));
    if (fila.codigo() != null && !fila.codigo().isBlank()) {
      String codigo = Producto.normalizarCodigo(fila.codigo());
      if (!vistos.add(codigo)) {
        mensajes.add("El código " + codigo + " está repetido en el archivo.");
      } else if (existentes.contains(codigo)) {
        mensajes.add("Ya existe un producto con el código " + codigo + ".");
      }
    }
    if (fila.categoria() != null
        && !fila.categoria().isBlank()
        && referencias.categoria(fila.categoria()) == null) {
      mensajes.add("La categoría " + fila.categoria().trim() + " no existe.");
    }
    if (fila.unidad() != null
        && !fila.unidad().isBlank()
        && referencias.unidad(fila.unidad()) == null) {
      mensajes.add("La unidad de medida " + fila.unidad().trim() + " no existe.");
    }
    return mensajes;
  }

  /** Construye el producto en memoria; las reglas del dominio que fallen quedan en mensajes. */
  private static Producto construir(
      FilaProducto fila, Referencias referencias, List<String> mensajes) {
    try {
      return Producto.crear(
          new DatosProducto(
              fila.codigo(),
              fila.nombre(),
              fila.marca(),
              fila.modelo(),
              fila.controlaSerial(),
              fila.precioInstalador(),
              fila.precioClienteFinal(),
              fila.monedaPrecio(),
              fila.stockMinimo(),
              fila.descripcion()),
          referencias.categoria(fila.categoria()),
          referencias.unidad(fila.unidad()));
    } catch (NegocioException e) {
      mensajes.add(e.getMessage());
      return null;
    }
  }

  private Referencias referencias() {
    return new Referencias(
        categorias.findAll().stream()
            .collect(Collectors.toMap(c -> clave(c.getNombre()), Function.identity())),
        unidades.findAll().stream()
            .collect(Collectors.toMap(u -> clave(u.getAbreviatura()), Function.identity())));
  }

  private static String clave(String texto) {
    return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
  }

  /** Categorías por nombre y unidades por abreviatura, sin distinguir mayúsculas. */
  private record Referencias(
      Map<String, Categoria> categorias, Map<String, UnidadMedida> unidades) {

    Categoria categoria(String nombre) {
      return categorias.get(clave(nombre));
    }

    UnidadMedida unidad(String abreviatura) {
      return unidades.get(clave(abreviatura));
    }
  }
}
