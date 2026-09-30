package co.italarm.api.configuracion.aplicacion;

import java.math.BigDecimal;

/** Configuración como la ve el frontend. {@code logoUrl} es un enlace firmado de corta duración. */
public record ConfiguracionVista(
    String empresaNombre,
    String empresaLema,
    String empresaNit,
    String empresaCiudad,
    String empresaTelefono,
    String empresaCorreo,
    String logoUrl,
    BigDecimal limiteVariacionTasa,
    int validezCotizacionDias,
    int garantiaManoObraMeses,
    int garantiaEquiposMeses,
    String condicionesGarantia,
    String piePdf,
    long version) {}
