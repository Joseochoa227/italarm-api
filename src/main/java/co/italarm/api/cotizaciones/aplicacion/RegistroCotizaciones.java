package co.italarm.api.cotizaciones.aplicacion;

import co.italarm.api.catalogo.aplicacion.ConsultaProductos;
import co.italarm.api.catalogo.aplicacion.ProductoValorizado;
import co.italarm.api.comercial.aplicacion.LineaMaterial;
import co.italarm.api.comercial.aplicacion.MaterialPreparado;
import co.italarm.api.comercial.aplicacion.PreparacionMaterial;
import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import co.italarm.api.cotizaciones.dominio.Cotizacion;
import co.italarm.api.cotizaciones.dominio.CotizacionInvalidaException;
import co.italarm.api.cotizaciones.dominio.LineaCotizacion;
import co.italarm.api.cotizaciones.dominio.ReglasCotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.cotizaciones.dominio.VersionCotizacion;
import co.italarm.api.cotizaciones.infraestructura.CotizacionRepositorio;
import co.italarm.api.cotizaciones.infraestructura.VersionCotizacionRepositorio;
import co.italarm.api.shared.aplicacion.ServicioIdempotencia;
import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.ClaveIdempotencia;
import co.italarm.api.shared.dominio.ClienteNoExisteException;
import co.italarm.api.shared.dominio.Descuento;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.ProductoInactivoException;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.dominio.ResumenDocumento;
import co.italarm.api.shared.dominio.Tasas;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.shared.infraestructura.GeneradorConsecutivos;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasAplicables;
import co.italarm.api.terceros.aplicacion.ClienteDocumento;
import co.italarm.api.terceros.aplicacion.ConsultaClientes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creación y edición de cotizaciones (RF-80 a RF-84, RF-88): precio sugerido según el tipo de
 * cliente, tasas de hoy (P-49), costo vigente de cada producto y utilidad estimada. No toca el
 * inventario (RN-11).
 */
@Service
public class RegistroCotizaciones {

  private final CotizacionRepositorio cotizaciones;
  private final VersionCotizacionRepositorio versiones;
  private final ConsultaProductos productos;
  private final ConsultaClientes clientes;
  private final ServicioTasas tasas;
  private final ServicioConfiguracion configuracion;
  private final ServicioIdempotencia idempotencia;
  private final GeneradorConsecutivos consecutivos;
  private final FechaNegocio fechas;

  public RegistroCotizaciones(
      CotizacionRepositorio cotizaciones,
      VersionCotizacionRepositorio versiones,
      ConsultaProductos productos,
      ConsultaClientes clientes,
      ServicioTasas tasas,
      ServicioConfiguracion configuracion,
      ServicioIdempotencia idempotencia,
      GeneradorConsecutivos consecutivos,
      FechaNegocio fechas) {
    this.cotizaciones = cotizaciones;
    this.versiones = versiones;
    this.productos = productos;
    this.clientes = clientes;
    this.tasas = tasas;
    this.configuracion = configuracion;
    this.idempotencia = idempotencia;
    this.consecutivos = consecutivos;
    this.fechas = fechas;
  }

  /** Lo que la vista previa, el registro y la edición comparten. */
  public record Preparacion(
      ClienteDocumento cliente,
      LocalDate fecha,
      int validezDias,
      TasasAplicables tasas,
      Tasas conversion,
      Descuento descuento,
      List<MaterialPreparado> lineas,
      ResumenDocumento resumen) {}

  @Transactional(readOnly = true)
  public Preparacion preparar(DatosCotizacion datos) {
    List<LineaMaterial> lineas = datos.lineas() == null ? List.of() : datos.lineas();
    if (PreparacionMaterial.hayRepetidos(lineas)) {
      throw new CotizacionInvalidaException(
          CotizacionInvalidaException.PRODUCTO_REPETIDO,
          "Un producto aparece dos veces en la cotización. Súmalo en una sola línea.");
    }
    BigDecimal manoDeObra = datos.manoDeObra() == null ? BigDecimal.ZERO : datos.manoDeObra();
    ReglasCotizacion.validarContenido(datos.tipo(), lineas.size(), manoDeObra, datos.descripcion());
    int validez =
        datos.validezDias() == null ? configuracion.validezCotizacionDias() : datos.validezDias();
    ReglasCotizacion.validarValidez(validez);
    ClienteDocumento cliente =
        clientes.porId(datos.clienteId()).orElseThrow(ClienteNoExisteException::new);
    Descuento descuento = Descuento.de(datos.descuentoTipo(), datos.descuentoValor());
    LocalDate hoy = fechas.hoy();
    TasasAplicables aplicables = tasas.tasasPara(hoy);
    Tasas conversion = PreparacionMaterial.conversion(aplicables);
    conversion.aUsd(BigDecimal.ONE, datos.moneda());

    List<Long> ids = lineas.stream().map(LineaMaterial::productoId).toList();
    Map<Long, ProductoValorizado> valorizados = productos.valorizadosPorId(ids);
    Map<Long, BigDecimal> costos = new HashMap<>();
    valorizados.forEach(
        (id, p) -> {
          if (!p.activo()) {
            throw new ProductoInactivoException(
                p.nombre() + " está inactivo y no se puede cotizar.");
          }
          costos.put(id, p.costoActualUsd());
        });
    List<MaterialPreparado> preparadas =
        PreparacionMaterial.lineas(
            lineas, valorizados, costos, cliente.precioInstalador(), datos.moneda(), conversion);
    ResumenDocumento resumen =
        CalculoDocumento.calcular(
            preparadas.stream().map(MaterialPreparado::calculo).toList(),
            datos.tipo() == TipoCotizacion.INSTALACION ? manoDeObra : BigDecimal.ZERO,
            descuento,
            datos.moneda(),
            conversion);
    return new Preparacion(
        cliente, hoy, validez, aplicables, conversion, descuento, preparadas, resumen);
  }

  /** Registra la cotización en Borrador y devuelve su id. */
  @Transactional
  public Long registrar(DatosCotizacion datos, ClaveIdempotencia clave) {
    Preparacion preparacion = preparar(datos);
    idempotencia.reservar(clave);
    Cotizacion cotizacion =
        cotizaciones.save(
            Cotizacion.registrar(
                consecutivos.siguiente(TipoDocumento.COTIZACION), contenido(datos, preparacion)));
    cotizaciones.flush();
    idempotencia.asociar(clave, cotizacion.getId());
    return cotizacion.getId();
  }

  /**
   * Edita la cotización (RF-88, P-46): en Borrador reemplaza su contenido; en En evaluación guarda
   * una nueva versión y conserva la anterior.
   */
  @Transactional
  public void editar(Long id, DatosCotizacion datos, long version) {
    Cotizacion cotizacion =
        cotizaciones
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
    cotizacion.verificarVersion(version);
    VersionCotizacion anterior = cotizacion.editar(contenido(datos, preparar(datos)));
    if (anterior != null) {
      versiones.save(anterior);
    }
    cotizaciones.flush();
  }

  private static Cotizacion.Contenido contenido(DatosCotizacion datos, Preparacion preparacion) {
    TasasAplicables aplicables = preparacion.tasas();
    return new Cotizacion.Contenido(
        datos.tipo(),
        preparacion.fecha(),
        preparacion.validezDias(),
        PreparacionMaterial.copia(preparacion.cliente()),
        datos.moneda(),
        new Cotizacion.TasasCotizacion(
            aplicables.trm(),
            aplicables.fechaTrm(),
            aplicables.tasaVes(),
            aplicables.fechaTasaVes()),
        datos.descripcion(),
        preparacion.descuento(),
        preparacion.resumen(),
        datos.observaciones(),
        datos.monedasComprobante(),
        preparacion.lineas().stream()
            .map(
                l ->
                    new LineaCotizacion(
                        l.producto().id(),
                        l.producto().codigo(),
                        l.producto().nombre(),
                        l.producto().abreviatura(),
                        l.cantidad(),
                        l.precioUnitario(),
                        l.precioSugerido(),
                        l.costoUnitarioUsd()))
            .toList());
  }
}
