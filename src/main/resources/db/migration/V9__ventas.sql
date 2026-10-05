-- Ventas de material (sección 3.12), su anulación (RF-72, RF-73) y enlaces públicos de los
-- comprobantes (RF-134).

CREATE SEQUENCE seq_venta START WITH 1;

CREATE TABLE venta (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero              BIGINT         NOT NULL,
    fecha               DATE           NOT NULL,
    cliente_id          BIGINT         NOT NULL,
    -- Copia de los datos del cliente para el comprobante (P-35).
    cliente_tipo        VARCHAR(15)    NOT NULL,
    cliente_nombre      VARCHAR(150)   NOT NULL,
    cliente_documento   VARCHAR(40),
    cliente_telefono    VARCHAR(20),
    cliente_direccion   VARCHAR(200),
    cliente_ciudad      VARCHAR(80),
    moneda              VARCHAR(3)     NOT NULL,
    -- Tasas vigentes en la fecha de la venta, guardadas con ella (RN-04).
    trm                 NUMERIC(19, 6),
    fecha_trm           DATE,
    tasa_ves            NUMERIC(19, 6),
    fecha_tasa_ves      DATE,
    subtotal            NUMERIC(19, 4) NOT NULL,
    descuento_tipo      VARCHAR(10)    NOT NULL,
    descuento_valor     NUMERIC(19, 4) NOT NULL,
    descuento           NUMERIC(19, 4) NOT NULL,
    total               NUMERIC(19, 4) NOT NULL,
    -- Costo del material al momento de la salida y utilidad (RF-68, RN-03).
    costo               NUMERIC(19, 4) NOT NULL,
    utilidad            NUMERIC(19, 4) NOT NULL,
    porcentaje_utilidad NUMERIC(9, 2),
    total_usd           NUMERIC(19, 4) NOT NULL,
    costo_usd           NUMERIC(19, 4) NOT NULL,
    utilidad_usd        NUMERIC(19, 4) NOT NULL,
    observaciones       VARCHAR(500),
    monedas_comprobante VARCHAR(20),
    -- Cotización de origen (RF-74): se llena desde la Fase 5.
    cotizacion_id       BIGINT,
    estado              VARCHAR(10)    NOT NULL DEFAULT 'ACTIVA',
    motivo_anulacion    VARCHAR(300),
    anulada_por         BIGINT,
    anulada_en          TIMESTAMPTZ,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    CONSTRAINT uq_venta_numero UNIQUE (numero),
    CONSTRAINT ck_venta_moneda CHECK (moneda IN ('USD', 'COP', 'VES')),
    CONSTRAINT ck_venta_tasa_cop CHECK (moneda <> 'COP' OR trm IS NOT NULL),
    CONSTRAINT ck_venta_tasa_ves CHECK (moneda <> 'VES' OR tasa_ves IS NOT NULL),
    CONSTRAINT ck_venta_cliente_tipo CHECK (cliente_tipo IN ('INSTALADOR', 'CLIENTE_FINAL')),
    CONSTRAINT ck_venta_descuento_tipo CHECK (descuento_tipo IN ('PORCENTAJE', 'VALOR')),
    -- P-30: descuento no negativo y nunca mayor que el subtotal.
    CONSTRAINT ck_venta_descuento CHECK (descuento_valor >= 0 AND descuento >= 0 AND descuento <= subtotal),
    CONSTRAINT ck_venta_total CHECK (subtotal >= 0 AND total = subtotal - descuento AND total_usd >= 0),
    CONSTRAINT ck_venta_costo CHECK (costo >= 0 AND costo_usd >= 0),
    CONSTRAINT ck_venta_estado CHECK (estado IN ('ACTIVA', 'ANULADA')),
    CONSTRAINT ck_venta_anulacion CHECK (estado = 'ACTIVA' OR (motivo_anulacion IS NOT NULL
        AND anulada_por IS NOT NULL AND anulada_en IS NOT NULL)),
    CONSTRAINT fk_venta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_venta_anulada_por FOREIGN KEY (anulada_por) REFERENCES usuario (id),
    CONSTRAINT fk_venta_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_venta_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_venta_fecha ON venta (fecha);
CREATE INDEX ix_venta_cliente ON venta (cliente_id);
CREATE INDEX ix_venta_estado ON venta (estado);
CREATE INDEX ix_venta_anulada_por ON venta (anulada_por);
CREATE INDEX ix_venta_created_by ON venta (created_by);
CREATE INDEX ix_venta_updated_by ON venta (updated_by);

-- Una línea por producto en cada venta.
CREATE TABLE linea_venta (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venta_id           BIGINT         NOT NULL,
    producto_id        BIGINT         NOT NULL,
    codigo             VARCHAR(30)    NOT NULL,
    descripcion        VARCHAR(150)   NOT NULL,
    unidad             VARCHAR(10)    NOT NULL,
    cantidad           NUMERIC(14, 3) NOT NULL,
    precio_unitario    NUMERIC(19, 4) NOT NULL,
    precio_sugerido    NUMERIC(19, 4) NOT NULL,
    subtotal           NUMERIC(19, 4) NOT NULL,
    -- Costo vigente del producto en el momento de la salida (RF-68).
    costo_unitario_usd NUMERIC(19, 4) NOT NULL,
    CONSTRAINT uq_linea_venta_producto UNIQUE (venta_id, producto_id),
    CONSTRAINT ck_linea_venta_cantidad CHECK (cantidad > 0),
    -- P-29: el precio puede ser 0, nunca negativo.
    CONSTRAINT ck_linea_venta_precio CHECK (precio_unitario >= 0 AND precio_sugerido >= 0 AND subtotal >= 0),
    CONSTRAINT ck_linea_venta_costo CHECK (costo_unitario_usd >= 0),
    CONSTRAINT fk_linea_venta_venta FOREIGN KEY (venta_id) REFERENCES venta (id),
    CONSTRAINT fk_linea_venta_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);
CREATE INDEX ix_linea_venta_producto ON linea_venta (producto_id);

-- Enlaces públicos de los comprobantes para WhatsApp (RF-134, P-33). Solo se guarda el SHA-256
-- del token, como en las sesiones.
CREATE TABLE enlace_comprobante (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash     VARCHAR(64)  NOT NULL,
    documento_tipo VARCHAR(20)  NOT NULL,
    documento_id   BIGINT       NOT NULL,
    vence_en       TIMESTAMPTZ  NOT NULL,
    creado_en      TIMESTAMPTZ  NOT NULL,
    creado_por     BIGINT,
    CONSTRAINT uq_enlace_comprobante_token UNIQUE (token_hash),
    CONSTRAINT ck_enlace_comprobante_tipo CHECK (documento_tipo IN ('VENTA', 'INSTALACION', 'COTIZACION')),
    CONSTRAINT fk_enlace_comprobante_creado_por FOREIGN KEY (creado_por) REFERENCES usuario (id)
);
CREATE INDEX ix_enlace_comprobante_documento ON enlace_comprobante (documento_tipo, documento_id);
CREATE INDEX ix_enlace_comprobante_creado_por ON enlace_comprobante (creado_por);

-- Nuevos movimientos del kárdex y del historial de seriales (V7 no se modifica, AG-10).
ALTER TABLE movimiento_inventario DROP CONSTRAINT ck_movimiento_tipo;
ALTER TABLE movimiento_inventario ADD CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('COMPRA',
    'ANULACION_COMPRA', 'AJUSTE_ENTRADA', 'AJUSTE_SALIDA', 'INVENTARIO_INICIAL', 'VENTA',
    'ANULACION_VENTA'));

ALTER TABLE movimiento_serial DROP CONSTRAINT ck_movimiento_serial_tipo;
ALTER TABLE movimiento_serial ADD CONSTRAINT ck_movimiento_serial_tipo CHECK (tipo IN ('ENTRADA',
    'BAJA', 'ANULACION', 'VENTA', 'ANULACION_VENTA'));
