package co.italarm.api.garantias.aplicacion;

import static co.italarm.api.documentos.aplicacion.LibroExcel.fecha;
import static co.italarm.api.documentos.aplicacion.LibroExcel.fila;
import static co.italarm.api.documentos.aplicacion.LibroExcel.numero;
import static co.italarm.api.documentos.aplicacion.LibroExcel.texto;

import co.italarm.api.documentos.aplicacion.ArchivoGenerado;
import co.italarm.api.documentos.aplicacion.ExportadorExcel;
import co.italarm.api.documentos.aplicacion.LibroExcel;
import co.italarm.api.shared.dominio.EstadoGarantia;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Consulta de garantías en Excel, con los mismos filtros (RNF-06, P-62). */
@Service
public class ExportacionGarantias {

  private final ServicioGarantias garantias;
  private final ExportadorExcel exportador;

  public ExportacionGarantias(ServicioGarantias garantias, ExportadorExcel exportador) {
    this.garantias = garantias;
    this.exportador = exportador;
  }

  public ArchivoGenerado excel(EstadoGarantia estado, Long clienteId, String tipo, String serial) {
    Page<GarantiaVista> encontradas =
        garantias.consultar(
            estado, clienteId, tipo, serial, PageRequest.of(0, ExportadorExcel.LIMITE_FILAS + 1));
    ExportadorExcel.exigirLimite(encontradas.getContent().size());
    List<String> filtros = new ArrayList<>();
    if (estado != null) {
      filtros.add("Estado: " + estado.name());
    }
    if (serial != null && !serial.isBlank()) {
      filtros.add("Serial: " + serial);
    }
    return exportador.exportar(
        new LibroExcel(
            "garantias",
            List.of(
                new LibroExcel.Hoja(
                    "Garantías",
                    "Garantías",
                    filtros,
                    List.of(
                        "Documento",
                        "Fecha",
                        "Cliente",
                        "Garantía de",
                        "Serial",
                        "Producto",
                        "Vence",
                        "Días restantes",
                        "Estado"),
                    encontradas.getContent().stream()
                        .map(
                            g ->
                                fila(
                                    texto(g.documento().consecutivo()),
                                    fecha(g.fecha()),
                                    texto(g.cliente()),
                                    texto(
                                        "MANO_OBRA".equals(g.clase()) ? "Mano de obra" : "Equipo"),
                                    texto(g.serial()),
                                    texto(g.producto()),
                                    fecha(g.vencimiento()),
                                    numero(BigDecimal.valueOf(g.diasRestantes())),
                                    texto(g.estado().name())))
                        .toList()))));
  }
}
