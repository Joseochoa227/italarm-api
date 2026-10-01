package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Carga inicial realizada (RF-151, RF-152): documento, usuario, fecha y resumen.
 *
 * @param productosConStock productos con un movimiento de inventario inicial
 * @param valorUsd suma de cantidad × costo de esos productos
 */
public record CargaInicialVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    String archivo,
    int productosCreados,
    int clientesCreados,
    int proveedoresCreados,
    int productosConStock,
    Dinero valorUsd,
    String registradaPor,
    Instant registradaEn) {}
