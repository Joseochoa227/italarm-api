package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.comercial.aplicacion.MaterialPreparado;
import co.italarm.api.comercial.aplicacion.OrigenCotizacion;
import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.configuracion.aplicacion.EmpresaDocumentos;
import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import co.italarm.api.instalaciones.dominio.Instalacion;
import co.italarm.api.instalaciones.dominio.InstalacionInvalidaException;
import co.italarm.api.instalaciones.dominio.LineaInstalacion;
import co.italarm.api.instalaciones.dominio.ReglasInstalacion;
import co.italarm.api.instalaciones.infraestructura.InstalacionRepositorio;
import co.italarm.api.inventario.aplicacion.ExistenciaProducto;
import co.italarm.api.inventario.aplicacion.LineaSalida;
import co.italarm.api.inventario.aplicacion.ServicioMovimientos;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.ClienteNoExisteException;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.DocumentoRef;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.shared.infraestructura.GeneradorConsecutivos;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ClienteDocumento;
import co.italarm.api.terceros.aplicacion.ConsultaClientes;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import co.italarm.api.usuarios.aplicacion.UsuarioReferencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de una instalación en una sola transacción (RF-109): bloquea los productos, toma su
 * costo vigente (RF-68), guarda la instalación con las tasas de su fecha (RN-04, P-38) y descuenta
 * el material con los seriales y su garantía (RF-114).
 */
@Service
public class RegistroInstalaciones {

  private final InstalacionRepositorio instalaciones;
  private final ServicioMovimientos movimientos;
  private final ConsultaProductos productos;
  private final ConsultaClientes clientes;
  private final ConsultaUsuarios usuarios;
  private final ServicioTasas tasas;
  private final ServicioConfiguracion configuracion;
  private final ServicioIdempotencia idempotencia;
  private final GeneradorConsecutivos consecutivos;
  private final OrigenCotizacion origen;
  private final FechaNegocio fechas;

  public RegistroInstalaciones(
      InstalacionRepositorio instalaciones,
      ServicioMovimientos movimientos,
      ConsultaProductos productos,
      ConsultaClientes clientes,
      ConsultaUsuarios usuarios,
      ServicioTasas tasas,
      ServicioConfiguracion configuracion,
      ServicioIdempotencia idempotencia,
      GeneradorConsecutivos consecutivos,
      OrigenCotizacion origen,
      FechaNegocio fechas) {
    this.instalaciones = instalaciones;
    this.movimientos = movimientos;
    this.productos = productos;
    this.clientes = clientes;
    this.usuarios = usuarios;
    this.tasas = tasas;
    this.configuracion = configuracion;
    this.idempotencia = idempotencia;
    this.consecutivos = consecutivos;
    this.origen = origen;
    this.fechas = fechas;
  }

  /** Registra la instalación y devuelve su id. */
  @Transactional
  public Long registrar(DatosInstalacion datos, Long usuarioId, ClaveIdempotencia clave) {
    Contexto contexto = contexto(datos);
    idempotencia.reservar(clave);
    if (datos.cotizacionId() != null) {
      origen.validarConversion(
          datos.cotizacionId(), TipoDocumento.INSTALACION, contexto.cliente().id());
    }

    List<Long> ids = datos.lineas().stream().map(LineaMaterial::productoId).toList();
    Map<Long, ExistenciaProducto> existencias = movimientos.bloquearParaSalida(ids);
    Map<Long, ProductoValorizado> valorizados = productos.valorizadosPorId(ids);
    Map<Long, BigDecimal> costos = new HashMap<>();
    existencias.forEach((id, existencia) -> costos.put(id, existencia.costoActualUsd()));
    List<MaterialPreparado> lineas =
        PreparacionMaterial.lineas(
            datos.lineas(),
            valorizados,
            costos,
            contexto.cliente().precioInstalador(),
            datos.moneda(),
            contexto.conversion());
    ResumenDocumento resumen = resumen(lineas, datos, contexto.conversion());

    Instalacion nueva =
        Instalacion.registrar(
            consecutivos.siguiente(TipoDocumento.INSTALACION),
            contexto.fecha(),
            PreparacionMaterial.copia(contexto.cliente()),
            descripcion(datos),
            datos.moneda(),
            new Instalacion.TasasInstalacion(
                contexto.tasas().trm(),
                contexto.tasas().fechaTrm(),
                contexto.tasas().tasaVes(),
                contexto.tasas().fechaTasaVes()),
            Descuento.de(datos.descuentoTipo(), datos.descuentoValor()),
            resumen,
            contexto.garantias(),
            lineas.stream()
                .map(
                    l ->
                        new LineaInstalacion(
                            l.producto().id(),
                            l.producto().codigo(),
                            l.producto().nombre(),
                            l.producto().abreviatura(),
                            l.cantidad(),
                            l.precioUnitario(),
                            l.precioSugerido(),
                            l.costoUnitarioUsd()))
                .toList());
    nueva.desdeCotizacion(datos.cotizacionId());
    Instalacion instalacion = instalaciones.save(nueva);
    DocumentoRef documento =
        new DocumentoRef(TipoDocumento.INSTALACION, instalacion.getId(), instalacion.consecutivo());
    if (!lineas.isEmpty()) {
      movimientos.registrarSalida(
          documento,
          contexto.fecha(),
          lineas.stream()
              .map(l -> new LineaSalida(l.producto().id(), l.cantidad(), l.seriales()))
              .toList(),
          instalacion.getVenceEquipos(),
          "Cliente: " + contexto.cliente().nombre() + " · " + instalacion.consecutivo(),
          usuarioId);
    }
    if (datos.cotizacionId() != null) {
      origen.convertir(datos.cotizacionId(), documento, contexto.cliente().id());
    }
    instalaciones.flush();
    idempotencia.asociar(clave, instalacion.getId());
    return instalacion.getId();
  }

  /** Lo que la vista previa y el registro comparten: cliente, fecha, tasas y garantías. */
  public record Contexto(
      ClienteDocumento cliente,
      LocalDate fecha,
      TasasAplicables tasas,
      Tasas conversion,
      Instalacion.Garantias garantias) {}

  public Contexto contexto(DatosInstalacion datos) {
    validarLineas(datos.lineas());
    ReglasInstalacion.validarContenido(datos.lineas().size(), datos.manoDeObra());
    ReglasInstalacion.validarTecnicos(datos.tecnicos());
    validarTecnicos(datos.tecnicos());
    ClienteDocumento cliente =
        clientes.porId(datos.clienteId()).orElseThrow(ClienteNoExisteException::new);
    LocalDate hoy = fechas.hoy();
    LocalDate fecha = datos.fecha() == null ? hoy : datos.fecha();
    ReglasInstalacion.validarFecha(fecha, hoy);
    EmpresaDocumentos empresa = configuracion.empresaParaDocumentos();
    int meses =
        datos.garantiaManoObraMeses() == null
            ? empresa.garantiaManoObraMeses()
            : datos.garantiaManoObraMeses();
    ReglasInstalacion.validarMesesGarantia(meses);
    TasasAplicables aplicables = tasas.tasasPara(fecha);
    Tasas conversion = PreparacionMaterial.conversion(aplicables);
    conversion.aUsd(BigDecimal.ONE, datos.moneda());
    return new Contexto(
        cliente,
        fecha,
        aplicables,
        conversion,
        new Instalacion.Garantias(
            meses, empresa.garantiaEquiposMeses(), empresa.condicionesGarantia()));
  }

  static ResumenDocumento resumen(
      List<MaterialPreparado> lineas, DatosInstalacion datos, Tasas conversion) {
    return CalculoDocumento.calcular(
        lineas.stream().map(MaterialPreparado::calculo).toList(),
        datos.manoDeObra() == null ? BigDecimal.ZERO : datos.manoDeObra(),
        Descuento.de(datos.descuentoTipo(), datos.descuentoValor()),
        datos.moneda(),
        conversion);
  }

  static Instalacion.Descripcion descripcion(DatosInstalacion datos) {
    return new Instalacion.Descripcion(
        datos.direccion(),
        datos.descripcion(),
        datos.tecnicos(),
        datos.condicionesGarantia(),
        datos.observaciones(),
        datos.monedasComprobante());
  }

  /** Una línea por producto. */
  static void validarLineas(List<LineaMaterial> lineas) {
    if (PreparacionMaterial.hayRepetidos(lineas)) {
      throw new InstalacionInvalidaException(
          InstalacionInvalidaException.PRODUCTO_REPETIDO,
          "Un producto aparece dos veces en la instalación. Súmalo en una sola línea.");
    }
  }

  /** P-37: los técnicos son usuarios activos. */
  public void validarTecnicos(List<Long> tecnicos) {
    Set<Long> activos =
        usuarios.activos().stream().map(UsuarioReferencia::id).collect(Collectors.toSet());
    if (!activos.containsAll(tecnicos)) {
      throw new InstalacionInvalidaException(
          "TECNICO_NO_EXISTE", "Uno de los técnicos no existe o está inactivo.");
    }
  }
}
