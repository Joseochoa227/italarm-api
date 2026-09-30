package co.italarm.api.tasas.infraestructura;

import co.italarm.api.shared.infraestructura.PropiedadesItalarm;
import co.italarm.api.tasas.aplicacion.FuenteTrm;
import co.italarm.api.tasas.aplicacion.FuenteTrmNoDisponibleException;
import co.italarm.api.tasas.aplicacion.TrmPublicada;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * TRM de los datos abiertos de la Superintendencia Financiera en datos.gov.co, conjunto {@code
 * 32sa-8pi3} (sección 9.2). Toma la TRM cuya vigencia cubre la fecha pedida, lo que resuelve fines
 * de semana y festivos.
 */
@Component
public class FuenteTrmDatosAbiertos implements FuenteTrm {

  private final RestClient cliente;
  private final String url;

  public FuenteTrmDatosAbiertos(PropiedadesItalarm propiedades) {
    PropiedadesItalarm.Trm trm = propiedades.trm();
    HttpClient http = HttpClient.newBuilder().connectTimeout(trm.tiempoMaximo()).build();
    JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
    fabrica.setReadTimeout(trm.tiempoMaximo());
    this.cliente = RestClient.builder().requestFactory(fabrica).build();
    this.url = trm.url();
  }

  /** Fila del conjunto de datos: los valores vienen como texto. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record FilaTrm(String valor, String unidad, String vigenciadesde, String vigenciahasta) {}

  @Override
  public TrmPublicada consultar(LocalDate fecha) {
    String dia = fecha + "T00:00:00.000";
    URI consulta =
        UriComponentsBuilder.fromUriString(url)
            .queryParam(
                "$where", "vigenciadesde <= '" + dia + "' AND vigenciahasta >= '" + dia + "'")
            .queryParam("$order", "vigenciadesde DESC")
            .queryParam("$limit", "1")
            .encode()
            .build()
            .toUri();
    List<FilaTrm> filas;
    try {
      filas = cliente.get().uri(consulta).retrieve().body(new ParameterizedTypeReference<>() {});
    } catch (RestClientException e) {
      throw new FuenteTrmNoDisponibleException(
          "La fuente de la TRM no respondió: " + e.getMessage(), e);
    }
    if (filas == null || filas.isEmpty()) {
      throw new FuenteTrmNoDisponibleException("La fuente no tiene la TRM del " + fecha);
    }
    FilaTrm fila = filas.get(0);
    if (fila.unidad() != null && !"COP".equalsIgnoreCase(fila.unidad())) {
      throw new FuenteTrmNoDisponibleException("Unidad inesperada en la TRM: " + fila.unidad());
    }
    try {
      BigDecimal valor = new BigDecimal(fila.valor().trim());
      if (valor.signum() <= 0) {
        throw new FuenteTrmNoDisponibleException("La fuente devolvió una TRM no positiva");
      }
      return new TrmPublicada(
          valor,
          LocalDateTime.parse(fila.vigenciadesde()).toLocalDate(),
          LocalDateTime.parse(fila.vigenciahasta()).toLocalDate());
    } catch (RuntimeException e) {
      if (e instanceof FuenteTrmNoDisponibleException propia) {
        throw propia;
      }
      throw new FuenteTrmNoDisponibleException("La respuesta de la TRM no es válida", e);
    }
  }
}
