package co.italarm.api.usuarios.infraestructura;

import co.italarm.api.usuarios.dominio.Sesion;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionRepositorio extends JpaRepository<Sesion, Long> {

  Optional<Sesion> findByTokenHashAndRevocadaEnIsNull(String tokenHash);

  /** Cierra todas las sesiones activas del usuario. */
  @Modifying
  @Query(
      "update Sesion s set s.revocadaEn = :ahora"
          + " where s.usuarioId = :usuarioId and s.revocadaEn is null")
  int revocarTodas(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

  /** Cierra todas las sesiones activas del usuario, salvo la indicada. */
  @Modifying
  @Query(
      "update Sesion s set s.revocadaEn = :ahora"
          + " where s.usuarioId = :usuarioId and s.id <> :sesionConservada"
          + " and s.revocadaEn is null")
  int revocarOtras(
      @Param("usuarioId") Long usuarioId,
      @Param("sesionConservada") Long sesionConservada,
      @Param("ahora") Instant ahora);
}
