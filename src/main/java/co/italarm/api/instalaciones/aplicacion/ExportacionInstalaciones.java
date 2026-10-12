package co.italarm.api.instalaciones.aplicacion;

import static co.italarm.api.documentos.aplicacion.LibroExcel.dinero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fecha;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fechaHora;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fila;
import static co.italarm.api.documentos.aplicacion.LibroExcel.texto;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import co.italarm.api.shared.dominio.EstadoGarantia;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Listado de instalaciones en Excel, con los mismos filtros (RNF-06, P-62). */
@Service
public class ExportacionInstalaciones {

  private final ServicioInstalaciones instalaciones;
  private final ExportadorExcel exportador;

  public ExportacionInstalaciones(ServicioInstalaciones instalaciones, ExportadorExcel exportador) {
    this.instalaciones = instalaciones;
    this.exportador = exportador;
  }

  public ArchivoGenerado excel(
      Long clienteId,
      Long tecnicoId,
      LocalDate desde,
      LocalDate hasta,
      EstadoGarantia estadoGarantia,
      Boolean incluirAnuladas) {
    ListadoInstalacionesVista listado =
        instalaciones.listar(
            clienteId,
            tecnicoId,
            desde,
            hasta,
            estadoGarantia,
            incluirAnuladas,
            PageRequest.of(
                0,
                ExportadorExcel.LIMITE_FILAS + 1,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("numero"))));
    ExportadorExcel.exigirLimite(listado.instalaciones().contenido().size());
    return exportador.exportar(
        new LibroExcel(
            "instalaciones",
            List.of(
                new LibroExcel.Hoja(
                    "Instalaciones",
                    "Instalaciones",
                    List.of(LibroExcel.periodo(listado.desde(), listado.hasta())),
                    List.of(
                        "Consecutivo",
                        "Fecha",
                        "Cliente",
                        "Dirección",
                        "Técnicos",
                        "Total",
                        "Total USD",
                        "Utilidad",
                        "Garantía de mano de obra hasta",
                        "Estado de la garantía",
                        "Estado",
                        "Registrada por",
                        "Registrada el"),
                    listado.instalaciones().contenido().stream()
                        .map(
                            i ->
                                fila(
                                    texto(i.consecutivo()),
                                    fecha(i.fecha()),
                                    texto(i.cliente()),
                                    texto(i.direccion()),
                                    texto(i.tecnicos()),
                                    dinero(i.total()),
                                    dinero(i.totalUsd()),
                                    dinero(i.utilidad()),
                                    fecha(i.venceManoObra()),
                                    texto(
                                        i.estadoGarantia() == null
                                            ? null
                                            : i.estadoGarantia().name()),
                                    texto(i.estado()),
                                    texto(i.registradaPor()),
                                    fechaHora(i.registradaEn())))
                        .toList()))));
  }
}
