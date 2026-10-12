package co.italarm.api.reportes.aplicacion;

import co.italarm.api.cotizaciones.aplicacion.CotizacionResumenVista;
import co.italarm.api.cotizaciones.aplicacion.ServicioCotizaciones;
import co.italarm.api.cotizaciones.dominio.EstadoCotizacion;
import co.italarm.api.garantias.aplicacion.GarantiaVista;
import co.italarm.api.garantias.aplicacion.ServicioGarantias;
import co.italarm.api.reportes.dominio.Periodo;
import co.italarm.api.reportes.dominio.TipoPeriodo;
import co.italarm.api.reportes.infraestructura.ConsultasReportesSql;
import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.EstadoGarantia;
import co.italarm.api.shared.dominio.FechaNegocio;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDocumento;
import co.italarm.api.tasas.aplicacion.ServicioTasas;
import co.italarm.api.tasas.aplicacion.TasasVigentesVista;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pantalla de Inicio (sección 3.2): el estado del negocio del mes en curso. */
@Service
public class ServicioInicio {

  /** Filas por bloque; el resto se ve con "Ver todo". */
  static final int FILAS = 10;

  private final ServicioReportes reportes;
  private final ConsultasReportesSql consultas;
  private final ServicioTasas tasas;
  private final ServicioGarantias garantias;
  private final ServicioCotizaciones cotizaciones;
  private final ConsultaUsuarios usuarios;
  private final FechaNegocio fechas;

  public ServicioInicio(
      ServicioReportes reportes,
      ConsultasReportesSql consultas,
      ServicioTasas tasas,
      ServicioGarantias garantias,
      ServicioCotizaciones cotizaciones,
      ConsultaUsuarios usuarios,
      FechaNegocio fechas) {
    this.reportes = reportes;
    this.consultas = consultas;
    this.tasas = tasas;
    this.garantias = garantias;
    this.cotizaciones = cotizaciones;
    this.usuarios = usuarios;
    this.fechas = fechas;
  }

  @Transactional(readOnly = true)
  public InicioVista inicio(Long usuarioId) {
    LocalDate hoy = fechas.hoy();
    Periodo mes = Periodo.de(TipoPeriodo.MES, hoy);
    TasasVigentesVista vigentes = tasas.vigentes();
    Page<GarantiaVista> porVencer =
        garantias.consultar(EstadoGarantia.POR_VENCER, null, null, null, PageRequest.of(0, FILAS));
    Pagina<CotizacionResumenVista> enEvaluacion =
        cotizaciones.listar(
            EstadoCotizacion.EN_EVALUACION,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, FILAS, Sort.by("vence", "numero")));
    return new InicioVista(
        usuarios.nombres(Set.of(usuarioId)).get(usuarioId),
        hoy,
        avisos(vigentes),
        vigentes,
        reportes.indicadores(mes.desde(), mes.hasta(), reportes.tasasDeHoy()),
        reportes.stockBajo(FILAS),
        reportes.contarStockBajo(),
        porVencer.getContent(),
        porVencer.getTotalElements(),
        enEvaluacion.contenido(),
        enEvaluacion.totalElementos(),
        consultas.ultimosMovimientos(FILAS).stream()
            .map(
                m ->
                    new MovimientoRecienteVista(
                        m.tipo(),
                        m.id(),
                        TipoDocumento.valueOf(m.tipo()).consecutivo(m.numero()),
                        m.detalle(),
                        m.usuario(),
                        m.registradoEn(),
                        new Dinero(m.valor(), Moneda.valueOf(m.moneda())),
                        m.anulado()))
            .toList());
  }

  /**
   * RF-07: falta la tasa del bolívar de hoy, o la TRM automática falló. Antes de la consulta de las
   * 6:00 a. m. no se avisa por la TRM: la tarea todavía no ha corrido.
   */
  private static List<InicioVista.AvisoTasa> avisos(TasasVigentesVista vigentes) {
    List<InicioVista.AvisoTasa> avisos = new ArrayList<>();
    if (!vigentes.bolivar().esDeHoy()) {
      avisos.add(
          new InicioVista.AvisoTasa(
              "USD_VES",
              vigentes.bolivar().aviso() != null
                  ? vigentes.bolivar().aviso()
                  : "Falta registrar la tasa del bolívar de hoy."));
    }
    if (vigentes.trmAutomaticaFallo()) {
      avisos.add(
          new InicioVista.AvisoTasa(
              "USD_COP", "La consulta automática de la TRM de hoy falló. Regístrala a mano."));
    } else if (vigentes.trm().valor() == null) {
      avisos.add(new InicioVista.AvisoTasa("USD_COP", "No hay ninguna TRM registrada."));
    }
    return avisos;
  }
}
