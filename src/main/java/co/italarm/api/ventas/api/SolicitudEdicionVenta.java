package co.italarm.api.ventas.api;

import co.italarm.api.shared.dominio.Moneda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Datos descriptivos de una venta que sí se pueden cambiar (RF-70, P-34). */
public record SolicitudEdicionVenta(
    @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres.")
        String observaciones,
    Set<Moneda> monedasComprobante,
    @NotNull(message = "Falta la versión del registro.") Long version) {}
