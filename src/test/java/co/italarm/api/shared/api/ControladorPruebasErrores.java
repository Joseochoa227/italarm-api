package co.italarm.api.shared.api;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador solo para pruebas: provoca cada tipo de error. No existe en el código principal. */
@RestController
@RequestMapping("/api/v1/_pruebas")
class ControladorPruebasErrores {

  static final class StockDePrueba extends NegocioException {
    StockDePrueba() {
      super(TipoError.REGLA_NEGOCIO, "STOCK_INSUFICIENTE", "Stock insuficiente · quedan 3");
    }
  }

  @GetMapping("/negocio")
  void negocio() {
    throw new StockDePrueba();
  }

  @GetMapping("/inesperado")
  void inesperado() {
    throw new IllegalStateException("detalle interno que no debe salir");
  }

  @GetMapping("/concurrencia")
  void concurrencia() {
    throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
  }

  @GetMapping("/acceso")
  void acceso() {
    throw new AccessDeniedException("sin permiso");
  }

  @GetMapping("/dinero")
  Dinero dinero() {
    return Dinero.de("19.5000", Moneda.USD);
  }

  @GetMapping("/dinero-grande")
  Dinero dineroGrande() {
    return new Dinero(new java.math.BigDecimal("1E+7"), Moneda.COP);
  }
}
