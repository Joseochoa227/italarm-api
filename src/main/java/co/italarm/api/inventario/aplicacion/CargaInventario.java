package co.italarm.api.inventario.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.DatosProductoInventario;
import co.italarm.api.inventario.dominio.HistorialCosto;
import co.italarm.api.inventario.dominio.InventarioInicial;
import co.italarm.api.inventario.dominio.LineaInventarioInicial;
import co.italarm.api.inventario.dominio.MovimientoInventario;
import co.italarm.api.inventario.dominio.ProductoInventario;
import co.italarm.api.inventario.dominio.ReglaCosto;
import co.italarm.api.inventario.dominio.Serial;
import co.italarm.api.inventario.dominio.TipoMovimiento;
import co.italarm.api.inventario.infraestructura.HistorialCostoRepositorio;
import co.italarm.api.inventario.infraestructura.InventarioInicialRepositorio;
import co.italarm.api.inventario.infraestructura.MovimientoInventarioRepositorio;
import co.italarm.api.inventario.infraestructura.SerialRepositorio;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.ErrorCarga;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.shared.infraestructura.GeneradorConsecutivos;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hoja Inventario inicial de la carga desde Excel (RF-150 a RF-152): valida sin guardar y crea el
 * documento II-00N con un movimiento de entrada por producto. El costo cargado es el costo inicial
 * (sin regla de costo) y los seriales quedan en bodega.
 */
@Service
public class CargaInventario {

  public static final String HOJA = "Inventario inicial";

  private final InventarioInicialRepositorio documentos;
  private final MovimientoInventarioRepositorio kardex;
  private final HistorialCostoRepositorio historial;
  private final SerialRepositorio seriales;
  private final OperacionesInventario operaciones;
  private final ConsultaProductos productos;
  private final ConsultaUsuarios usuarios;
  private final GeneradorConsecutivos consecutivos;
  private final FechaNegocio fechas;

  public CargaInventario(
      InventarioInicialRepositorio documentos,
      MovimientoInventarioRepositorio kardex,
      HistorialCostoRepositorio historial,
      SerialRepositorio seriales,
      OperacionesInventario operaciones,
      ConsultaProductos productos,
      ConsultaUsuarios usuarios,
      GeneradorConsecutivos consecutivos,
      FechaNegocio fechas) {
    this.documentos = documentos;
    this.kardex = kardex;
    this.historial = historial;
    this.seriales = seriales;
    this.operaciones = operaciones;
    this.productos = productos;
    this.usuarios = usuarios;
    this.consecutivos = consecutivos;
    this.fechas = fechas;
  }

  /**
   * Errores de la hoja. Un producto puede venir de la hoja Productos ({@code nuevos}) o existir ya,
   * y en ese caso no puede tener movimientos (RF-152). Los productos de {@code codigosConErrores}
   * tienen errores en la hoja Productos.
   */
  @Transactional(readOnly = true)
  public List<ErrorCarga> validar(
      List<FilaInventario> filas,
      Map<String, DatosProductoInventario> nuevos,
      Set<String> codigosConErrores) {
    Map<String, DatosProductoInventario> existentes =
        productos.porCodigo(
            filas.stream()
                .map(FilaInventario::codigo)
                .filter(c -> c != null && !c.isBlank())
                .toList());
    List<ErrorCarga> errores = new ArrayList<>();
    Set<String> vistos = new HashSet<>();
    for (FilaInventario fila : filas) {
      List<String> mensajes = new ArrayList<>();
      DatosProductoInventario producto =
          producto(fila, nuevos, codigosConErrores, existentes, vistos, mensajes);
      if (fila.cantidad() == null || fila.cantidad().signum() <= 0) {
        mensajes.add("La cantidad debe ser mayor que 0.");
      }
      if (fila.costoUnitarioUsd() == null || fila.costoUnitarioUsd().signum() <= 0) {
        mensajes.add("El costo unitario en USD debe ser mayor que 0.");
      } else if (fila.costoUnitarioUsd().stripTrailingZeros().scale() > 4) {
        mensajes.add("El costo unitario admite máximo 4 decimales.");
      }
      if (producto != null && mensajes.isEmpty()) {
        validarProducto(fila, producto, mensajes);
      }
      mensajes.forEach(m -> errores.add(new ErrorCarga(HOJA, fila.fila(), m)));
    }
    return errores;
  }

  /**
   * Crea el documento de inventario inicial y suma al inventario las filas ya validadas. Devuelve
   * el id del documento.
   *
   * @param ids id de cada producto por código (los existentes y los recién creados)
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Long registrar(
      List<FilaInventario> filas,
      Map<String, Long> ids,
      String archivo,
      int productosCreados,
      int clientesCreados,
      int proveedoresCreados,
      Long usuarioId) {
    LocalDate fecha = fechas.hoy();
    Instant ahora = fechas.ahora();
    InventarioInicial documento =
        InventarioInicial.crear(
            consecutivos.siguiente(TipoDocumento.INVENTARIO_INICIAL),
            fecha,
            archivo,
            productosCreados,
            clientesCreados,
            proveedoresCreados);
    for (FilaInventario fila : filas) {
      documento.agregarLinea(
          new LineaInventarioInicial(
              ids.get(codigo(fila.codigo())),
              fila.cantidad(),
              Redondeo.paraAlmacenar(fila.costoUnitarioUsd())));
    }
    documentos.save(documento);
    DocumentoRef referencia = documento.documento();

    Map<Long, DatosProductoInventario> datos = productos.porId(ids.values());
    Map<Long, ProductoInventario> bloqueados =
        operaciones.bloquear(
            filas.stream().map(f -> ids.get(codigo(f.codigo()))).collect(Collectors.toSet()));
    for (FilaInventario fila : filas) {
      Long productoId = ids.get(codigo(fila.codigo()));
      DatosProductoInventario producto = datos.get(productoId);
      ProductoInventario inventario = bloqueados.get(productoId);
      BigDecimal costoAnterior = inventario.getCostoActualUsd();
      inventario.entrar(fila.cantidad());
      inventario.cambiarCosto(fila.costoUnitarioUsd());
      kardex.save(
          MovimientoInventario.entrada(
              productoId,
              TipoMovimiento.INVENTARIO_INICIAL,
              referencia,
              fecha,
              fila.cantidad(),
              inventario.getStock(),
              inventario.getCostoActualUsd(),
              null,
              usuarioId,
              ahora));
      historial.save(
          HistorialCosto.sinFactura(
              productoId,
              referencia,
              fecha,
              costoAnterior,
              inventario.getCostoActualUsd(),
              ReglaCosto.INVENTARIO_INICIAL,
              usuarioId,
              ahora));
      List<String> numeros =
          operaciones.validarSerialesNuevos(producto, fila.cantidad(), fila.seriales());
      operaciones.registrarSeriales(
          productoId, numeros, referencia, fecha, "Inventario inicial", usuarioId, ahora);
    }
    documentos.flush();
    return documento.getId();
  }

  /** Cargas realizadas, de la más reciente a la más antigua (RF-152). */
  @Transactional(readOnly = true)
  public List<CargaInicialVista> listar() {
    List<InventarioInicial> cargas = documentos.findAllByOrderByIdDesc();
    Map<Long, String> nombres =
        usuarios.nombres(
            cargas.stream()
                .map(InventarioInicial::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    return cargas.stream()
        .map(
            c ->
                new CargaInicialVista(
                    c.getId(),
                    c.documento().consecutivo(),
                    c.getFecha(),
                    c.getArchivoNombre(),
                    c.getProductosCreados(),
                    c.getClientesCreados(),
                    c.getProveedoresCreados(),
                    c.getLineas().size(),
                    new Dinero(
                        Redondeo.paraAlmacenar(
                            c.getLineas().stream()
                                .map(l -> l.getCantidad().multiply(l.getCostoUnitarioUsd()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)),
                        Moneda.USD),
                    nombres.get(c.getCreatedBy()),
                    c.getCreatedAt()))
        .toList();
  }

  private DatosProductoInventario producto(
      FilaInventario fila,
      Map<String, DatosProductoInventario> nuevos,
      Set<String> codigosConErrores,
      Map<String, DatosProductoInventario> existentes,
      Set<String> vistos,
      List<String> mensajes) {
    if (fila.codigo() == null || fila.codigo().isBlank()) {
      mensajes.add("Falta el código del producto.");
      return null;
    }
    String codigo = codigo(fila.codigo());
    if (!vistos.add(codigo)) {
      mensajes.add("El producto " + codigo + " está repetido en la hoja.");
      return null;
    }
    if (codigosConErrores.contains(codigo)) {
      mensajes.add("Corrige primero el producto " + codigo + " en la hoja Productos.");
      return null;
    }
    DatosProductoInventario producto = nuevos.get(codigo);
    if (producto == null) {
      producto = existentes.get(codigo);
      if (producto == null) {
        mensajes.add("El producto " + codigo + " no existe ni está en la hoja Productos.");
      } else if (kardex.existsByProductoId(producto.id())) {
        mensajes.add(
            "El producto "
                + codigo
                + " ya tiene movimientos; su inventario inicial no se puede cargar.");
        return null;
      }
    }
    return producto;
  }

  /** Cantidad según la unidad y seriales según la cantidad (RF-20, P-22). */
  private void validarProducto(
      FilaInventario fila, DatosProductoInventario producto, List<String> mensajes) {
    try {
      OperacionesInventario.exigirCantidad(fila.cantidad(), producto);
      if (!producto.controlaSerial()) {
        if (fila.seriales() != null && !fila.seriales().isEmpty()) {
          mensajes.add(producto.codigo() + " no controla serial; deja vacía la columna Seriales.");
        }
        return;
      }
      List<String> numeros =
          Serial.validarLista(fila.seriales(), fila.cantidad(), producto.codigo());
      if (producto.id() != null) {
        List<String> repetidos = seriales.existentes(producto.id(), numeros);
        if (!repetidos.isEmpty()) {
          mensajes.add("El serial " + repetidos.get(0) + " ya está registrado.");
        }
      }
    } catch (NegocioException e) {
      mensajes.add(e.getMessage());
    }
  }

  private static String codigo(String codigo) {
    return codigo.trim().toUpperCase(Locale.ROOT);
  }

  /** Productos por código: los ids de los existentes, para unirlos a los recién creados. */
  @Transactional(readOnly = true)
  public Map<String, Long> idsExistentes(List<FilaInventario> filas) {
    Map<String, Long> ids = new HashMap<>();
    productos
        .porCodigo(filas.stream().map(FilaInventario::codigo).toList())
        .forEach((codigo, datos) -> ids.put(codigo, datos.id()));
    return ids;
  }
}
