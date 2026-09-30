package co.italarm.api.soporte;

import org.springframework.jdbc.core.JdbcTemplate;

/** Deja la base de datos como la dejan las migraciones, antes de cada prueba de integración. */
final class LimpiezaDatos {

  private LimpiezaDatos() {}

  static void restablecer(JdbcTemplate jdbc) {
    jdbc.update("delete from producto");
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
