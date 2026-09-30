-- Catálogo: categorías, unidades de medida y productos (sección 3.3, RF-14 a RF-18, RF-148).

CREATE TABLE categoria (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(80) NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT,
    CONSTRAINT ck_categoria_nombre CHECK (length(trim(nombre)) > 0),
    CONSTRAINT fk_categoria_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_categoria_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE UNIQUE INDEX uq_categoria_nombre ON categoria (lower(nombre));
CREATE INDEX ix_categoria_created_by ON categoria (created_by);
CREATE INDEX ix_categoria_updated_by ON categoria (updated_by);

-- admite_decimales: Metro admite hasta 2 decimales; Unidad y Par, solo enteros (P-09).
CREATE TABLE unidad_medida (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            VARCHAR(40) NOT NULL,
    abreviatura       VARCHAR(10) NOT NULL,
    admite_decimales  BOOLEAN     NOT NULL DEFAULT FALSE,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        BIGINT,
    CONSTRAINT ck_unidad_medida_nombre CHECK (length(trim(nombre)) > 0),
    CONSTRAINT ck_unidad_medida_abreviatura CHECK (length(trim(abreviatura)) > 0),
    CONSTRAINT fk_unidad_medida_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_unidad_medida_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE UNIQUE INDEX uq_unidad_medida_nombre ON unidad_medida (lower(nombre));
CREATE UNIQUE INDEX uq_unidad_medida_abreviatura ON unidad_medida (lower(abreviatura));
CREATE INDEX ix_unidad_medida_created_by ON unidad_medida (created_by);
CREATE INDEX ix_unidad_medida_updated_by ON unidad_medida (updated_by);

-- stock y costo_actual_usd los mueve el inventario desde la Fase 2; al crear, stock 0 y sin costo (RF-16).
CREATE TABLE producto (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo               VARCHAR(30)    NOT NULL,
    nombre               VARCHAR(150)   NOT NULL,
    marca                VARCHAR(80),
    modelo               VARCHAR(80),
    categoria_id         BIGINT         NOT NULL,
    unidad_medida_id     BIGINT         NOT NULL,
    controla_serial      BOOLEAN        NOT NULL DEFAULT FALSE,
    precio_instalador    NUMERIC(19, 4) NOT NULL,
    precio_cliente_final NUMERIC(19, 4) NOT NULL,
    moneda_precio        VARCHAR(3)     NOT NULL DEFAULT 'USD',
    stock                NUMERIC(14, 3) NOT NULL DEFAULT 0,
    costo_actual_usd     NUMERIC(19, 4),
    stock_minimo         NUMERIC(14, 3),
    descripcion          TEXT,
    foto_clave           VARCHAR(300),
    activo               BOOLEAN        NOT NULL DEFAULT TRUE,
    version              BIGINT         NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by           BIGINT,
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by           BIGINT,
    CONSTRAINT ck_producto_codigo CHECK (codigo = upper(trim(codigo)) AND length(codigo) > 0),
    CONSTRAINT ck_producto_nombre CHECK (length(trim(nombre)) > 0),
    CONSTRAINT ck_producto_precio_instalador CHECK (precio_instalador >= 0),
    CONSTRAINT ck_producto_precio_cliente_final CHECK (precio_cliente_final >= 0),
    CONSTRAINT ck_producto_moneda_precio CHECK (moneda_precio IN ('USD', 'COP', 'VES')),
    CONSTRAINT ck_producto_stock CHECK (stock >= 0),
    CONSTRAINT ck_producto_costo CHECK (costo_actual_usd IS NULL OR costo_actual_usd >= 0),
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo IS NULL OR stock_minimo >= 0),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_producto_unidad_medida FOREIGN KEY (unidad_medida_id) REFERENCES unidad_medida (id),
    CONSTRAINT fk_producto_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_producto_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE UNIQUE INDEX uq_producto_codigo ON producto (upper(codigo));
CREATE INDEX ix_producto_categoria ON producto (categoria_id);
CREATE INDEX ix_producto_unidad_medida ON producto (unidad_medida_id);
CREATE INDEX ix_producto_nombre ON producto (lower(nombre));
CREATE INDEX ix_producto_activo ON producto (activo);
CREATE INDEX ix_producto_created_by ON producto (created_by);
CREATE INDEX ix_producto_updated_by ON producto (updated_by);

-- Categorías iniciales (RF-15).
INSERT INTO categoria (nombre)
VALUES ('Cámaras'),
       ('Grabadores (DVR/NVR)'),
       ('Discos duros'),
       ('Cable'),
       ('Balunes'),
       ('Fuentes de poder'),
       ('Conectores'),
       ('Cajas y accesorios');

-- Unidades de medida iniciales (sección 3.3).
INSERT INTO unidad_medida (nombre, abreviatura, admite_decimales)
VALUES ('Unidad', 'und', FALSE),
       ('Metro', 'm', TRUE),
       ('Par', 'par', FALSE);
