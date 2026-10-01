package co.italarm.api.compras.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.DatosProductoInventario;
import co.italarm.api.compras.dominio.Compra;
import co.italarm.api.compras.dominio.LineaCompra;
import co.italarm.api.compras.dominio.ReglasCompra;
import co.italarm.api.compras.infraestructura.CompraRepositorio;
import co.italarm.api.compras.infraestructura.EspecificacionesCompras;
import co.italarm.api.compras.infraestructura.LineaCompraRepositorio;
import co.italarm.api.compras.infraestructura.TotalesCompras;
import co.italarm.api.documentos.aplicacion.ServicioArchivos;
import co.italarm.api.inventario.aplicacion.CambioCosto;
import co.italarm.api.inventario.aplicacion.LineaAnulacion;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ConsultaProveedores;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compras a proveedores (sección 3.6) y su anulación (RF-71, RF-73). */
@Service
public class ServicioCompras {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final int PRODUCTOS_EN_RESUMEN = 2;

  private final CompraRepositorio compras;
  private final LineaCompraRepositorio lineas;
  private final TotalesCompras totales;
  private final RegistroCompras registro;
  private final ServicioMovimientos movimientos;
  private final ServicioTasas tasas;
  private final ServicioIdempotencia idempotencia;
  private final ServicioArchivos archivos;
  private final ConsultaProductos productos;
  private final ConsultaProveedores proveedores;
  private final ConsultaUsuarios usuarios;
  private final FechaNegocio fechas;

  public ServicioCompras(
      CompraRepositorio compras,
      LineaCompraRepositorio lineas,
      TotalesCompras totales,
      RegistroCompras registro,
      ServicioMovimientos movimientos,
      ServicioTasas tasas,
      ServicioIdempotencia idempotencia,
      ServicioArchivos archivos,
      ConsultaProductos productos,
      ConsultaProveedores proveedores,
      ConsultaUsuarios usuarios,
      FechaNegocio fechas) {
    this.compras = compras;
    this.lineas = lineas;
    this.totales = totales;
    this.registro = registro;
    this.movimientos = movimientos;
    this.tasas = tasas;
    this.idempotencia = idempotencia;
    this.archivos = archivos;
    this.productos = productos;
    this.proveedores = proveedores;
    this.usuarios = usuarios;
    this.fechas = fechas;
  }

  /** Costo actual → nuevo y regla por línea, y subtotales en las tres monedas (RF-41, RF-42). */
  @Transactional(readOnly = true)
  public VistaPreviaCompraVista vistaPrevia(
      LocalDate fechaCompra, Moneda moneda, List<DatosCompra.Linea> lineasCompra) {
    LocalDate fecha = RegistroCompras.fechaDe(fechaCompra, fechas);
    ReglasCompra.validar(fecha, fechas.hoy(), RegistroCompras.lineasAValidar(lineasCompra));
    TasasAplicables aplicables = tasas.tasasPara(fecha);
    Tasas conversion = new Tasas(aplicables.trm(), aplicables.tasaVes());
    Map<Long, DatosProductoInventario> datos =
        productos.porId(lineasCompra.stream().map(DatosCompra.Linea::productoId).toList());

    List<VistaPreviaCompraVista.Linea> resultado = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    for (DatosCompra.Linea linea : lineasCompra) {
      DatosProductoInventario producto = datos.get(linea.productoId());
      BigDecimal costoUsd = conversion.aUsd(linea.costoUnitario(), moneda);
      CambioCosto cambio =
          movimientos.vistaPreviaCosto(linea.productoId(), linea.cantidad(), costoUsd);
      BigDecimal subtotal =
          Redondeo.paraAlmacenar(linea.cantidad().multiply(linea.costoUnitario()));
      total = total.add(subtotal);
      resultado.add(
          new VistaPreviaCompraVista.Linea(
              linea.productoId(),
              producto.codigo(),
              producto.nombre(),
              cantidad(linea.cantidad()),
              producto.abreviatura(),
              cantidad(cambio.stockActual()),
              new Dinero(Redondeo.paraAlmacenar(linea.costoUnitario()), moneda),
              usd(Redondeo.paraAlmacenar(costoUsd)),
              usd(cambio.costoAnterior()),
              usd(cambio.costoNuevo()),
              cambio.regla(),
              conversion.equivalentes(new Dinero(subtotal, moneda))));
    }
    return new VistaPreviaCompraVista(
        fecha,
        moneda,
        tasasVista(aplicables),
        avisos(fecha, aplicables),
        resultado,
        conversion.equivalentes(new Dinero(Redondeo.paraAlmacenar(total), moneda)));
  }

  /**
   * Registra la compra una sola vez por {@code Idempotency-Key} (RT-07) y devuelve su id. La
   * transacción la abre {@link RegistroCompras}.
   */
  public Long registrar(DatosCompra datos, Long usuarioId, ClaveIdempotencia clave) {
    return idempotencia.ejecutar(clave, () -> registro.registrar(datos, usuarioId, clave));
  }

  /**
   * Listado del período (por defecto, el mes en curso) con los totales sin las anuladas (RF-46,
   * RF-47).
   */
  @Transactional(readOnly = true)
  public ListadoComprasVista listar(
      Long proveedorId,
      Long productoId,
      LocalDate desde,
      LocalDate hasta,
      Boolean incluirAnuladas,
      Pageable pagina) {
    LocalDate hoy = fechas.hoy();
    LocalDate inicio = desde != null ? desde : hoy.withDayOfMonth(1);
    LocalDate fin = hasta != null ? hasta : hoy.with(TemporalAdjusters.lastDayOfMonth());
    Page<Compra> encontradas =
        compras.findAll(
            EspecificacionesCompras.filtrar(
                proveedorId, productoId, inicio, fin, !Boolean.FALSE.equals(incluirAnuladas)),
            pagina);

    List<Long> ids = encontradas.getContent().stream().map(Compra::getId).toList();
    Map<Long, List<LineaCompra>> lineasPorCompra =
        ids.isEmpty()
            ? Map.of()
            : lineas.deCompras(ids).stream()
                .collect(Collectors.groupingBy(LineaCompra::getCompraId));
    Map<Long, DatosProductoInventario> datos =
        productos.porId(
            lineasPorCompra.values().stream()
                .flatMap(List::stream)
                .map(LineaCompra::getProductoId)
                .collect(Collectors.toSet()));
    Map<Long, String> nombresProveedores =
        proveedores.nombres(
            encontradas.getContent().stream()
                .map(Compra::getProveedorId)
                .collect(Collectors.toSet()));
    Map<Long, String> nombresUsuarios =
        usuarios.nombres(
            encontradas.getContent().stream()
                .map(Compra::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

    Pagina<CompraResumenVista> contenido =
        Pagina.de(
            encontradas,
            compra ->
                new CompraResumenVista(
                    compra.getId(),
                    compra.consecutivo(),
                    compra.getFecha(),
                    new CompraVista.Referencia(
                        compra.getProveedorId(), nombresProveedores.get(compra.getProveedorId())),
                    compra.getNumeroFactura(),
                    compra.getMoneda(),
                    tasasVista(compra),
                    new Dinero(compra.getTotal(), compra.getMoneda()),
                    usd(compra.getTotalUsd()),
                    compra.getEstado().name(),
                    resumen(lineasPorCompra.getOrDefault(compra.getId(), List.of()), datos),
                    archivos.urlDe(compra.getFacturaClave()),
                    nombresUsuarios.get(compra.getCreatedBy()),
                    compra.getCreatedAt()));

    Specification<Compra> activas =
        EspecificacionesCompras.filtrar(proveedorId, productoId, inicio, fin, false);
    List<ListadoComprasVista.TotalMoneda> porMoneda =
        totales.porMoneda(activas).stream()
            .map(
                t ->
                    new ListadoComprasVista.TotalMoneda(
                        new Dinero(t.total(), t.moneda()), usd(t.totalUsd()), t.compras()))
            .toList();
    BigDecimal totalUsd =
        porMoneda.stream().map(t -> t.totalUsd().monto()).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ListadoComprasVista(inicio, fin, contenido, porMoneda, usd(totalUsd));
  }

  /** Detalle, con si se puede anular y por qué no (RF-48, P-23). */
  @Transactional(readOnly = true)
  public CompraVista detalle(Long id) {
    Compra compra =
        compras
            .findConLineasById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La compra no existe."));
    DocumentoRef documento = documento(compra);
    List<Long> productoIds = compra.getLineas().stream().map(LineaCompra::getProductoId).toList();
    Map<Long, DatosProductoInventario> datos = productos.porId(productoIds);
    Map<Long, List<String>> seriales = movimientos.serialesDeEntrada(documento);
    Optional<String> motivo =
        compra.estaAnulada()
            ? Optional.empty()
            : movimientos.motivoNoAnulable(documento, productoIds);
    Map<Long, String> nombresUsuarios =
        usuarios.nombres(idsNoNulos(compra.getCreatedBy(), compra.getAnuladaPor()));

    List<CompraVista.Linea> detalle =
        compra.getLineas().stream()
            .map(
                linea -> {
                  DatosProductoInventario producto = datos.get(linea.getProductoId());
                  return new CompraVista.Linea(
                      linea.getProductoId(),
                      producto.codigo(),
                      producto.nombre(),
                      cantidad(linea.getCantidad()),
                      producto.abreviatura(),
                      new Dinero(linea.getCostoUnitario(), compra.getMoneda()),
                      usd(Redondeo.paraAlmacenar(linea.getCostoUnitarioUsd())),
                      new Dinero(linea.getSubtotal(), compra.getMoneda()),
                      usd(linea.getCostoAnteriorUsd()),
                      usd(linea.getCostoNuevoUsd()),
                      linea.getRegla(),
                      seriales.getOrDefault(linea.getProductoId(), List.of()));
                })
            .toList();
    return new CompraVista(
        compra.getId(),
        compra.consecutivo(),
        compra.getFecha(),
        new CompraVista.Referencia(
            compra.getProveedorId(),
            proveedores.nombres(List.of(compra.getProveedorId())).get(compra.getProveedorId())),
        compra.getNumeroFactura(),
        compra.getMoneda(),
        tasasVista(compra),
        new Dinero(compra.getTotal(), compra.getMoneda()),
        usd(compra.getTotalUsd()),
        compra.getEstado().name(),
        detalle,
        archivos.urlDe(compra.getFacturaClave()),
        !compra.estaAnulada() && motivo.isEmpty(),
        motivo.orElse(null),
        compra.estaAnulada()
            ? new CompraVista.Anulacion(
                compra.getMotivoAnulacion(),
                nombresUsuarios.get(compra.getAnuladaPor()),
                compra.getAnuladaEn())
            : null,
        nombresUsuarios.get(compra.getCreatedBy()),
        compra.getCreatedAt());
  }

  /**
   * Anula la compra (RF-71, RF-73): descuenta el stock, devuelve el costo al anterior y deja sus
   * seriales anulados. Solo si es el último movimiento de cada producto (P-23).
   */
  @Transactional
  public CompraVista anular(Long id, String motivo, Long usuarioId) {
    Compra compra =
        compras
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La compra no existe."));
    if (!compra.estaAnulada()) {
      movimientos.anularEntradaCompra(
          documento(compra),
          fechas.hoy(),
          compra.getLineas().stream()
              .map(l -> new LineaAnulacion(l.getProductoId(), l.getCantidad()))
              .toList(),
          usuarioId);
    }
    compra.anular(motivo, usuarioId, fechas.ahora());
    compras.flush();
    return detalle(id);
  }

  /** Sube o reemplaza la factura: imagen o PDF, máximo 5 MB (RF-44, RF-70). */
  @Transactional
  public CompraVista cambiarFactura(Long id, byte[] contenido) {
    Compra compra = buscar(id);
    String clave = archivos.guardarImagenOPdf("compras/" + id, "factura", contenido);
    archivos.eliminarAlConfirmar(compra.cambiarFactura(clave));
    compras.flush();
    return detalle(id);
  }

  @Transactional
  public CompraVista quitarFactura(Long id) {
    Compra compra = buscar(id);
    archivos.eliminarAlConfirmar(compra.cambiarFactura(null));
    compras.flush();
    return detalle(id);
  }

  private Compra buscar(Long id) {
    return compras
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La compra no existe."));
  }

  private static DocumentoRef documento(Compra compra) {
    return new DocumentoRef(TipoDocumento.COMPRA, compra.getId(), compra.consecutivo());
  }

  /** Aviso si una tasa falta o no es la de la fecha de la compra (RF-33). */
  private static List<String> avisos(LocalDate fecha, TasasAplicables aplicables) {
    List<String> avisos = new ArrayList<>();
    if (aplicables.trm() == null) {
      avisos.add("No hay TRM registrada; la compra se guarda sin ella.");
    } else if (!aplicables.fechaTrm().equals(fecha)) {
      avisos.add(
          "La TRM usada es la del "
              + FORMATO_FECHA.format(aplicables.fechaTrm())
              + ", no la de la fecha de la compra.");
    }
    if (aplicables.tasaVes() == null) {
      avisos.add("No hay tasa del bolívar registrada; la compra se guarda sin ella.");
    } else if (!aplicables.fechaTasaVes().equals(fecha)) {
      avisos.add(
          "La tasa del bolívar usada es la del "
              + FORMATO_FECHA.format(aplicables.fechaTasaVes())
              + ", no la de la fecha de la compra.");
    }
    return avisos;
  }

  /** "Cámara IP × 3 und, Cable UTP × 100 m y 2 más". */
  private static String resumen(
      List<LineaCompra> lineasCompra, Map<Long, DatosProductoInventario> datos) {
    String primeros =
        lineasCompra.stream()
            .limit(PRODUCTOS_EN_RESUMEN)
            .map(
                linea -> {
                  DatosProductoInventario producto = datos.get(linea.getProductoId());
                  return producto.nombre()
                      + " × "
                      + cantidad(linea.getCantidad()).toPlainString()
                      + " "
                      + producto.abreviatura();
                })
            .collect(Collectors.joining(", "));
    int resto = lineasCompra.size() - PRODUCTOS_EN_RESUMEN;
    return resto > 0 ? primeros + " y " + resto + " más" : primeros;
  }

  private static TasasCompraVista tasasVista(TasasAplicables aplicables) {
    return new TasasCompraVista(
        aplicables.trm(), aplicables.fechaTrm(), aplicables.tasaVes(), aplicables.fechaTasaVes());
  }

  private static TasasCompraVista tasasVista(Compra compra) {
    return new TasasCompraVista(
        compra.getTrm(), compra.getFechaTrm(), compra.getTasaVes(), compra.getFechaTasaVes());
  }

  private static Dinero usd(BigDecimal monto) {
    return monto == null ? null : new Dinero(monto, Moneda.USD);
  }

  /** Cantidad sin ceros sobrantes: 12.500 → 12.5; 3.000 → 3. */
  static BigDecimal cantidad(BigDecimal valor) {
    if (valor == null) {
      return null;
    }
    BigDecimal sinCeros = valor.stripTrailingZeros();
    return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
  }

  private static Collection<Long> idsNoNulos(Long... ids) {
    Set<Long> resultado = new HashSet<>();
    for (Long id : ids) {
      if (id != null) {
        resultado.add(id);
      }
    }
    return resultado;
  }
}
