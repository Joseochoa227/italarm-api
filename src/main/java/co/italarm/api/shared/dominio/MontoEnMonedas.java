package co.italarm.api.shared.dominio;

/**
 * Un valor en las tres monedas (RF-31). Un equivalente queda vacío si falta la tasa para
 * calcularlo.
 */
public record MontoEnMonedas(Dinero usd, Dinero cop, Dinero ves) {}
