package co.italarm.api.configuracion.dominio;

import java.math.BigDecimal;

/** Datos editables de la configuración (RF-145 a RF-147). */
public record DatosConfiguracion(
    String empresaNombre,
    String empresaLema,
    String empresaNit,
    String empresaCiudad,
    String empresaTelefono,
    String empresaCorreo,
    BigDecimal limiteVariacionTasa,
    int validezCotizacionDias,
    int garantiaManoObraMeses,
    int garantiaEquiposMeses,
    String condicionesGarantia,
    String piePdf) {}
