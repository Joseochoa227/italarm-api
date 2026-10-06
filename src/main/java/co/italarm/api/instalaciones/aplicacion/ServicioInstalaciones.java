package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.comercial.aplicacion.MaterialPreparado;
import co.italarm.api.comercial.aplicacion.OrigenCotizacion;
import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.comercial.aplicacion.VistaPreviaMaterial;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.EnlaceCreado;
import co.italarm.api.documentos.aplicacion.ServicioArchivos;
import co.italarm.api.documentos.aplicacion.ServicioEnlacesComprobante;
import co.italarm.api.instalaciones.dominio.FotoInstalacion;
import co.italarm.api.instalaciones.dominio.GrupoFoto;
import co.italarm.api.instalaciones.dominio.Instalacion;
import co.italarm.api.instalaciones.infraestructura.EspecificacionesInstalaciones;
import co.italarm.api.instalaciones.infraestructura.FotoInstalacionRepositorio;
import co.italarm.api.instalaciones.infraestructura.InstalacionRepositorio;
import co.italarm.api.instalaciones.infraestructura.TotalesInstalaciones;
import co.italarm.api.inventario.aplicacion.LineaAnulacion;
import co.italarm.api.inventario.aplicacion.SerialSalida;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.EstadoGarantia;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Garantia;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import co.italarm.api.usuarios.aplicacion.UsuarioReferencia;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Instalaciones (sección 3.13), sus fotos y su anulación (RF-72, RF-73). */
@Service
public class ServicioInstalaciones {

  private final InstalacionRepositorio instalaciones;
  private final FotoInstalacionRepositorio fotos;
  private final TotalesInstalaciones totales;
  private final RegistroInstalaciones registro;
  private final ServicioMovimientos movimientos;
  private final ConsultaProductos productos;
  private final VistaPreviaMaterial vistaPreviaMaterial;
  private final ServicioIdempotencia idempotencia;
  private final ServicioArchivos archivos;
  private final ConsultaUsuarios usuarios;
  private final ComprobantesInstalacion comprobantes;
  private final ServicioEnlacesComprobante enlaces;
  private final OrigenCotizacion origen;
  private final FechaNegocio fechas;

  public ServicioInstalaciones(
      InstalacionRepositorio instalaciones,
      FotoInstalacionRepositorio fotos,
      TotalesInstalaciones totales,
      RegistroInstalaciones registro,
      ServicioMovimientos movimientos,
      ConsultaProductos productos,
      VistaPreviaMaterial vistaPreviaMaterial,
      ServicioIdempotencia idempotencia,
      ServicioArchivos archivos,
      ConsultaUsuarios usuarios,
      ComprobantesInstalacion comprobantes,
      ServicioEnlacesComprobante enlaces,
      OrigenCotizacion origen,
      FechaNegocio fechas) {
    this.instalaciones = instalaciones;
    this.fotos = fotos;
    this.totales = totales;
    this.registro = registro;
    this.movimientos = movimientos;
    this.productos = productos;
    this.vistaPreviaMaterial = vistaPreviaMaterial;
    this.idempotencia = idempotencia;
    this.archivos = archivos;
    this.usuarios = usuarios;
    this.comprobantes = comprobantes;
    this.enlaces = enlaces;
    this.origen = origen;
    this.fechas = fechas;
  }

  /**
   * Precios, disponibilidad, costos, cobro con utilidad y vencimientos de garantía, sin guardar
   * nada (RF-108, RF-113, RF-119).
   */
  @Transactional(readOnly = true)
  public VistaPreviaInstalacionVista vistaPrevia(DatosInstalacion datos) {
    RegistroInstalaciones.Contexto contexto = registro.contexto(datos);
    Moneda moneda = datos.moneda();
    List<Long> ids = datos.lineas().stream().map(LineaMaterial::productoId).toList();
    Map<Long, ProductoValorizado> valorizados = productos.valorizadosPorId(ids);
    Map<Long, BigDecimal> costos = new HashMap<>();
    valorizados.forEach((id, p) -> costos.put(id, p.costoActualUsd()));
    List<MaterialPreparado> preparadas =
        PreparacionMaterial.lineas(
            datos.lineas(),
            valorizados,
            costos,
            contexto.cliente().precioInstalador(),
            moneda,
            contexto.conversion());
    ResumenDocumento resumen =
        RegistroInstalaciones.resumen(preparadas, datos, contexto.conversion());
    VistaPreviaMaterial.Resultado material =
        vistaPreviaMaterial.construir(preparadas, moneda, contexto.conversion());
    Instalacion.Garantias garantias = contexto.garantias();
    String direccion =
        datos.direccion() == null || datos.direccion().isBlank()
            ? contexto.cliente().direccion()
            : datos.direccion().trim();
    return new VistaPreviaInstalacionVista(
        contexto.fecha(),
        PreparacionMaterial.clienteVista(contexto.cliente()),
        direccion,
        moneda,
        PreparacionMaterial.tasasVista(contexto.tasas()),
        PreparacionMaterial.avisos(contexto.fecha(), contexto.tasas()),
        material.lineas(),
        PreparacionMaterial.resumenVista(resumen, moneda, contexto.conversion()),
        new GarantiasInstalacionVista(
            garantias.manoObraMeses(),
            Garantia.vencimiento(contexto.fecha(), garantias.manoObraMeses()),
            null,
            Garantia.vencimiento(contexto.fecha(), garantias.equiposMeses()),
            null,
            datos.condicionesGarantia() == null || datos.condicionesGarantia().isBlank()
                ? garantias.condiciones()
                : datos.condicionesGarantia().trim()),
        material.puedeGuardar());
  }

  /**
   * Registra la instalación una sola vez por {@code Idempotency-Key} (RT-07) y devuelve su id. La
   * transacción la abre {@link RegistroInstalaciones}.
   */
  public Long registrar(DatosInstalacion datos, Long usuarioId, ClaveIdempotencia clave) {
    return idempotencia.ejecutar(clave, () -> registro.registrar(datos, usuarioId, clave));
  }

  /** Listado del período (por defecto, el mes en curso) con totales sin las anuladas (RF-121). */
  @Transactional(readOnly = true)
  public ListadoInstalacionesVista listar(
      Long clienteId,
      Long tecnicoId,
      LocalDate desde,
      LocalDate hasta,
      EstadoGarantia estadoGarantia,
      Boolean incluirAnuladas,
      Pageable pagina) {
    LocalDate hoy = fechas.hoy();
    LocalDate inicio = desde != null ? desde : hoy.withDayOfMonth(1);
    LocalDate fin = hasta != null ? hasta : hoy.with(TemporalAdjusters.lastDayOfMonth());
    Page<Instalacion> encontradas =
        instalaciones.findAll(
            EspecificacionesInstalaciones.filtrar(
                clienteId,
                tecnicoId,
                inicio,
                fin,
                estadoGarantia,
                hoy,
                !Boolean.FALSE.equals(incluirAnuladas)),
            pagina);
    Set<Long> personas = new HashSet<>();
    encontradas
        .getContent()
        .forEach(
            i -> {
              personas.addAll(i.getTecnicos());
              if (i.getCreatedBy() != null) {
                personas.add(i.getCreatedBy());
              }
            });
    Map<Long, String> nombres = nombres(personas);
    Pagina<InstalacionResumenVista> contenido =
        Pagina.de(
            encontradas,
            i ->
                new InstalacionResumenVista(
                    i.getId(),
                    i.consecutivo(),
                    i.getFecha(),
                    i.getClienteId(),
                    i.getCliente().nombre(),
                    i.getDireccion(),
                    i.getTecnicos().stream()
                        .map(nombres::get)
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.joining(", ")),
                    i.getMoneda(),
                    new Dinero(i.getResumen().total(), i.getMoneda()),
                    usd(i.getResumen().totalUsd()),
                    new Dinero(i.getResumen().utilidad(), i.getMoneda()),
                    i.getVenceManoObra(),
                    i.estaAnulada() ? null : Garantia.estado(i.getVenceManoObra(), hoy),
                    i.getEstado().name(),
                    nombres.get(i.getCreatedBy()),
                    i.getCreatedAt()));
    List<TotalesInstalaciones.TotalPorMoneda> porMoneda =
        totales.porMoneda(
            EspecificacionesInstalaciones.filtrar(
                clienteId, tecnicoId, inicio, fin, estadoGarantia, hoy, false));
    return new ListadoInstalacionesVista(
        inicio,
        fin,
        contenido,
        porMoneda.stream()
            .map(
                t ->
                    new ListadoInstalacionesVista.TotalMoneda(
                        new Dinero(t.material(), t.moneda()),
                        new Dinero(t.manoDeObra(), t.moneda()),
                        new Dinero(t.total(), t.moneda()),
                        new Dinero(t.costo(), t.moneda()),
                        new Dinero(t.utilidad(), t.moneda()),
                        t.instalaciones()))
            .toList(),
        usd(sumar(porMoneda.stream().map(TotalesInstalaciones.TotalPorMoneda::totalUsd).toList())),
        usd(
            sumar(
                porMoneda.stream()
                    .map(TotalesInstalaciones.TotalPorMoneda::utilidadUsd)
                    .toList())));
  }

  @Transactional(readOnly = true)
  public InstalacionVista detalle(Long id) {
    Instalacion instalacion =
        instalaciones
            .findConLineasById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La instalación no existe."));
    LocalDate hoy = fechas.hoy();
    Map<Long, List<SerialSalida>> seriales = movimientos.serialesDeSalida(documento(instalacion));
    Set<Long> personas = new HashSet<>(instalacion.getTecnicos());
    if (instalacion.getCreatedBy() != null) {
      personas.add(instalacion.getCreatedBy());
    }
    if (instalacion.getAnuladaPor() != null) {
      personas.add(instalacion.getAnuladaPor());
    }
    Map<Long, String> nombres = nombres(personas);
    Moneda moneda = instalacion.getMoneda();
    Instalacion.TasasInstalacion tasas = instalacion.getTasas();
    Tasas conversion = new Tasas(tasas.trm(), tasas.tasaVes());
    ResumenDocumento resumen = instalacion.getResumen();
    List<FotoInstalacion> todas = fotos.findByInstalacionIdOrderById(id);
    return new InstalacionVista(
        instalacion.getId(),
        instalacion.consecutivo(),
        instalacion.getFecha(),
        PreparacionMaterial.clienteVista(instalacion.getCliente()),
        instalacion.getDireccion(),
        instalacion.getDescripcion(),
        instalacion.getTecnicos().stream()
            .map(t -> new UsuarioReferencia(t, nombres.get(t)))
            .sorted(Comparator.comparing(UsuarioReferencia::id))
            .toList(),
        moneda,
        new TasasDocumentoVista(
            tasas.trm(), tasas.fechaTrm(), tasas.tasaVes(), tasas.fechaTasaVes()),
        instalacion.getLineas().stream()
            .map(
                l ->
                    new InstalacionVista.Linea(
                        l.getProductoId(),
                        l.getCodigo(),
                        l.getDescripcion(),
                        l.getUnidad(),
                        PreparacionMaterial.cantidadVista(l.getCantidad()),
                        new Dinero(l.getPrecioUnitario(), moneda),
                        new Dinero(l.getPrecioSugerido(), moneda),
                        new Dinero(l.getSubtotal(), moneda),
                        usd(l.getCostoUnitarioUsd()),
                        seriales.getOrDefault(l.getProductoId(), List.of()).stream()
                            .map(
                                s ->
                                    new InstalacionVista.SerialInstalado(
                                        s.id(), s.numero(), s.vencimientoGarantia()))
                            .toList()))
            .toList(),
        instalacion.getDescuentoTipo().name(),
        instalacion.getDescuentoValor(),
        PreparacionMaterial.resumenVista(resumen, moneda, conversion),
        new Dinero(resumen.total(), moneda),
        new Dinero(resumen.utilidad(), moneda),
        resumen.porcentajeUtilidad(),
        new GarantiasInstalacionVista(
            instalacion.getGarantiaManoObraMeses(),
            instalacion.getVenceManoObra(),
            instalacion.estaAnulada() ? null : Garantia.estado(instalacion.getVenceManoObra(), hoy),
            instalacion.getVenceEquipos(),
            instalacion.estaAnulada() ? null : Garantia.estado(instalacion.getVenceEquipos(), hoy),
            instalacion.getCondicionesGarantia()),
        new InstalacionVista.Fotos(
            fotos(todas, GrupoFoto.ANTES),
            fotos(todas, GrupoFoto.DURANTE),
            fotos(todas, GrupoFoto.DESPUES)),
        instalacion.getObservaciones(),
        instalacion.getMonedasComprobante(),
        origen.origen(instalacion.getCotizacionId()).orElse(null),
        instalacion.getEstado().name(),
        instalacion.estaAnulada()
            ? new InstalacionVista.Anulacion(
                instalacion.getMotivoAnulacion(),
                nombres.get(instalacion.getAnuladaPor()),
                instalacion.getAnuladaEn())
            : null,
        nombres.get(instalacion.getCreatedBy()),
        instalacion.getCreatedAt(),
        instalacion.getVersion());
  }

  /** Corrige los datos descriptivos (RF-122, P-44). */
  @Transactional
  public InstalacionVista actualizar(Long id, Instalacion.Descripcion datos, long version) {
    Instalacion instalacion = buscar(id);
    instalacion.verificarVersion(version);
    if (datos.tecnicos() != null) {
      registro.validarTecnicos(List.copyOf(datos.tecnicos()));
    }
    instalacion.cambiarDescripcion(datos);
    instalaciones.flush();
    return detalle(id);
  }

  /**
   * Anula la instalación (RF-72, RF-73): el material vuelve al costo vigente y los seriales a
   * bodega. Las fotos se conservan.
   */
  @Transactional
  public InstalacionVista anular(Long id, String motivo, Long usuarioId) {
    Instalacion instalacion =
        instalaciones
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La instalación no existe."));
    if (!instalacion.estaAnulada() && !instalacion.getLineas().isEmpty()) {
      movimientos.anularSalida(
          documento(instalacion),
          fechas.hoy(),
          instalacion.getLineas().stream()
              .map(l -> new LineaAnulacion(l.getProductoId(), l.getCantidad()))
              .toList(),
          usuarioId);
    }
    if (!instalacion.estaAnulada() && instalacion.getCotizacionId() != null) {
      origen.revertir(instalacion.getCotizacionId(), instalacion.getId());
    }
    instalacion.anular(motivo, usuarioId, fechas.ahora());
    instalaciones.flush();
    return detalle(id);
  }

  /** Agrega una foto a un grupo (RF-110, RF-111, P-42), también después de guardada. */
  @Transactional
  public InstalacionVista agregarFoto(Long id, GrupoFoto grupo, byte[] contenido) {
    Instalacion instalacion = buscar(id);
    long enElGrupo = fotos.countByInstalacionIdAndGrupo(id, grupo);
    FotoInstalacion.agregar(id, grupo, "pendiente", enElGrupo);
    String clave =
        archivos.guardarImagen(
            "instalaciones/" + instalacion.getId(),
            grupo.name().toLowerCase(Locale.ROOT),
            contenido);
    fotos.saveAndFlush(FotoInstalacion.agregar(id, grupo, clave, enElGrupo));
    return detalle(id);
  }

  @Transactional
  public InstalacionVista quitarFoto(Long id, Long fotoId) {
    FotoInstalacion foto =
        fotos
            .findById(fotoId)
            .filter(f -> f.getInstalacionId().equals(id))
            .orElseThrow(() -> new RecursoNoEncontradoException("La foto no existe."));
    fotos.delete(foto);
    fotos.flush();
    archivos.eliminarAlConfirmar(foto.getClave());
    return detalle(id);
  }

  /** PDF del comprobante para descargar con sesión (RF-133). */
  public ArchivoGenerado comprobante(Long id) {
    return comprobantes.pdf(id);
  }

  /** Enlace público del comprobante con el mensaje y el enlace de WhatsApp (RF-134). */
  @Transactional
  public EnlaceComprobante crearEnlace(Long id, Long usuarioId) {
    Instalacion instalacion = buscar(id);
    EnlaceCreado enlace = enlaces.crear(TipoDocumento.INSTALACION, instalacion.getId(), usuarioId);
    String mensaje =
        "Hola "
            + instalacion.getCliente().nombre()
            + ", te compartimos el comprobante de la instalación "
            + instalacion.consecutivo()
            + " de ITALARM, con sus garantías: "
            + enlace.url();
    String telefono = instalacion.getCliente().telefono();
    String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
    return new EnlaceComprobante(
        enlace.url(),
        enlace.venceEn(),
        mensaje,
        digitos.isEmpty()
            ? null
            : "https://wa.me/"
                + digitos
                + "?text="
                + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
  }

  private Instalacion buscar(Long id) {
    return instalaciones
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La instalación no existe."));
  }

  private List<InstalacionVista.Foto> fotos(List<FotoInstalacion> todas, GrupoFoto grupo) {
    return todas.stream()
        .filter(f -> f.getGrupo() == grupo)
        .map(
            f ->
                new InstalacionVista.Foto(
                    f.getId(), archivos.urlDe(f.getClave()), f.getCreatedAt()))
        .toList();
  }

  private Map<Long, String> nombres(Collection<Long> ids) {
    return ids.isEmpty() ? Map.of() : usuarios.nombres(ids);
  }

  static DocumentoRef documento(Instalacion instalacion) {
    return new DocumentoRef(
        TipoDocumento.INSTALACION, instalacion.getId(), instalacion.consecutivo());
  }

  private static BigDecimal sumar(List<BigDecimal> valores) {
    return valores.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static Dinero usd(BigDecimal monto) {
    return monto == null ? null : new Dinero(monto, Moneda.USD);
  }
}
