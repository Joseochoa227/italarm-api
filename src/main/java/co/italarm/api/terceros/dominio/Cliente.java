package co.italarm.api.terceros.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cliente: instalador o cliente final (sección 3.10). No se elimina (P-11). */
@Entity
@Table(name = "cliente")
public class Cliente extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 20)
  private TipoCliente tipo;

  @Column(name = "nombre", nullable = false, length = 150)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento", length = 3)
  private TipoDocumento tipoDocumento;

  @Column(name = "numero_documento", length = 30)
  private String numeroDocumento;

  @Column(name = "telefono", nullable = false, length = 20)
  private String telefono;

  @Column(name = "correo", length = 254)
  private String correo;

  @Column(name = "direccion", length = 200)
  private String direccion;

  @Column(name = "ciudad", length = 80)
  private String ciudad;

  protected Cliente() {}

  public static Cliente crear(DatosCliente datos) {
    Cliente cliente = new Cliente();
    cliente.actualizar(datos);
    return cliente;
  }

  public void actualizar(DatosCliente datos) {
    String numero = Documentos.normalizar(datos.numeroDocumento());
    if ((datos.tipoDocumento() == null) != (numero == null)) {
      throw new DocumentoIncompletoException();
    }
    this.tipo = datos.tipo();
    this.nombre = Textos.limpiar(datos.nombre());
    this.tipoDocumento = datos.tipoDocumento();
    this.numeroDocumento = numero;
    this.telefono = Telefono.normalizar(datos.telefono());
    this.correo = Textos.limpiar(datos.correo());
    this.direccion = Textos.limpiar(datos.direccion());
    this.ciudad = Textos.limpiar(datos.ciudad());
  }

  public PrecioAplicado precioAplicado() {
    return tipo.precio();
  }

  public Long getId() {
    return id;
  }

  public TipoCliente getTipo() {
    return tipo;
  }

  public String getNombre() {
    return nombre;
  }

  public TipoDocumento getTipoDocumento() {
    return tipoDocumento;
  }

  public String getNumeroDocumento() {
    return numeroDocumento;
  }

  public String getTelefono() {
    return telefono;
  }

  public String getCorreo() {
    return correo;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getCiudad() {
    return ciudad;
  }
}
