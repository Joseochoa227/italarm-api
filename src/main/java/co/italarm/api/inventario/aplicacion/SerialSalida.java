package co.italarm.api.inventario.aplicacion;

import java.time.LocalDate;

/** Serial que salió con un documento, con su garantía (RF-23). */
public record SerialSalida(Long id, String numero, LocalDate vencimientoGarantia) {}
