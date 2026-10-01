package co.italarm.api.inventario.infraestructura;

import co.italarm.api.inventario.dominio.EstadoSerial;
import co.italarm.api.inventario.dominio.Serial;
import co.italarm.api.shared.dominio.TipoDocumento;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerialRepositorio extends JpaRepository<Serial, Long> {

  /** Números que ya existen (sin contar los anulados) para el producto. */
  @Query(
      "select s.numero from Serial s where s.productoId = :producto and s.numero in :numeros"
          + " and s.estado <> co.italarm.api.inventario.dominio.EstadoSerial.ANULADO")
  List<String> existentes(
      @Param("producto") Long productoId, @Param("numeros") Collection<String> numeros);

  /** Bloquea los seriales (SELECT … FOR UPDATE) en orden de id (BP-08). */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select s from Serial s where s.productoId = :producto and s.numero in :numeros"
          + " and s.estado <> co.italarm.api.inventario.dominio.EstadoSerial.ANULADO"
          + " order by s.id")
  List<Serial> bloquear(
      @Param("producto") Long productoId, @Param("numeros") Collection<String> numeros);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select s from Serial s where s.productoId = :producto"
          + " and s.documentoEntrada.tipo = :tipo and s.documentoEntrada.id = :documento"
          + " order by s.id")
  List<Serial> bloquearDeEntrada(
      @Param("producto") Long productoId,
      @Param("tipo") TipoDocumento tipo,
      @Param("documento") Long documentoId);

  List<Serial> findByProductoIdAndDocumentoEntradaTipoAndDocumentoEntradaId(
      Long productoId, TipoDocumento tipo, Long documentoId);

  List<Serial> findByDocumentoEntradaTipoAndDocumentoEntradaIdOrderById(
      TipoDocumento tipo, Long documentoId);

  List<Serial> findByDocumentoSalidaTipoAndDocumentoSalidaIdOrderById(
      TipoDocumento tipo, Long documentoId);

  List<Serial> findByProductoIdOrderByNumero(Long productoId);

  List<Serial> findByProductoIdAndEstadoOrderByNumero(Long productoId, EstadoSerial estado);

  List<Serial> findTop50ByNumeroContainingOrderByNumero(String numero);

  /** Productos con algún serial que contiene el texto (búsqueda del inventario, RF-51). */
  @Query("select distinct s.productoId from Serial s where s.numero like :patron escape '\\'")
  List<Long> productosConSerial(@Param("patron") String patron);

  /** Cantidad de seriales del producto por estado: filas {@code [estado, cantidad]}. */
  @Query("select s.estado, count(s) from Serial s where s.productoId = :producto group by s.estado")
  List<Object[]> contarPorEstado(@Param("producto") Long productoId);
}
