package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.shared.api.Pagina;
import co.italarm.api.shared.dominio.Dinero;
import java.time.LocalDate;
import java.util.List;

/** Instalaciones de un período con sus totales sin las anuladas (RF-121, RF-73). */
public record ListadoInstalacionesVista(
    LocalDate desde,
    LocalDate hasta,
    Pagina<InstalacionResumenVista> instalaciones,
    List<TotalMoneda> totalesPorMoneda,
    Dinero totalUsd,
    Dinero utilidadUsd) {

  public record TotalMoneda(
      Dinero material,
      Dinero manoDeObra,
      Dinero total,
      Dinero costo,
      Dinero utilidad,
      long instalaciones) {}
}
