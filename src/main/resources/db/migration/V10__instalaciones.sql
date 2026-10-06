-- Instalaciones (sección 3.13), fotos, garantías y reclamos (sección 3.14).

CREATE SEQUENCE seq_instalacion START WITH 1;

CREATE TABLE instalacion (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero                   BIGINT         NOT NULL,
    fecha                    DATE           NOT NULL,
    cliente_id               BIGINT         NOT NULL,
    -- Copia de los datos del cliente para el comprobante (P-35).
    cliente_tipo             VARCHAR(15)    NOT NULL,
    cliente_nombre           VARCHAR(150)   NOT NULL,
    cliente_documento        VARCHAR(40),
    cliente_telefono         VARCHAR(20),
    cliente_direccion        VARCHAR(200),
    cliente_ciudad           VARCHAR(80),
    direccion                VARCHAR(200)   NOT NULL,
    descripcion              VARCHAR(2000)  NOT NULL,
    moneda                   VARCHAR(3)     NOT NULL,
    -- Tasas de la fecha de la instalación (RN-04, P-38).
    trm                      NUMERIC(19, 6),
    fecha_trm                DATE,
    tasa_ves                 NUMERIC(19, 6),
    fecha_tasa_ves           DATE,
    material                 NUMERIC(19, 4) NOT NULL,
    mano_obra                NUMERIC(19, 4) NOT NULL,
    subtotal                 NUMERIC(19, 4) NOT NULL,
    descuento_tipo           VARCHAR(10)    NOT NULL,
    descuento_valor          NUMERIC(19, 4) NOT NULL,
    descuento                NUMERIC(19, 4) NOT NULL,
    total                    NUMERIC(19, 4) NOT NULL,
    costo                    NUMERIC(19, 4) NOT NULL,
    utilidad                 NUMERIC(19, 4) NOT NULL,
    porcentaje_utilidad      NUMERIC(9, 2),
    total_usd                NUMERIC(19, 4) NOT NULL,
    costo_usd                NUMERIC(19, 4) NOT NULL,
    utilidad_usd             NUMERIC(19, 4) NOT NULL,
    -- Garantías (RF-113, RF-114).
    garantia_mano_obra_meses INTEGER        NOT NULL,
    vence_mano_obra          DATE           NOT NULL,
    vence_equipos            DATE           NOT NULL,
    condiciones_garantia     VARCHAR(2000)  NOT NULL,
    observaciones            VARCHAR(500),
    monedas_comprobante      VARCHAR(20),
    -- Cotización de origen (RF-74): se llena desde la Fase 5.
    cotizacion_id            BIGINT,
    estado                   VARCHAR(10)    NOT NULL DEFAULT 'ACTIVA',
    motivo_anulacion         VARCHAR(300),
    anulada_por              BIGINT,
    anulada_en               TIMESTAMPTZ,
    version                  BIGINT         NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by               BIGINT,
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by               BIGINT,
    CONSTRAINT uq_instalacion_numero UNIQUE (numero),
    CONSTRAINT ck_instalacion_moneda CHECK (moneda IN ('USD', 'COP', 'VES')),
    CONSTRAINT ck_instalacion_tasa_cop CHECK (moneda <> 'COP' OR trm IS NOT NULL),
    CONSTRAINT ck_instalacion_tasa_ves CHECK (moneda <> 'VES' OR tasa_ves IS NOT NULL),
    CONSTRAINT ck_instalacion_cliente_tipo CHECK (cliente_tipo IN ('INSTALADOR', 'CLIENTE_FINAL')),
    CONSTRAINT ck_instalacion_descuento_tipo CHECK (descuento_tipo IN ('PORCENTAJE', 'VALOR')),
    CONSTRAINT ck_instalacion_descuento CHECK (descuento_valor >= 0 AND descuento >= 0 AND descuento <= subtotal),
    -- P-41: al menos material o mano de obra.
    CONSTRAINT ck_instalacion_valores CHECK (material >= 0 AND mano_obra >= 0
        AND subtotal = material + mano_obra AND total = subtotal - descuento AND total_usd >= 0),
    CONSTRAINT ck_instalacion_costo CHECK (costo >= 0 AND costo_usd >= 0),
    -- P-39: de 1 a 3 meses.
    CONSTRAINT ck_instalacion_garantia CHECK (garantia_mano_obra_meses BETWEEN 1 AND 3
        AND vence_mano_obra > fecha AND vence_equipos > fecha),
    CONSTRAINT ck_instalacion_estado CHECK (estado IN ('ACTIVA', 'ANULADA')),
    CONSTRAINT ck_instalacion_anulacion CHECK (estado = 'ACTIVA' OR (motivo_anulacion IS NOT NULL
        AND anulada_por IS NOT NULL AND anulada_en IS NOT NULL)),
    CONSTRAINT fk_instalacion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_instalacion_anulada_por FOREIGN KEY (anulada_por) REFERENCES usuario (id),
    CONSTRAINT fk_instalacion_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_instalacion_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_instalacion_fecha ON instalacion (fecha);
CREATE INDEX ix_instalacion_cliente ON instalacion (cliente_id);
CREATE INDEX ix_instalacion_estado ON instalacion (estado);
CREATE INDEX ix_instalacion_vence_mano_obra ON instalacion (vence_mano_obra);
CREATE INDEX ix_instalacion_anulada_por ON instalacion (anulada_por);
CREATE INDEX ix_instalacion_created_by ON instalacion (created_by);
CREATE INDEX ix_instalacion_updated_by ON instalacion (updated_by);

-- Material usado: una línea por producto.
CREATE TABLE linea_instalacion (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instalacion_id     BIGINT         NOT NULL,
    producto_id        BIGINT         NOT NULL,
    codigo             VARCHAR(30)    NOT NULL,
    descripcion        VARCHAR(150)   NOT NULL,
    unidad             VARCHAR(10)    NOT NULL,
    cantidad           NUMERIC(14, 3) NOT NULL,
    precio_unitario    NUMERIC(19, 4) NOT NULL,
    precio_sugerido    NUMERIC(19, 4) NOT NULL,
    subtotal           NUMERIC(19, 4) NOT NULL,
    costo_unitario_usd NUMERIC(19, 4) NOT NULL,
    CONSTRAINT uq_linea_instalacion_producto UNIQUE (instalacion_id, producto_id),
    CONSTRAINT ck_linea_instalacion_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_linea_instalacion_precio CHECK (precio_unitario >= 0 AND precio_sugerido >= 0 AND subtotal >= 0),
    CONSTRAINT ck_linea_instalacion_costo CHECK (costo_unitario_usd >= 0),
    CONSTRAINT fk_linea_instalacion_instalacion FOREIGN KEY (instalacion_id) REFERENCES instalacion (id),
    CONSTRAINT fk_linea_instalacion_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);
CREATE INDEX ix_linea_instalacion_producto ON linea_instalacion (producto_id);

-- Técnicos que hicieron la instalación (P-37): usuarios del sistema.
CREATE TABLE tecnico_instalacion (
    instalacion_id BIGINT NOT NULL,
    usuario_id     BIGINT NOT NULL,
    CONSTRAINT pk_tecnico_instalacion PRIMARY KEY (instalacion_id, usuario_id),
    CONSTRAINT fk_tecnico_instalacion_instalacion FOREIGN KEY (instalacion_id) REFERENCES instalacion (id),
    CONSTRAINT fk_tecnico_instalacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX ix_tecnico_instalacion_usuario ON tecnico_instalacion (usuario_id);

-- Fotos por grupo (RF-110). El archivo vive en el almacenamiento, nunca en la base de datos.
CREATE TABLE foto_instalacion (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instalacion_id BIGINT       NOT NULL,
    grupo          VARCHAR(10)  NOT NULL,
    clave          VARCHAR(300) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     BIGINT,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     BIGINT,
    CONSTRAINT ck_foto_instalacion_grupo CHECK (grupo IN ('ANTES', 'DURANTE', 'DESPUES')),
    CONSTRAINT fk_foto_instalacion_instalacion FOREIGN KEY (instalacion_id) REFERENCES instalacion (id),
    CONSTRAINT fk_foto_instalacion_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_foto_instalacion_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_foto_instalacion_instalacion ON foto_instalacion (instalacion_id, grupo);
CREATE INDEX ix_foto_instalacion_created_by ON foto_instalacion (created_by);
CREATE INDEX ix_foto_instalacion_updated_by ON foto_instalacion (updated_by);

-- Reclamos de garantía sobre una instalación o un serial (RF-125, P-45). No se borran.
CREATE TABLE reclamo_garantia (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instalacion_id BIGINT,
    serial_id      BIGINT,
    cliente_id     BIGINT        NOT NULL,
    fecha          DATE          NOT NULL,
    problema       VARCHAR(2000) NOT NULL,
    solucion       VARCHAR(2000),
    en_garantia    BOOLEAN       NOT NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     BIGINT,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by     BIGINT,
    CONSTRAINT ck_reclamo_garantia_objeto CHECK ((instalacion_id IS NULL) <> (serial_id IS NULL)),
    CONSTRAINT fk_reclamo_garantia_instalacion FOREIGN KEY (instalacion_id) REFERENCES instalacion (id),
    CONSTRAINT fk_reclamo_garantia_serial FOREIGN KEY (serial_id) REFERENCES serial (id),
    CONSTRAINT fk_reclamo_garantia_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_reclamo_garantia_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_reclamo_garantia_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_reclamo_garantia_instalacion ON reclamo_garantia (instalacion_id);
CREATE INDEX ix_reclamo_garantia_serial ON reclamo_garantia (serial_id);
CREATE INDEX ix_reclamo_garantia_cliente ON reclamo_garantia (cliente_id);
CREATE INDEX ix_reclamo_garantia_created_by ON reclamo_garantia (created_by);
CREATE INDEX ix_reclamo_garantia_updated_by ON reclamo_garantia (updated_by);
CREATE INDEX ix_serial_salida ON serial (salida_tipo, salida_id);

-- Nuevos movimientos del kárdex y del historial de seriales (V7 y V9 no se modifican, AG-10).
ALTER TABLE movimiento_inventario DROP CONSTRAINT ck_movimiento_tipo;
ALTER TABLE movimiento_inventario ADD CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('COMPRA',
    'ANULACION_COMPRA', 'AJUSTE_ENTRADA', 'AJUSTE_SALIDA', 'INVENTARIO_INICIAL', 'VENTA',
    'ANULACION_VENTA', 'INSTALACION', 'ANULACION_INSTALACION'));

ALTER TABLE movimiento_serial ALTER COLUMN tipo TYPE VARCHAR(25);
ALTER TABLE movimiento_serial DROP CONSTRAINT ck_movimiento_serial_tipo;
ALTER TABLE movimiento_serial ADD CONSTRAINT ck_movimiento_serial_tipo CHECK (tipo IN ('ENTRADA',
    'BAJA', 'ANULACION', 'VENTA', 'ANULACION_VENTA', 'INSTALACION', 'ANULACION_INSTALACION'));

-- Todas las garantías vigentes o pasadas de documentos no anulados (RF-123, RF-124): la mano de
-- obra de cada instalación y cada equipo con serial vendido o instalado. Es solo de lectura.
CREATE VIEW garantia AS
SELECT 'INSTALACION' AS tipo,
       'MANO_OBRA'   AS clase,
       i.id          AS documento_id,
       i.numero      AS documento_numero,
       i.fecha       AS fecha,
       i.cliente_id  AS cliente_id,
       i.cliente_nombre AS cliente_nombre,
       CAST(NULL AS BIGINT)       AS serial_id,
       CAST(NULL AS VARCHAR(80))  AS serial_numero,
       CAST(NULL AS BIGINT)       AS producto_id,
       CAST(NULL AS VARCHAR(150)) AS producto_nombre,
       i.vence_mano_obra AS vencimiento
  FROM instalacion i
 WHERE i.estado = 'ACTIVA'
UNION ALL
SELECT 'INSTALACION', 'EQUIPO', i.id, i.numero, i.fecha, i.cliente_id,
       i.cliente_nombre, s.id, s.numero, p.id, p.nombre, s.vencimiento_garantia
  FROM serial s
  JOIN instalacion i ON s.salida_tipo = 'INSTALACION' AND s.salida_id = i.id
  JOIN producto p ON p.id = s.producto_id
 WHERE s.estado = 'INSTALADO' AND i.estado = 'ACTIVA'
UNION ALL
SELECT 'VENTA', 'EQUIPO', v.id, v.numero, v.fecha, v.cliente_id,
       v.cliente_nombre, s.id, s.numero, p.id, p.nombre, s.vencimiento_garantia
  FROM serial s
  JOIN venta v ON s.salida_tipo = 'VENTA' AND s.salida_id = v.id
  JOIN producto p ON p.id = s.producto_id
 WHERE s.estado = 'VENDIDO' AND v.estado = 'ACTIVA';
