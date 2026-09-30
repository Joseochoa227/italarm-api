package co.italarm.api.tasas.aplicacion;

import co.italarm.api.configuracion.aplicacion.ServicioConfiguracion;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.tasas.dominio.AvisoTasa;
import co.italarm.api.tasas.dominio.ConfirmacionTasa;
import co.italarm.api.tasas.dominio.CorreccionTasa;
import co.italarm.api.tasas.dominio.FuenteTasa;
import co.italarm.api.tasas.dominio.ParMoneda;
import co.italarm.api.tasas.dominio.ResultadoEjecucion;
import co.italarm.api.tasas.dominio.TasaCambio;
import co.italarm.api.tasas.dominio.TasaSinCambioException;
import co.italarm.api.tasas.dominio.TasaYaRegistradaException;
import co.italarm.api.tasas.dominio.TrmAutomaticaDisponibleException;
import co.italarm.api.tasas.dominio.VariacionTasa;
import co.italarm.api.tasas.infraestructura.CorreccionTasaRepositorio;
import co.italarm.api.tasas.infraestructura.EjecucionTareaTrmRepositorio;
import co.italarm.api.tasas.infraestructura.TasaCambioRepositorio;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tasas de cambio: vigentes, historial, registro manual y corrección (sección 3.5). */
@Service
public class ServicioTasas {

  private final TasaCambioRepositorio tasas;
  private final CorreccionTasaRepositorio correcciones;
  private final EjecucionTareaTrmRepositorio ejecuciones;
  private final ServicioConfiguracion configuracion;
  private final ConsultaUsuarios usuarios;
  private final FechaNegocio fechas;

  public ServicioTasas(
      TasaCambioRepositorio tasas,
      CorreccionTasaRepositorio correcciones,
      EjecucionTareaTrmRepositorio ejecuciones,
      ServicioConfiguracion configuracion,
      ConsultaUsuarios usuarios,
      FechaNegocio fechas) {
    this.tasas = tasas;
    this.correcciones = correcciones;
    this.ejecuciones = ejecuciones;
    this.configuracion = configuracion;
    this.usuarios = usuarios;
    this.fechas = fechas;
  }

  /** Tasas del día, o las últimas disponibles con su aviso (RF-30, RF-33). */
  @Transactional(readOnly = true)
  public TasasVigentesVista vigentes() {
    LocalDate hoy = fechas.hoy();
    Optional<TasaCambio> trm =
        tasas.findFirstByParAndFechaLessThanEqualOrderByFechaDesc(ParMoneda.USD_COP, hoy);
    Optional<TasaCambio> bolivar =
        tasas.findFirstByParAndFechaLessThanEqualOrderByFechaDesc(ParMoneda.USD_VES, hoy);
    boolean trmOficialDeHoy =
        trm.filter(t -> t.getFecha().equals(hoy) && t.getFuente() == FuenteTasa.SUPERFINANCIERA)
            .isPresent();
    boolean fallo =
        !trmOficialDeHoy
            && ejecuciones.existsByFechaObjetivoAndResultado(hoy, ResultadoEjecucion.FALLO);
    Map<Long, String> nombres =
        usuarios.nombres(
            idsDe(
                trm.map(TasaCambio::getRegistradaPor).orElse(null),
                bolivar.map(TasaCambio::getRegistradaPor).orElse(null)));
    return new TasasVigentesVista(
        hoy,
        vigente(ParMoneda.USD_COP, trm, hoy, fallo, nombres),
        vigente(ParMoneda.USD_VES, bolivar, hoy, false, nombres),
        fallo);
  }

  /** Historial de tasas por par y rango de fechas (RF-34). */
  @Transactional(readOnly = true)
  public Page<TasaVista> historial(
      ParMoneda par, LocalDate desde, LocalDate hasta, Pageable pagina) {
    Page<TasaCambio> resultado = tasas.buscar(par, desde, hasta, pagina);
    Map<Long, String> nombres =
        usuarios.nombres(
            resultado.getContent().stream()
                .map(TasaCambio::getRegistradaPor)
                .filter(Objects::nonNull)
                .toList());
    return resultado.map(t -> vista(t, nombres, List.of()));
  }

  @Transactional(readOnly = true)
  public TasaVista detalle(Long id) {
    TasaCambio tasa = buscar(id);
    List<CorreccionTasa> lista = correcciones.findByTasaIdOrderByCorregidaEnAsc(id);
    Set<Long> ids = new HashSet<>();
    if (tasa.getRegistradaPor() != null) {
      ids.add(tasa.getRegistradaPor());
    }
    lista.stream().map(CorreccionTasa::getCorregidaPor).filter(Objects::nonNull).forEach(ids::add);
    Map<Long, String> nombres = usuarios.nombres(ids);
    return vista(
        tasa,
        nombres,
        lista.stream()
            .map(
                c ->
                    new TasaVista.CorreccionVista(
                        c.getValorAnterior(),
                        c.getValorNuevo(),
                        c.getMotivo(),
                        c.isAutomatica(),
                        c.getCorregidaPor() == null ? null : nombres.get(c.getCorregidaPor()),
                        c.getCorregidaEn()))
            .toList());
  }

  /**
   * Vista previa antes de guardar (RF-35b). Para un registro nuevo compara con la última tasa
   * anterior a hoy; para una corrección ({@code tasaId}), con el valor actual de esa tasa.
   */
  @Transactional(readOnly = true)
  public VariacionVista vistaPrevia(ParMoneda par, BigDecimal valor, Long tasaId) {
    BigDecimal anterior =
        tasaId != null
            ? buscar(tasaId).getValor()
            : tasas
                .findFirstByParAndFechaLessThanOrderByFechaDesc(par, fechas.hoy())
                .map(TasaCambio::getValor)
                .orElse(null);
    return vista(VariacionTasa.calcular(anterior, valor, configuracion.limiteVariacionTasa()));
  }

  /**
   * Registro manual de la tasa de hoy con doble confirmación y alerta de variación (RF-29, RF-33,
   * RF-35). La TRM manual solo se admite si hoy no hay TRM oficial.
   */
  @Transactional
  public TasaVista registrarManual(
      ParMoneda par,
      BigDecimal valor,
      BigDecimal confirmacion,
      boolean aceptarVariacion,
      UsuarioAutenticado usuario) {
    BigDecimal confirmada = ConfirmacionTasa.validar(valor, confirmacion);
    LocalDate hoy = fechas.hoy();
    tasas
        .findByParAndFecha(par, hoy)
        .ifPresent(
            existente -> {
              if (existente.getFuente() == FuenteTasa.SUPERFINANCIERA) {
                throw new TrmAutomaticaDisponibleException(
                    "Ya está la TRM oficial de hoy; no hace falta registrarla a mano.");
              }
              throw new TasaYaRegistradaException(
                  "La " + par.nombre() + " de hoy ya fue registrada. Si está errada, corrígela.");
            });
    BigDecimal anterior =
        tasas
            .findFirstByParAndFechaLessThanOrderByFechaDesc(par, hoy)
            .map(TasaCambio::getValor)
            .orElse(null);
    VariacionTasa.calcular(anterior, confirmada, configuracion.limiteVariacionTasa())
        .exigirAceptacion(aceptarVariacion);
    TasaCambio tasa =
        tasas.saveAndFlush(
            TasaCambio.registrarManual(par, hoy, confirmada, usuario.usuarioId(), fechas.ahora()));
    return vista(tasa, Map.of(usuario.usuarioId(), usuario.nombre()), List.of());
  }

  /**
   * Corrige una tasa de cualquier fecha (RF-36, P-14), con doble confirmación y alerta de
   * variación. Lo ya registrado conserva la tasa con que se guardó (RN-04).
   */
  @Transactional
  public TasaVista corregir(
      Long id,
      BigDecimal valor,
      BigDecimal confirmacion,
      boolean aceptarVariacion,
      String motivo,
      UsuarioAutenticado usuario) {
    BigDecimal confirmada = ConfirmacionTasa.validar(valor, confirmacion);
    TasaCambio tasa = buscar(id);
    if (tasa.getValor().compareTo(confirmada) == 0) {
      throw new TasaSinCambioException("El nuevo valor es igual al actual.");
    }
    VariacionTasa.calcular(tasa.getValor(), confirmada, configuracion.limiteVariacionTasa())
        .exigirAceptacion(aceptarVariacion);
    correcciones.save(tasa.corregir(confirmada, usuario.usuarioId(), motivo, fechas.ahora()));
    tasas.flush();
    return detalle(id);
  }

  private TasaCambio buscar(Long id) {
    return tasas
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La tasa no existe."));
  }

  private static Set<Long> idsDe(Long... ids) {
    Set<Long> conjunto = new HashSet<>();
    for (Long id : ids) {
      if (id != null) {
        conjunto.add(id);
      }
    }
    return conjunto;
  }

  private static TasaVigenteVista vigente(
      ParMoneda par,
      Optional<TasaCambio> tasa,
      LocalDate hoy,
      boolean fallo,
      Map<Long, String> nombres) {
    String aviso = AvisoTasa.para(par, tasa.map(TasaCambio::getFecha).orElse(null), hoy, fallo);
    return tasa.map(
            t ->
                new TasaVigenteVista(
                    par,
                    t.getId(),
                    t.getValor(),
                    t.getFecha(),
                    t.getFuente(),
                    t.getRegistradaEn(),
                    t.getRegistradaPor() == null ? null : nombres.get(t.getRegistradaPor()),
                    t.getFecha().equals(hoy),
                    aviso))
        .orElseGet(
            () -> new TasaVigenteVista(par, null, null, null, null, null, null, false, aviso));
  }

  private static TasaVista vista(
      TasaCambio tasa, Map<Long, String> nombres, List<TasaVista.CorreccionVista> lista) {
    return new TasaVista(
        tasa.getId(),
        tasa.getPar(),
        tasa.getFecha(),
        tasa.getValor(),
        tasa.getFuente(),
        tasa.getRegistradaEn(),
        tasa.getRegistradaPor() == null ? null : nombres.get(tasa.getRegistradaPor()),
        lista);
  }

  private static VariacionVista vista(VariacionTasa variacion) {
    return new VariacionVista(
        variacion.anterior(),
        variacion.nueva(),
        variacion.porcentaje(),
        variacion.limite(),
        variacion.superaLimite());
  }
}
