package co.italarm.api.terceros.aplicacion;

import co.italarm.api.shared.dominio.ErrorCarga;
import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.terceros.dominio.Cliente;
import co.italarm.api.terceros.dominio.DatosCliente;
import co.italarm.api.terceros.dominio.DatosProveedor;
import co.italarm.api.terceros.dominio.Documentos;
import co.italarm.api.terceros.dominio.Proveedor;
import co.italarm.api.terceros.dominio.TipoCliente;
import co.italarm.api.terceros.dominio.TipoDocumento;
import co.italarm.api.terceros.infraestructura.ClienteRepositorio;
import co.italarm.api.terceros.infraestructura.ProveedorRepositorio;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Hojas Clientes y Proveedores de la carga inicial (sección 3.18). */
@Service
public class CargaTerceros {

  public static final String HOJA_CLIENTES = "Clientes";
  public static final String HOJA_PROVEEDORES = "Proveedores";

  private final ClienteRepositorio clientes;
  private final ProveedorRepositorio proveedores;
  private final Validator validador;

  public CargaTerceros(
      ClienteRepositorio clientes, ProveedorRepositorio proveedores, Validator validador) {
    this.clientes = clientes;
    this.proveedores = proveedores;
    this.validador = validador;
  }

  /** Errores de la hoja Clientes, incluidos documentos repetidos en el archivo o ya existentes. */
  @Transactional(readOnly = true)
  public List<ErrorCarga> validarClientes(List<FilaCliente> filas) {
    List<ErrorCarga> errores = new ArrayList<>();
    Set<String> documentos = new HashSet<>();
    for (FilaCliente fila : filas) {
      List<String> mensajes = violaciones(fila);
      if (fila.tipo() != null && !fila.tipo().isBlank() && tipoCliente(fila.tipo()) == null) {
        mensajes.add("Tipo: escribe Instalador o Cliente final.");
      }
      if (fila.tipoDocumento() != null && tipoDocumento(fila.tipoDocumento()) == null) {
        mensajes.add("Tipo de documento: escribe CC o NIT.");
      }
      if (mensajes.isEmpty()) {
        try {
          Cliente.crear(datos(fila));
        } catch (NegocioException e) {
          mensajes.add(e.getMessage());
        }
        TipoDocumento tipoDocumento = tipoDocumento(fila.tipoDocumento());
        String numero = Documentos.normalizar(fila.numeroDocumento());
        if (tipoDocumento != null && numero != null) {
          if (!documentos.add(tipoDocumento + ":" + numero)) {
            mensajes.add("El documento " + numero + " está repetido en el archivo.");
          } else if (clientes.existeDocumento(tipoDocumento, numero, null)) {
            mensajes.add("Ya existe un cliente con el documento " + numero + ".");
          }
        }
      }
      mensajes.forEach(m -> errores.add(new ErrorCarga(HOJA_CLIENTES, fila.fila(), m)));
    }
    return errores;
  }

  @Transactional(readOnly = true)
  public List<ErrorCarga> validarProveedores(List<FilaProveedor> filas) {
    List<ErrorCarga> errores = new ArrayList<>();
    for (FilaProveedor fila : filas) {
      List<String> mensajes = violaciones(fila);
      if (mensajes.isEmpty()) {
        try {
          Proveedor.crear(datos(fila));
        } catch (NegocioException e) {
          mensajes.add(e.getMessage());
        }
      }
      mensajes.forEach(m -> errores.add(new ErrorCarga(HOJA_PROVEEDORES, fila.fila(), m)));
    }
    return errores;
  }

  /** Crea los clientes y proveedores de las hojas ya validadas. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void crear(List<FilaCliente> filasClientes, List<FilaProveedor> filasProveedores) {
    filasClientes.forEach(f -> clientes.save(Cliente.crear(datos(f))));
    filasProveedores.forEach(f -> proveedores.save(Proveedor.crear(datos(f))));
    clientes.flush();
  }

  private <T> List<String> violaciones(T fila) {
    return validador.validate(fila).stream()
        .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static DatosCliente datos(FilaCliente f) {
    return new DatosCliente(
        tipoCliente(f.tipo()),
        f.nombre(),
        tipoDocumento(f.tipoDocumento()),
        f.numeroDocumento(),
        f.telefono(),
        f.correo(),
        f.direccion(),
        f.ciudad());
  }

  private static DatosProveedor datos(FilaProveedor f) {
    return new DatosProveedor(
        f.nombre(), f.nit(), f.telefono(), f.correo(), f.ciudad(), f.monedaHabitual());
  }

  /** "Instalador" o "Cliente final", sin distinguir mayúsculas ni tildes; null si no es válido. */
  static TipoCliente tipoCliente(String texto) {
    if (texto == null) {
      return null;
    }
    return switch (normalizar(texto).replace('_', ' ')) {
      case "instalador" -> TipoCliente.INSTALADOR;
      case "cliente final", "final" -> TipoCliente.CLIENTE_FINAL;
      default -> null;
    };
  }

  /** "CC" o "NIT"; null si está vacío o no es válido. */
  static TipoDocumento tipoDocumento(String texto) {
    if (texto == null || texto.isBlank()) {
      return null;
    }
    return switch (normalizar(texto)) {
      case "cc" -> TipoDocumento.CC;
      case "nit" -> TipoDocumento.NIT;
      default -> null;
    };
  }

  private static String normalizar(String texto) {
    return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
