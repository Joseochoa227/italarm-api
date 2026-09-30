package co.italarm.api.shared.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Formato estable de los listados paginados (RT-04). La página empieza en 0.
 *
 * @param <T> tipo de cada elemento
 */
public record Pagina<T>(
    List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

  public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> conversion) {
    return new Pagina<>(
        pagina.getContent().stream().map(conversion).toList(),
        pagina.getNumber(),
        pagina.getSize(),
        pagina.getTotalElements(),
        pagina.getTotalPages());
  }
}
