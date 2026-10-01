package co.italarm.api.catalogo.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.math.BigDecimal;

/** Producto con su stock y costo para las consultas del inventario (RF-49 a RF-55). */
public record ProductoValorizado(
    Long id,
    String codigo,
    String nombre,
    String marca,
    String modelo,
    Long categoriaId,
    String categoria,
    String abreviatura,
    boolean controlaSerial,
    BigDecimal stock,
    BigDecimal stockMinimo,
    boolean bajoMinimo,
    BigDecimal costoActualUsd,
    Dinero precioInstalador,
    Dinero precioClienteFinal,
    String fotoUrl,
    boolean activo) {}
