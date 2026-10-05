package co.italarm.api.configuracion.aplicacion;

/**
 * Datos de la empresa y textos que salen en los PDF (RF-127, RF-131, RF-145).
 *
 * @param logo contenido de la imagen del logo, o vacío si no se ha cargado
 */
public record EmpresaDocumentos(
    String nombre,
    String lema,
    String nit,
    String ciudad,
    String telefono,
    String correo,
    byte[] logo,
    String condicionesGarantia,
    String piePdf,
    int garantiaEquiposMeses,
    int garantiaManoObraMeses) {}
