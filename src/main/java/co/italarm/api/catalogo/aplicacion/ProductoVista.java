package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.math.BigDecimal;

/**
 * Producto como lo ve el frontend. {@code stock} y {@code costoActual} son de solo lectura (los
 * mueve el inventario desde la Fase 2). {@code fotoUrl} es un enlace firmado de corta duración.
 */
public record ProductoVista(
    Long id,
    String codigo,
    String nombre,
    String marca,
    String modelo,
    Referencia categoria,
    UnidadMedidaVista unidadMedida,
    boolean controlaSerial,
    Dinero precioInstalador,
    Dinero precioClienteFinal,
    BigDecimal stock,
    BigDecimal stockMinimo,
    boolean bajoMinimo,
    Dinero costoActual,
    String descripcion,
    String fotoUrl,
    boolean activo,
    long version) {

  /** Referencia a otro registro por id y nombre. */
  public record Referencia(Long id, String nombre) {}
}
