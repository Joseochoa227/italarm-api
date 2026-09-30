package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;

/** Datos editables de un producto (sección 3.3). */
public record DatosProducto(
    String codigo,
    String nombre,
    String marca,
    String modelo,
    boolean controlaSerial,
    BigDecimal precioInstalador,
    BigDecimal precioClienteFinal,
    Moneda monedaPrecio,
    BigDecimal stockMinimo,
    String descripcion) {}
