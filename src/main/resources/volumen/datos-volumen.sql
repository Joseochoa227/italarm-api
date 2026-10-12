-- Datos de volumen para medir el rendimiento en local (Fase 6, T4). Parecido a 3 años de
-- operación: 500 productos, 300 clientes, 3.000 compras, 6.000 ventas, 1.500 instalaciones,
-- 2.000 cotizaciones y 20.000 seriales. Solo para una base de pruebas desechable: el stock no
-- cuadra con el kárdex. Lo carga CargaDatosVolumen con el perfil "volumen" sobre una base vacía.

SELECT setseed(0.42);

INSERT INTO tasa_cambio (par, fecha, valor, fuente, registrada_por, registrada_en)
SELECT 'USD_COP', d::date, 3800 + (random() * 500)::int, 'SUPERFINANCIERA', NULL, d
FROM generate_series(current_date - 1095, current_date, interval '1 day') d
WHERE NOT EXISTS (SELECT 1 FROM tasa_cambio t WHERE t.par = 'USD_COP' AND t.fecha = d::date);

INSERT INTO tasa_cambio (par, fecha, valor, fuente, registrada_por, registrada_en)
SELECT 'USD_VES', d::date, 40 + (random() * 20)::int, 'MANUAL', 1, d
FROM generate_series(current_date - 1095, current_date, interval '1 day') d
WHERE NOT EXISTS (SELECT 1 FROM tasa_cambio t WHERE t.par = 'USD_VES' AND t.fecha = d::date);

INSERT INTO producto (codigo, nombre, marca, categoria_id, unidad_medida_id, controla_serial,
                      precio_instalador, precio_cliente_final, stock, costo_actual_usd, stock_minimo)
SELECT 'VOL-' || lpad(g::text, 4, '0'),
       'Producto de volumen ' || g,
       'Marca ' || (g % 12),
       ((g - 1) % 8) + 1,
       CASE WHEN ((g - 1) % 8) + 1 = 4 THEN 2 ELSE 1 END,
       ((g - 1) % 8) + 1 = 1,
       10 + (g % 70), 15 + (g % 90),
       (random() * 200)::int,
       5 + (g % 75),
       CASE WHEN g % 5 = 0 THEN 20 END
FROM generate_series(1, 500) g;

INSERT INTO cliente (tipo, nombre, tipo_documento, numero_documento, telefono, direccion, ciudad)
SELECT CASE WHEN g % 3 = 0 THEN 'INSTALADOR' ELSE 'CLIENTE_FINAL' END,
       'Cliente de volumen ' || g, 'CC', (1000000 + g)::text,
       '+57300' || lpad(g::text, 7, '0'), 'Calle ' || g, 'Cúcuta'
FROM generate_series(1, 300) g;

INSERT INTO proveedor (nombre, moneda_habitual)
SELECT 'Proveedor de volumen ' || g, 'USD' FROM generate_series(1, 20) g;

-- Mapas de números a ids reales.
CREATE TEMP TABLE vol_producto AS
SELECT row_number() OVER (ORDER BY id) AS n, id, codigo, nombre, costo_actual_usd AS costo,
       CASE WHEN unidad_medida_id = 2 THEN 'm' ELSE 'und' END AS unidad, controla_serial
FROM producto WHERE codigo LIKE 'VOL-%';
CREATE TEMP TABLE vol_cliente AS
SELECT row_number() OVER (ORDER BY id) AS n, id, tipo, nombre FROM cliente
WHERE nombre LIKE 'Cliente de volumen %';
CREATE TEMP TABLE vol_proveedor AS
SELECT row_number() OVER (ORDER BY id) AS n, id FROM proveedor
WHERE nombre LIKE 'Proveedor de volumen %';

-- Compras: 2 líneas cada una.
INSERT INTO compra (numero, fecha, proveedor_id, numero_factura, moneda, total, total_usd,
                    created_by, created_at)
SELECT g, current_date - (random() * 1095)::int, p.id, 'F-' || g, 'USD', 1, 1, 1,
       now() - (random() * 1095 || ' days')::interval
FROM generate_series(1, 3000) g JOIN vol_proveedor p ON p.n = (g % 20) + 1;

INSERT INTO linea_compra (compra_id, producto_id, cantidad, costo_unitario, costo_unitario_usd,
                          subtotal)
SELECT c.id, p.id, 10, p.costo, p.costo, 10 * p.costo
FROM compra c
JOIN LATERAL (SELECT unnest(ARRAY[(c.numero % 500) + 1, ((c.numero * 7 + 3) % 500) + 1]) AS n) x
  ON true
JOIN vol_producto p ON p.n = x.n
WHERE c.numero_factura LIKE 'F-%'
ON CONFLICT DO NOTHING;

UPDATE compra c SET total = s.total, total_usd = s.total
FROM (SELECT compra_id, sum(subtotal) AS total FROM linea_compra GROUP BY compra_id) s
WHERE s.compra_id = c.id;

-- Ventas: 2 líneas; 70 % en USD y 30 % en COP; 3 % anuladas.
INSERT INTO venta (numero, fecha, cliente_id, cliente_tipo, cliente_nombre, moneda, trm, fecha_trm,
                   subtotal, descuento_tipo, descuento_valor, descuento, total, costo, utilidad,
                   total_usd, costo_usd, utilidad_usd, estado, motivo_anulacion, anulada_por,
                   anulada_en, created_by, created_at)
SELECT g, current_date - (random() * 1095)::int, c.id, c.tipo, c.nombre,
       CASE WHEN g % 10 < 7 THEN 'USD' ELSE 'COP' END, 4000, current_date,
       0, 'VALOR', 0, 0, 0, 0, 0, 0, 0, 0,
       CASE WHEN g % 33 = 0 THEN 'ANULADA' ELSE 'ACTIVA' END,
       CASE WHEN g % 33 = 0 THEN 'Volumen' END,
       CASE WHEN g % 33 = 0 THEN 1 END,
       CASE WHEN g % 33 = 0 THEN now() END,
       (g % 2) + 1, now() - (random() * 1095 || ' days')::interval
FROM generate_series(1, 6000) g JOIN vol_cliente c ON c.n = (g % 300) + 1;

INSERT INTO linea_venta (venta_id, producto_id, codigo, descripcion, unidad, cantidad,
                         precio_unitario, precio_sugerido, subtotal, costo_unitario_usd)
SELECT v.id, p.id, p.codigo, p.nombre, p.unidad, 2,
       CASE v.moneda WHEN 'USD' THEN p.costo * 1.5 ELSE p.costo * 1.5 * 4000 END,
       CASE v.moneda WHEN 'USD' THEN p.costo * 1.5 ELSE p.costo * 1.5 * 4000 END,
       2 * CASE v.moneda WHEN 'USD' THEN p.costo * 1.5 ELSE p.costo * 1.5 * 4000 END,
       p.costo
FROM venta v
JOIN LATERAL (SELECT unnest(ARRAY[(v.numero % 500) + 1, ((v.numero * 11 + 5) % 500) + 1]) AS n) x
  ON true
JOIN vol_producto p ON p.n = x.n
ON CONFLICT DO NOTHING;

UPDATE venta v SET subtotal = s.subtotal, total = s.subtotal,
       costo = CASE v.moneda WHEN 'USD' THEN s.costo ELSE s.costo * 4000 END,
       utilidad = s.subtotal - CASE v.moneda WHEN 'USD' THEN s.costo ELSE s.costo * 4000 END,
       total_usd = CASE v.moneda WHEN 'USD' THEN s.subtotal ELSE round(s.subtotal / 4000, 4) END,
       costo_usd = s.costo,
       utilidad_usd = CASE v.moneda WHEN 'USD' THEN s.subtotal ELSE round(s.subtotal / 4000, 4) END
                      - s.costo
FROM (SELECT venta_id, sum(subtotal) AS subtotal, sum(cantidad * costo_unitario_usd) AS costo
      FROM linea_venta GROUP BY venta_id) s
WHERE s.venta_id = v.id;

-- Instalaciones: 2 líneas y mano de obra, en USD.
INSERT INTO instalacion (numero, fecha, cliente_id, cliente_tipo, cliente_nombre, direccion,
                         descripcion, moneda, material, mano_obra, subtotal, descuento_tipo,
                         descuento_valor, descuento, total, costo, utilidad, total_usd, costo_usd,
                         utilidad_usd, garantia_mano_obra_meses, vence_mano_obra, vence_equipos,
                         condiciones_garantia, created_by, created_at)
SELECT g, f, c.id, c.tipo, c.nombre, 'Calle ' || g, 'Instalación de volumen ' || g, 'USD',
       0, 100, 100, 'VALOR', 0, 0, 100, 0, 100, 100, 0, 100, 3,
       f + interval '3 months', f + interval '3 months', 'Condiciones', (g % 2) + 1,
       now() - (random() * 1095 || ' days')::interval
FROM (SELECT g, current_date - (random() * 1095)::int AS f FROM generate_series(1, 1500) g) d
JOIN vol_cliente c ON c.n = (g % 300) + 1;

INSERT INTO tecnico_instalacion (instalacion_id, usuario_id)
SELECT id, (numero % 2) + 1 FROM instalacion;

INSERT INTO linea_instalacion (instalacion_id, producto_id, codigo, descripcion, unidad, cantidad,
                               precio_unitario, precio_sugerido, subtotal, costo_unitario_usd)
SELECT i.id, p.id, p.codigo, p.nombre, p.unidad, 4, p.costo * 1.5, p.costo * 1.5,
       4 * p.costo * 1.5, p.costo
FROM instalacion i
JOIN LATERAL (SELECT unnest(ARRAY[(i.numero % 500) + 1, ((i.numero * 13 + 7) % 500) + 1]) AS n) x
  ON true
JOIN vol_producto p ON p.n = x.n
ON CONFLICT DO NOTHING;

UPDATE instalacion i SET material = s.material, subtotal = s.material + 100,
       total = s.material + 100, costo = s.costo, utilidad = s.material + 100 - s.costo,
       total_usd = s.material + 100, costo_usd = s.costo, utilidad_usd = s.material + 100 - s.costo
FROM (SELECT instalacion_id, sum(subtotal) AS material, sum(cantidad * costo_unitario_usd) AS costo
      FROM linea_instalacion GROUP BY instalacion_id) s
WHERE s.instalacion_id = i.id;

-- Cotizaciones: 2 líneas, estados variados (sin convertidas).
INSERT INTO cotizacion (numero, tipo, fecha, validez_dias, vence, cliente_id, cliente_tipo,
                        cliente_nombre, moneda, material, mano_obra, subtotal, descuento_tipo,
                        descuento_valor, descuento, total, costo, utilidad, total_usd, costo_usd,
                        utilidad_usd, estado, created_by, created_at)
SELECT g, 'VENTA', f, 15, f + 15, c.id, c.tipo, c.nombre, 'USD', 100, 0, 100, 'VALOR', 0, 0, 100,
       60, 40, 100, 60, 40,
       (ARRAY['BORRADOR', 'EN_EVALUACION', 'APROBADA', 'RECHAZADA', 'VENCIDA'])[(g % 5) + 1],
       1, now() - (random() * 1095 || ' days')::interval
FROM (SELECT g, current_date - (random() * 1095)::int AS f FROM generate_series(1, 2000) g) d
JOIN vol_cliente c ON c.n = (g % 300) + 1;

INSERT INTO linea_cotizacion (cotizacion_id, producto_id, codigo, descripcion, unidad, cantidad,
                              precio_unitario, precio_sugerido, subtotal, costo_unitario_usd)
SELECT k.id, p.id, p.codigo, p.nombre, p.unidad, 1, 50, 50, 50, 30
FROM cotizacion k
JOIN LATERAL (SELECT unnest(ARRAY[(k.numero % 500) + 1, ((k.numero * 17 + 1) % 500) + 1]) AS n) x
  ON true
JOIN vol_producto p ON p.n = x.n
ON CONFLICT DO NOTHING;

-- Seriales de las cámaras: 40 % en bodega, 40 % vendidos y 20 % instalados.
INSERT INTO serial (producto_id, numero, estado, fecha_entrada, entrada_tipo, entrada_id,
                    entrada_consecutivo, salida_tipo, salida_id, salida_consecutivo,
                    vencimiento_garantia)
SELECT p.id, 'SN-VOL-' || g,
       CASE WHEN g % 5 < 2 THEN 'EN_BODEGA' WHEN g % 5 < 4 THEN 'VENDIDO' ELSE 'INSTALADO' END,
       current_date - (g % 1095), 'COMPRA', 1, 'C-0001',
       CASE WHEN g % 5 < 2 THEN NULL WHEN g % 5 < 4 THEN 'VENTA' ELSE 'INSTALACION' END,
       CASE WHEN g % 5 < 2 THEN NULL
            WHEN g % 5 < 4 THEN (SELECT id FROM venta WHERE numero = (g % 6000) + 1)
            ELSE (SELECT id FROM instalacion WHERE numero = (g % 1500) + 1) END,
       CASE WHEN g % 5 < 2 THEN NULL
            WHEN g % 5 < 4 THEN 'V-' || lpad(((g % 6000) + 1)::text, 4, '0')
            ELSE 'I-' || lpad(((g % 1500) + 1)::text, 4, '0') END,
       CASE WHEN g % 5 < 2 THEN NULL ELSE current_date - (g % 1095) + 90 END
FROM generate_series(1, 20000) g
JOIN (SELECT row_number() OVER (ORDER BY id) AS n, id FROM vol_producto WHERE controla_serial) p
  ON p.n = (g % (SELECT count(*) FROM vol_producto WHERE controla_serial)) + 1;

-- Kárdex de compras, ventas e instalaciones.
INSERT INTO movimiento_inventario (producto_id, fecha, registrado_en, tipo, documento_tipo,
                                   documento_id, documento_consecutivo, entrada, salida, saldo,
                                   costo_unitario_usd, usuario_id)
SELECT l.producto_id, c.fecha, c.created_at, 'COMPRA', 'COMPRA', c.id,
       'C-' || lpad(c.numero::text, 4, '0'), l.cantidad, 0, 100, l.costo_unitario_usd, 1
FROM linea_compra l JOIN compra c ON c.id = l.compra_id
UNION ALL
SELECT l.producto_id, v.fecha, v.created_at, 'VENTA', 'VENTA', v.id,
       'V-' || lpad(v.numero::text, 4, '0'), 0, l.cantidad, 50, l.costo_unitario_usd, 1
FROM linea_venta l JOIN venta v ON v.id = l.venta_id
UNION ALL
SELECT l.producto_id, i.fecha, i.created_at, 'INSTALACION', 'INSTALACION', i.id,
       'I-' || lpad(i.numero::text, 4, '0'), 0, l.cantidad, 50, l.costo_unitario_usd, 1
FROM linea_instalacion l JOIN instalacion i ON i.id = l.instalacion_id;

SELECT setval('seq_compra', (SELECT max(numero) FROM compra));
SELECT setval('seq_venta', (SELECT max(numero) FROM venta));
SELECT setval('seq_instalacion', (SELECT max(numero) FROM instalacion));
SELECT setval('seq_cotizacion', (SELECT max(numero) FROM cotizacion));

ANALYZE;
