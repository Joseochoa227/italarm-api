package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Ajuste de inventario (RF-58 a RF-62).
 *
 * @param tipo ENTRADA o SALIDA
 * @param motivo PERDIDA, DANO, CONTEO_FISICO, GARANTIA u OTRO
 * @param cantidad siempre positiva; el sentido lo da {@code tipo}
 * @param costoUnitarioUsd costo con que entró o salió
 */
public record AjusteVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    ProductoReferencia producto,
    String tipo,
    String motivo,
    String motivoEtiqueta,
    String descripcion,
    BigDecimal cantidad,
    String abreviatura,
    Dinero costoUnitarioUsd,
    Dinero valorUsd,
    List<String> seriales,
    String registradoPor,
    Instant registradoEn) {}
