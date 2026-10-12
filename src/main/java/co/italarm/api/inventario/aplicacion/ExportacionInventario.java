package co.italarm.api.inventario.aplicacion;

import static co.italarm.api.documentos.aplicacion.LibroExcel.dinero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fecha;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fechaHora;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fila;
import static co.italarm.api.documentos.aplicacion.LibroExcel.numero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.texto;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Inventario valorizado y kárdex en Excel (RNF-06, P-62). */
@Service
public class ExportacionInventario {

  private final ServicioInventario inventario;
  private final ExportadorExcel exportador;

  public ExportacionInventario(ServicioInventario inventario, ExportadorExcel exportador) {
    this.inventario = inventario;
    this.exportador = exportador;
  }

  public ArchivoGenerado inventario(Long categoriaId, Boolean activo, String buscar) {
    InventarioVista vista =
        inventario.listar(
            categoriaId,
            activo,
            buscar,
            PageRequest.of(0, ExportadorExcel.LIMITE_FILAS + 1, Sort.by("nombre")));
    ExportadorExcel.exigirLimite(vista.productos().contenido().size());
    List<String> filtros = new ArrayList<>();
    if (buscar != null && !buscar.isBlank()) {
      filtros.add("Búsqueda: " + buscar);
    }
    filtros.add(
        "Productos: "
            + vista.totalProductos()
            + ". Valor en bodega con el costo actual; COP y VES con las tasas de hoy.");
    List<List<LibroExcel.Celda>> filas = new ArrayList<>();
    for (InventarioVista.Producto p : vista.productos().contenido()) {
      filas.add(
          fila(
              texto(p.codigo()),
              texto(p.nombre()),
              texto(p.marca()),
              texto(p.categoria()),
              numero(p.stock()),
              texto(p.abreviatura()),
              numero(p.stockMinimo()),
              texto(p.bajoMinimo() ? "Sí" : "No"),
              dinero(p.costoActualUsd()),
              dinero(p.valorEnBodega().usd()),
              dinero(p.valorEnBodega().cop()),
              dinero(p.valorEnBodega().ves()),
              texto(p.activo() ? "Activo" : "Inactivo")));
    }
    filas.add(
        fila(
            texto("Total"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            dinero(vista.valorTotal().usd()),
            dinero(vista.valorTotal().cop()),
            dinero(vista.valorTotal().ves()),
            null));
    return exportador.exportar(
        new LibroExcel(
            "inventario",
            List.of(
                new LibroExcel.Hoja(
                    "Inventario",
                    "Inventario valorizado",
                    filtros,
                    List.of(
                        "Código",
                        "Producto",
                        "Marca",
                        "Categoría",
                        "Stock",
                        "Unidad",
                        "Mínimo",
                        "Bajo el mínimo",
                        "Costo USD",
                        "Valor USD",
                        "Valor COP",
                        "Valor VES",
                        "Estado"),
                    filas))));
  }

  public ArchivoGenerado kardex(Long productoId) {
    ProductoInventarioVista producto = inventario.detalle(productoId);
    Page<MovimientoKardexVista> movimientos =
        inventario.kardex(
            productoId,
            PageRequest.of(0, ExportadorExcel.LIMITE_FILAS + 1, Sort.by(Sort.Order.asc("id"))));
    ExportadorExcel.exigirLimite(movimientos.getContent().size());
    return exportador.exportar(
        new LibroExcel(
            "kardex-" + producto.codigo(),
            List.of(
                new LibroExcel.Hoja(
                    "Kárdex",
                    "Kárdex de " + producto.codigo() + " · " + producto.nombre(),
                    List.of("Del movimiento más antiguo al más reciente."),
                    List.of(
                        "Fecha",
                        "Movimiento",
                        "Documento",
                        "Detalle",
                        "Entrada",
                        "Salida",
                        "Saldo",
                        "Costo unitario USD",
                        "Usuario",
                        "Registrado el"),
                    movimientos.getContent().stream()
                        .map(
                            m ->
                                fila(
                                    fecha(m.fecha()),
                                    texto(m.tipoEtiqueta()),
                                    texto(
                                        m.documento() == null ? null : m.documento().consecutivo()),
                                    texto(m.detalle()),
                                    numero(m.entrada()),
                                    numero(m.salida()),
                                    numero(m.saldo()),
                                    dinero(m.costoUnitarioUsd()),
                                    texto(m.usuario()),
                                    fechaHora(m.registradoEn())))
                        .toList()))));
  }
}
