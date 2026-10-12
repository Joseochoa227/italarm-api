package co.italarm.api.cotizaciones.aplicacion;

import static co.italarm.api.documentos.aplicacion.LibroExcel.dinero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fecha;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fechaHora;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fila;
import static co.italarm.api.documentos.aplicacion.LibroExcel.numero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.texto;

import co.italarm.api.cotizaciones.dominio.EstadoCotizacion;
import co.italarm.api.cotizaciones.dominio.TipoCotizacion;
import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import co.italarm.api.shared.api.Pagina;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Listado de cotizaciones en Excel, con los mismos filtros (RNF-06, P-62). */
@Service
public class ExportacionCotizaciones {

  private final ServicioCotizaciones cotizaciones;
  private final ExportadorExcel exportador;

  public ExportacionCotizaciones(ServicioCotizaciones cotizaciones, ExportadorExcel exportador) {
    this.cotizaciones = cotizaciones;
    this.exportador = exportador;
  }

  public ArchivoGenerado excel(
      EstadoCotizacion estado,
      Long clienteId,
      TipoCotizacion tipo,
      LocalDate desde,
      LocalDate hasta,
      Boolean porVencer) {
    Pagina<CotizacionResumenVista> listado =
        cotizaciones.listar(
            estado,
            clienteId,
            tipo,
            desde,
            hasta,
            porVencer,
            PageRequest.of(
                0,
                ExportadorExcel.LIMITE_FILAS + 1,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("numero"))));
    ExportadorExcel.exigirLimite(listado.contenido().size());
    List<String> filtros = new ArrayList<>();
    if (desde != null && hasta != null) {
      filtros.add(LibroExcel.periodo(desde, hasta));
    }
    if (estado != null) {
      filtros.add("Estado: " + estado.name());
    }
    if (Boolean.TRUE.equals(porVencer)) {
      filtros.add("Solo las por vencer");
    }
    return exportador.exportar(
        new LibroExcel(
            "cotizaciones",
            List.of(
                new LibroExcel.Hoja(
                    "Cotizaciones",
                    "Cotizaciones",
                    filtros,
                    List.of(
                        "Consecutivo",
                        "Tipo",
                        "Fecha",
                        "Cliente",
                        "Total",
                        "Total USD",
                        "Estado",
                        "Vence",
                        "Días para vencer",
                        "Documento generado",
                        "Registrada por",
                        "Registrada el"),
                    listado.contenido().stream()
                        .map(
                            c ->
                                fila(
                                    texto(c.consecutivo()),
                                    texto("VENTA".equals(c.tipo()) ? "Venta" : "Instalación"),
                                    fecha(c.fecha()),
                                    texto(c.cliente()),
                                    dinero(c.total()),
                                    dinero(c.totalUsd()),
                                    texto(c.estado()),
                                    fecha(c.vence()),
                                    c.diasParaVencer() == null
                                        ? null
                                        : numero(BigDecimal.valueOf(c.diasParaVencer())),
                                    texto(c.documentoGenerado()),
                                    texto(c.registradaPor()),
                                    fechaHora(c.registradaEn())))
                        .toList()))));
  }
}
