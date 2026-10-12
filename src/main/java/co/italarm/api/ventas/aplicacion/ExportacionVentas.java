package co.italarm.api.ventas.aplicacion;

import static co.italarm.api.documentos.aplicacion.LibroExcel.dinero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fecha;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fechaHora;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fila;
import static co.italarm.api.documentos.aplicacion.LibroExcel.texto;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Listado de ventas en Excel, con los mismos filtros (RNF-06, P-62). */
@Service
public class ExportacionVentas {

  private final ServicioVentas ventas;
  private final ExportadorExcel exportador;

  public ExportacionVentas(ServicioVentas ventas, ExportadorExcel exportador) {
    this.ventas = ventas;
    this.exportador = exportador;
  }

  public ArchivoGenerado excel(
      Long clienteId, Long productoId, LocalDate desde, LocalDate hasta, Boolean incluirAnuladas) {
    ListadoVentasVista listado =
        ventas.listar(
            clienteId,
            productoId,
            desde,
            hasta,
            incluirAnuladas,
            PageRequest.of(
                0,
                ExportadorExcel.LIMITE_FILAS + 1,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("numero"))));
    ExportadorExcel.exigirLimite(listado.ventas().contenido().size());
    return exportador.exportar(
        new LibroExcel(
            "ventas",
            List.of(
                new LibroExcel.Hoja(
                    "Ventas",
                    "Ventas",
                    List.of(LibroExcel.periodo(listado.desde(), listado.hasta())),
                    List.of(
                        "Consecutivo",
                        "Fecha",
                        "Cliente",
                        "Productos",
                        "Total",
                        "Total USD",
                        "Utilidad",
                        "Estado",
                        "Registrada por",
                        "Registrada el"),
                    listado.ventas().contenido().stream()
                        .map(
                            v ->
                                fila(
                                    texto(v.consecutivo()),
                                    fecha(v.fecha()),
                                    texto(v.cliente()),
                                    texto(v.productos()),
                                    dinero(v.total()),
                                    dinero(v.totalUsd()),
                                    dinero(v.utilidad()),
                                    texto(v.estado()),
                                    texto(v.registradaPor()),
                                    fechaHora(v.registradaEn())))
                        .toList()))));
  }
}
