package co.italarm.api.inventario.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.italarm.api.soporte.PruebaInventario;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventarioApiIntegracionTest extends PruebaInventario {

  private long proveedor;

  @BeforeEach
  void preparar() {
    proveedor = crearProveedor("Distribuidora Andina");
  }

  private void ajustar(long productoId, String motivo, String cantidad, List<String> seriales)
      throws Exception {
    Map<String, Object> ajuste = new HashMap<>();
    ajuste.put("productoId", productoId);
    ajuste.put("motivo", motivo);
    ajuste.put("cantidad", cantidad);
    ajuste.put("seriales", seriales);
    enviar(post("/api/v1/ajustes"), ajuste).andExpect(status().isCreated());
  }

  @Test
  void listadoValorizado_conTotalesEnLasTresMonedasYBusquedaPorSerial() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, true);
    long cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    crearProducto("SIN-STOCK", CATEGORIA_CAMARAS, UNIDAD, false);
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("HK-001", "HK-002")));
    comprar(proveedor, "USD", linea(cable, "100.5", "0.30"));

    sinCuerpo(get("/api/v1/inventario"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalProductos").value(3))
        .andExpect(jsonPath("$.valorTotal.usd.monto").value("70.1500"))
        .andExpect(jsonPath("$.valorTotal.cop.monto").value("280600.000000"))
        .andExpect(jsonPath("$.valorTotal.ves.monto").value("3507.500000"))
        .andExpect(jsonPath("$.avisos", hasSize(0)))
        .andExpect(jsonPath("$.productos.contenido", hasSize(3)))
        .andExpect(jsonPath("$.productos.contenido[0].codigo").value("CAM-1"))
        .andExpect(jsonPath("$.productos.contenido[0].stock").value("2"))
        .andExpect(jsonPath("$.productos.contenido[0].costoActualUsd.monto").value("20.0000"))
        .andExpect(jsonPath("$.productos.contenido[0].valorEnBodega.usd.monto").value("40.0000"))
        .andExpect(jsonPath("$.productos.contenido[1].costoActualUsd").value(nullValue()));

    sinCuerpo(get("/api/v1/inventario").param("buscar", "hk-00"))
        .andExpect(jsonPath("$.productos.contenido", hasSize(1)))
        .andExpect(jsonPath("$.productos.contenido[0].codigo").value("CAM-1"))
        .andExpect(jsonPath("$.totalProductos").value(1));
    sinCuerpo(get("/api/v1/inventario").param("categoriaId", String.valueOf(CATEGORIA_CABLE)))
        .andExpect(jsonPath("$.productos.contenido", hasSize(1)))
        .andExpect(jsonPath("$.valorTotal.usd.monto").value("30.1500"));
  }

  @Test
  void sinTasaDelBolivar_elEquivalenteQuedaVacioConAviso() throws Exception {
    jdbc.update("delete from tasa_cambio where par = 'USD_VES'");
    registrarTasa("USD_COP", HOY.minusDays(1), "4100");
    jdbc.update("delete from tasa_cambio where par = 'USD_COP' and fecha = ?", HOY);
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
    comprar(proveedor, "USD", linea(camara, "1", "10"));

    sinCuerpo(get("/api/v1/inventario"))
        .andExpect(jsonPath("$.valorTotal.cop.monto").value("41000.000000"))
        .andExpect(jsonPath("$.valorTotal.ves").value(nullValue()))
        .andExpect(jsonPath("$.avisos", hasSize(2)))
        .andExpect(jsonPath("$.avisos[0]", containsString("30/09/2026")))
        .andExpect(jsonPath("$.avisos[1]", containsString("bolívar")));
  }

  @Test
  void detalle_kardex_historialDeCostoYSeriales() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("S1", "S2")));
    comprar(proveedor, "COP", linea(camara, "1", "100000", List.of("S3")));
    ajustar(camara, "DANO", "-1", List.of("S2"));

    sinCuerpo(get("/api/v1/inventario/productos/{id}", camara))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value("2"))
        .andExpect(jsonPath("$.costoActual.usd.monto").value("25.0000"))
        .andExpect(jsonPath("$.costoActual.cop.monto").value("100000.000000"))
        .andExpect(jsonPath("$.valorEnBodega.usd.monto").value("50.0000"))
        .andExpect(jsonPath("$.precioInstalador.usd.monto").value("30.0000"))
        .andExpect(jsonPath("$.seriales.enBodega").value(2))
        .andExpect(jsonPath("$.seriales.dadosDeBaja").value(1));

    sinCuerpo(get("/api/v1/inventario/productos/{id}/kardex", camara))
        .andExpect(jsonPath("$.contenido", hasSize(3)))
        .andExpect(jsonPath("$.contenido[0].tipo").value("AJUSTE_SALIDA"))
        .andExpect(jsonPath("$.contenido[0].detalle").value("Daño"))
        .andExpect(jsonPath("$.contenido[0].documento.consecutivo").value("AJ-001"))
        .andExpect(jsonPath("$.contenido[0].salida").value("1"))
        .andExpect(jsonPath("$.contenido[0].saldo").value("2"))
        .andExpect(jsonPath("$.contenido[0].usuario").value("Jose"))
        .andExpect(jsonPath("$.contenido[2].tipoEtiqueta").value("Compra"))
        .andExpect(jsonPath("$.contenido[2].documento.consecutivo").value("C-0001"));

    sinCuerpo(get("/api/v1/inventario/productos/{id}/historial-costo", camara))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].regla").value("SUBE"))
        .andExpect(jsonPath("$[0].costoFactura.moneda").value("COP"))
        .andExpect(jsonPath("$[0].tasaFactura").value("4000.000000"))
        .andExpect(jsonPath("$[0].costoFacturaUsd.monto").value("25.0000"))
        .andExpect(jsonPath("$[1].regla").value("SIN_STOCK"))
        .andExpect(jsonPath("$[1].costoAnteriorUsd").value(nullValue()));

    sinCuerpo(get("/api/v1/inventario/productos/{id}/seriales", camara))
        .andExpect(jsonPath("$", hasSize(3)));
    sinCuerpo(
            get("/api/v1/inventario/productos/{id}/seriales", camara).param("estado", "EN_BODEGA"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].numero").value("S1"));

    sinCuerpo(get("/api/v1/inventario/productos/999999")).andExpect(status().isNotFound());
    sinCuerpo(get("/api/v1/inventario/productos/999999/kardex")).andExpect(status().isNotFound());
  }

  @Test
  void buscarSerialYSuHistorial() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, true);
    comprar(proveedor, "USD", linea(camara, "2", "20", List.of("ABC-1", "XYZ-9")));
    ajustar(camara, "GARANTIA", "-1", List.of("ABC-1"));

    JsonNode encontrados = leer(sinCuerpo(get("/api/v1/seriales").param("numero", " abc")));
    assertThat(encontrados).hasSize(1);
    assertThat(encontrados.at("/0/estado").asText()).isEqualTo("DADO_DE_BAJA");
    assertThat(encontrados.at("/0/documentoEntrada/consecutivo").asText()).isEqualTo("C-0001");
    assertThat(encontrados.at("/0/documentoSalida/consecutivo").asText()).isEqualTo("AJ-001");

    sinCuerpo(get("/api/v1/seriales/{id}", encontrados.at("/0/id").asLong()))
        .andExpect(jsonPath("$.serial.producto.codigo").value("CAM-1"))
        .andExpect(jsonPath("$.movimientos", hasSize(2)))
        .andExpect(jsonPath("$.movimientos[0].tipo").value("ENTRADA"))
        .andExpect(
            jsonPath("$.movimientos[0].detalle", containsString("Proveedor: Distribuidora Andina")))
        .andExpect(jsonPath("$.movimientos[1].tipo").value("BAJA"))
        .andExpect(jsonPath("$.movimientos[1].usuario").value("Jose"));

    sinCuerpo(get("/api/v1/seriales").param("numero", " ")).andExpect(jsonPath("$", hasSize(0)));
    sinCuerpo(get("/api/v1/seriales/999999")).andExpect(status().isNotFound());
  }

  /** P-17 y RF-14: con movimientos, el producto no se elimina ni cambia su serial o su unidad. */
  @Test
  void productoConMovimientos_noSeEliminaNiCambiaSerialOUnidad() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, false);
    comprar(proveedor, "USD", linea(camara, "1", "20"));

    sinCuerpo(delete("/api/v1/productos/{id}", camara))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("PRODUCTO_CON_MOVIMIENTOS"));
    Map<String, Object> cambio = new HashMap<>();
    cambio.put("codigo", "CAM-1");
    cambio.put("nombre", "Cámara");
    cambio.put("categoriaId", CATEGORIA_CAMARAS);
    cambio.put("unidadMedidaId", UNIDAD);
    cambio.put("controlaSerial", true);
    cambio.put("precioInstalador", "30.00");
    cambio.put("precioClienteFinal", "40.00");
    cambio.put("version", 0);
    enviar(put("/api/v1/productos/{id}", camara), cambio)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("PRODUCTO_CAMBIO_NO_PERMITIDO"));
  }

  /** BP-10: el stock de cada producto es la suma de entradas menos salidas de su kárdex. */
  @Test
  void elStockEsIgualALaSumaDelKardex() throws Exception {
    long camara = crearProducto("CAM-1", CATEGORIA_CAMARAS, UNIDAD, true);
    long cable = crearProducto("UTP", CATEGORIA_CABLE, METRO, false);
    comprar(proveedor, "USD", linea(camara, "3", "20", List.of("A", "B", "C")));
    comprar(proveedor, "COP", linea(cable, "305", "1200"));
    ajustar(camara, "DANO", "-1", List.of("A"));
    ajustar(cable, "CONTEO_FISICO", "2.5", List.of());
    JsonNode ultima = comprar(proveedor, "USD", linea(cable, "100", "0.25"));
    enviar(post("/api/v1/compras/{id}/anular", ultima.get("id").asLong()), Map.of("motivo", "X"))
        .andExpect(status().isOk());
    ajustar(cable, "PERDIDA", "-7.25", List.of());

    for (long producto : List.of(camara, cable)) {
      Map<String, Object> kardex =
          jdbc.queryForMap(
              "select coalesce(sum(entrada), 0) - coalesce(sum(salida), 0) as neto,"
                  + " (select saldo from movimiento_inventario where producto_id = ?"
                  + " order by id desc limit 1) as ultimo"
                  + " from movimiento_inventario where producto_id = ?",
              producto,
              producto);
      BigDecimal stock = stock(producto);
      assertThat((BigDecimal) kardex.get("neto")).isEqualByComparingTo(stock);
      assertThat((BigDecimal) kardex.get("ultimo")).isEqualByComparingTo(stock);
    }
    assertThat(stock(camara)).isEqualByComparingTo("2");
    assertThat(stock(cable)).isEqualByComparingTo("300.25");
  }
}
