package co.italarm.api.inventario.aplicacion;

import co.italarm.api.inventario.dominio.MotivoAjuste;
import java.math.BigDecimal;
import java.util.List;

/**
 * Ajuste a registrar (RF-58).
 *
 * @param cantidad positiva = entrada; negativa = salida
 * @param costoUnitarioUsd solo se usa si el producto nunca tuvo costo (P-25)
 * @param seriales nuevos en una entrada; los que se dan de baja en una salida
 */
public record DatosAjuste(
    Long productoId,
    MotivoAjuste motivo,
    String descripcion,
    BigDecimal cantidad,
    BigDecimal costoUnitarioUsd,
    List<String> seriales) {}
