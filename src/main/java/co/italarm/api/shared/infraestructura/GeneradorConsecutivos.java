package co.italarm.api.shared.infraestructura;

import co.italarm.api.shared.dominio.TipoDocumento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Consecutivos de los documentos con secuencias de PostgreSQL por tipo (BP-11). Una secuencia nunca
 * repite un número; si la operación falla después de pedirlo, queda un salto (RN-09 exige números
 * únicos y que no se reutilicen).
 */
@Component
public class GeneradorConsecutivos {

  private final JdbcTemplate jdbc;

  public GeneradorConsecutivos(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long siguiente(TipoDocumento tipo) {
    String secuencia =
        switch (tipo) {
          case COMPRA -> "seq_compra";
          case AJUSTE -> "seq_ajuste";
          case INVENTARIO_INICIAL -> "seq_inventario_inicial";
          default -> throw new IllegalStateException("Sin secuencia para " + tipo);
        };
    Long numero = jdbc.queryForObject("select nextval('" + secuencia + "')", Long.class);
    if (numero == null) {
      throw new IllegalStateException("La secuencia " + secuencia + " no devolvió un número");
    }
    return numero;
  }
}
