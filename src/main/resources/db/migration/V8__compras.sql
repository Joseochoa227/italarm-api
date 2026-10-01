-- Compras a proveedores (sección 3.6, RF-39 a RF-48) y su anulación (RF-71, RF-73).

CREATE SEQUENCE seq_compra START WITH 1;

CREATE TABLE compra (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero           BIGINT         NOT NULL,
    fecha            DATE           NOT NULL,
    proveedor_id     BIGINT         NOT NULL,
    numero_factura   VARCHAR(50)    NOT NULL,
    moneda           VARCHAR(3)     NOT NULL,
    -- Tasas vigentes en la fecha de la compra, guardadas con ella (RF-32, RN-04).
    trm              NUMERIC(19, 6),
    fecha_trm        DATE,
    tasa_ves         NUMERIC(19, 6),
    fecha_tasa_ves   DATE,
    total            NUMERIC(19, 4) NOT NULL,
    total_usd        NUMERIC(19, 4) NOT NULL,
    factura_clave    VARCHAR(300),
    estado           VARCHAR(10)    NOT NULL DEFAULT 'ACTIVA',
    motivo_anulacion VARCHAR(300),
    anulada_por      BIGINT,
    anulada_en       TIMESTAMPTZ,
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by       BIGINT,
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by       BIGINT,
    CONSTRAINT uq_compra_numero UNIQUE (numero),
    CONSTRAINT ck_compra_moneda CHECK (moneda IN ('USD', 'COP', 'VES')),
    CONSTRAINT ck_compra_tasa_cop CHECK (moneda <> 'COP' OR trm IS NOT NULL),
    CONSTRAINT ck_compra_tasa_ves CHECK (moneda <> 'VES' OR tasa_ves IS NOT NULL),
    CONSTRAINT ck_compra_total CHECK (total > 0 AND total_usd > 0),
    CONSTRAINT ck_compra_estado CHECK (estado IN ('ACTIVA', 'ANULADA')),
    CONSTRAINT ck_compra_anulacion CHECK (estado = 'ACTIVA' OR (motivo_anulacion IS NOT NULL
        AND anulada_por IS NOT NULL AND anulada_en IS NOT NULL)),
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id),
    CONSTRAINT fk_compra_anulada_por FOREIGN KEY (anulada_por) REFERENCES usuario (id),
    CONSTRAINT fk_compra_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_compra_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_compra_fecha ON compra (fecha);
CREATE INDEX ix_compra_proveedor ON compra (proveedor_id);
CREATE INDEX ix_compra_estado ON compra (estado);
CREATE INDEX ix_compra_anulada_por ON compra (anulada_por);
CREATE INDEX ix_compra_created_by ON compra (created_by);
CREATE INDEX ix_compra_updated_by ON compra (updated_by);

-- Una línea por producto en cada compra (P-20).
CREATE TABLE linea_compra (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    compra_id          BIGINT         NOT NULL,
    producto_id        BIGINT         NOT NULL,
    cantidad           NUMERIC(14, 3) NOT NULL,
    costo_unitario     NUMERIC(19, 4) NOT NULL,
    costo_unitario_usd NUMERIC(19, 6) NOT NULL,
    subtotal           NUMERIC(19, 4) NOT NULL,
    -- Cambio de costo que produjo la línea (RF-41). Se llena en la misma transacción, después de
    -- bloquear el producto.
    costo_anterior_usd NUMERIC(19, 4),
    costo_nuevo_usd    NUMERIC(19, 4),
    regla              VARCHAR(20),
    CONSTRAINT uq_linea_compra_producto UNIQUE (compra_id, producto_id),
    CONSTRAINT ck_linea_compra_cantidad CHECK (cantidad > 0),
    -- P-21: el costo de una compra es mayor que 0.
    CONSTRAINT ck_linea_compra_costo CHECK (costo_unitario > 0 AND costo_unitario_usd > 0),
    CONSTRAINT ck_linea_compra_regla CHECK (regla IS NULL OR regla IN ('SUBE', 'PROMEDIO', 'SIN_STOCK')),
    CONSTRAINT fk_linea_compra_compra FOREIGN KEY (compra_id) REFERENCES compra (id),
    CONSTRAINT fk_linea_compra_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);
CREATE INDEX ix_linea_compra_producto ON linea_compra (producto_id);
