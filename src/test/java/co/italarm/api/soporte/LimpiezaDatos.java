package co.italarm.api.soporte;

import org.springframework.jdbc.core.JdbcTemplate;

/** Deja la base de datos como la dejan las migraciones, antes de cada prueba de integración. */
final class LimpiezaDatos {

  private LimpiezaDatos() {}

  static void restablecer(JdbcTemplate jdbc) {
    jdbc.update("delete from enlace_comprobante");
    jdbc.update("delete from movimiento_serial");
    jdbc.update("delete from serial");
    jdbc.update("delete from movimiento_inventario");
    jdbc.update("delete from historial_costo");
    jdbc.update("delete from linea_venta");
    jdbc.update("delete from venta");
    jdbc.update("delete from linea_compra");
    jdbc.update("delete from compra");
    jdbc.update("delete from ajuste");
    jdbc.update("delete from linea_inventario_inicial");
    jdbc.update("delete from inventario_inicial");
    jdbc.update("delete from idempotencia");
    jdbc.execute("alter sequence seq_compra restart with 1");
    jdbc.execute("alter sequence seq_venta restart with 1");
    jdbc.execute("alter sequence seq_ajuste restart with 1");
    jdbc.execute("alter sequence seq_inventario_inicial restart with 1");
    jdbc.update(
        "update configuracion set empresa_nombre = 'ITALARM',"
            + " empresa_lema = 'Instalación de cámaras de seguridad', empresa_nit = null,"
            + " empresa_ciudad = null, empresa_telefono = null, empresa_correo = null,"
            + " empresa_logo_clave = null, limite_variacion_tasa = 5, validez_cotizacion_dias = 15,"
            + " garantia_mano_obra_meses = 3, garantia_equipos_meses = 3, version = 0,"
            + " condiciones_garantia = 'No cubre daños por descargas eléctricas, humedad o"
            + " manipulación de terceros.', pie_pdf = 'Pago de contado. Garantía de 3 meses en"
            + " equipos y mano de obra. Documento no válido como factura.'");
    jdbc.update("delete from correccion_tasa");
    jdbc.update("delete from tasa_cambio");
    jdbc.update("delete from ejecucion_tarea_trm");
    jdbc.update("delete from producto");
    jdbc.update("delete from sesion");
    jdbc.update("update usuario set updated_by = null, created_by = null where id <= 2");
    jdbc.update("delete from usuario where id > 2");
    jdbc.update("delete from cliente");
    jdbc.update("delete from proveedor");
    jdbc.update("delete from categoria where id > 8");
    jdbc.update("delete from unidad_medida where id > 3");
    jdbc.update(
        "update categoria set nombre = case id when 1 then 'Cámaras'"
            + " when 2 then 'Grabadores (DVR/NVR)' when 3 then 'Discos duros' when 4 then 'Cable'"
            + " when 5 then 'Balunes' when 6 then 'Fuentes de poder' when 7 then 'Conectores'"
            + " else 'Cajas y accesorios' end");
    jdbc.update(
        "update unidad_medida set nombre = case id when 1 then 'Unidad' when 2 then 'Metro'"
            + " else 'Par' end, abreviatura = case id when 1 then 'und' when 2 then 'm'"
            + " else 'par' end, admite_decimales = (id = 2)");
  }
}
