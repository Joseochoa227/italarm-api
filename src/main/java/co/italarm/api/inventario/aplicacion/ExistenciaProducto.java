package co.italarm.api.inventario.aplicacion;

import java.math.BigDecimal;

/** Stock y costo vigente en USD de un producto bloqueado para una salida (RF-68). */
public record ExistenciaProducto(Long productoId, BigDecimal stock, BigDecimal costoActualUsd) {}
