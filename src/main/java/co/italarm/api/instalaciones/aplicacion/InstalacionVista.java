package co.italarm.api.instalaciones.aplicacion;

import co.italarm.api.comercial.aplicacion.ClienteDocumentoVista;
import co.italarm.api.comercial.aplicacion.ResumenCobroVista;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.usuarios.aplicacion.UsuarioReferencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Detalle de una instalación (RF-121): trabajo, técnicos, material con seriales, cobro, garantías y
 * fotos por grupo.
 *
 * @param estado ACTIVA o ANULADA
 */
public record InstalacionVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    ClienteDocumentoVista cliente,
    String direccion,
    String descripcion,
    List<UsuarioReferencia> tecnicos,
    Moneda moneda,
    TasasDocumentoVista tasas,
    List<Linea> lineas,
    String descuentoTipo,
    BigDecimal descuentoValor,
    ResumenCobroVista resumen,
    Dinero total,
    Dinero utilidad,
    BigDecimal porcentajeUtilidad,
    GarantiasInstalacionVista garantias,
    Fotos fotos,
    String observaciones,
    Set<Moneda> monedasComprobante,
    String estado,
    Anulacion anulacion,
    String registradaPor,
    Instant registradaEn,
    long version) {

  /** Material usado, con el costo en USD al momento de la salida (RF-68) y sus seriales. */
  public record Linea(
      Long productoId,
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      Dinero precioUnitario,
      Dinero precioSugerido,
      Dinero subtotal,
      Dinero costoUnitarioUsd,
      List<SerialInstalado> seriales) {}

  public record SerialInstalado(Long id, String numero, LocalDate vencimientoGarantia) {}

  /** Fotos por grupo (RF-110, RF-111); el tamaño de cada lista es cuántas fotos tiene. */
  public record Fotos(List<Foto> antes, List<Foto> durante, List<Foto> despues) {}

  /**
   * @param url enlace firmado de corta duración
   */
  public record Foto(Long id, String url, Instant subidaEn) {}

  /** Quién anuló la instalación, cuándo y por qué (RF-73). */
  public record Anulacion(String motivo, String usuario, Instant fecha) {}
}
