package co.italarm.api.compras.aplicacion;

import static co.italarm.api.documentos.aplicacion.LibroExcel.dinero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fecha;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fechaHora;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fila;
import static co.italarm.api.documentos.aplicacion.LibroExcel.numero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.texto;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Listado de compras en Excel, con los mismos filtros (RNF-06, P-62). */
@Service
public class ExportacionCompras {

  private final ServicioCompras compras;
  private final ExportadorExcel exportador;

  public ExportacionCompras(ServicioCompras compras, ExportadorExcel exportador) {
    this.compras = compras;
    this.exportador = exportador;
  }

  public ArchivoGenerado excel(
      Long proveedorId,
      Long productoId,
      LocalDate desde,
      LocalDate hasta,
      Boolean incluirAnuladas) {
    ListadoComprasVista listado =
        compras.listar(
            proveedorId,
            productoId,
            desde,
            hasta,
            incluirAnuladas,
            PageRequest.of(
                0,
                ExportadorExcel.LIMITE_FILAS + 1,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("numero"))));
    ExportadorExcel.exigirLimite(listado.compras().contenido().size());
    return exportador.exportar(
        new LibroExcel(
            "compras",
            List.of(
                new LibroExcel.Hoja(
                    "Compras",
                    "Compras",
                    List.of(LibroExcel.periodo(listado.desde(), listado.hasta())),
                    List.of(
                        "Consecutivo",
                        "Fecha",
                        "Proveedor",
                        "Factura",
                        "Productos",
                        "Total",
                        "TRM",
                        "Tasa del bolívar",
                        "Total USD",
                        "Estado",
                        "Registrada por",
                        "Registrada el"),
                    listado.compras().contenido().stream()
                        .map(
                            c ->
                                fila(
                                    texto(c.consecutivo()),
                                    fecha(c.fecha()),
                                    texto(c.proveedor().nombre()),
                                    texto(c.numeroFactura()),
                                    texto(c.productos()),
                                    dinero(c.total()),
                                    c.tasas() == null ? null : numero(c.tasas().trm()),
                                    c.tasas() == null ? null : numero(c.tasas().tasaVes()),
                                    dinero(c.totalUsd()),
                                    texto(c.estado()),
                                    texto(c.registradaPor()),
                                    fechaHora(c.registradaEn())))
                        .toList()))));
  }
}
