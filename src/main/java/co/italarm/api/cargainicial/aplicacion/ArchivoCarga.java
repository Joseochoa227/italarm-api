package co.italarm.api.cargainicial.aplicacion;

import co.italarm.api.catalogo.aplicacion.FilaProducto;
import co.italarm.api.inventario.aplicacion.FilaInventario;
import co.italarm.api.shared.dominio.ErrorCarga;
import co.italarm.api.terceros.aplicacion.FilaCliente;
import co.italarm.api.terceros.aplicacion.FilaProveedor;
import java.util.List;

/**
 * Contenido leído del Excel de la carga inicial.
 *
 * @param errores errores de formato (por ejemplo, un número que no lo es); esas filas no se validan
 *     más
 */
public record ArchivoCarga(
    List<FilaProducto> productos,
    List<FilaInventario> inventario,
    List<FilaCliente> clientes,
    List<FilaProveedor> proveedores,
    List<ErrorCarga> errores) {}
