-- Terceros: clientes (sección 3.10) y proveedores (sección 3.6).
-- No se eliminan: conservan su historial (P-11).

CREATE TABLE cliente (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo             VARCHAR(20)  NOT NULL,
    nombre           VARCHAR(150) NOT NULL,
    tipo_documento   VARCHAR(3),
    numero_documento VARCHAR(30),
    telefono         VARCHAR(20)  NOT NULL,
    correo           VARCHAR(254),
    direccion        VARCHAR(200),
    ciudad           VARCHAR(80),
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       BIGINT,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by       BIGINT,
    CONSTRAINT ck_cliente_tipo CHECK (tipo IN ('INSTALADOR', 'CLIENTE_FINAL')),
    CONSTRAINT ck_cliente_nombre CHECK (length(trim(nombre)) > 0),
    CONSTRAINT ck_cliente_tipo_documento CHECK (tipo_documento IN ('CC', 'NIT')),
    CONSTRAINT ck_cliente_documento_completo
        CHECK ((tipo_documento IS NULL) = (numero_documento IS NULL)),
    -- Teléfono con indicativo internacional (P-10), por ejemplo +573001234567.
    CONSTRAINT ck_cliente_telefono CHECK (telefono ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT fk_cliente_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_cliente_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
-- Un mismo CC/NIT no se repite (P-12); el documento puede quedar vacío.
CREATE UNIQUE INDEX uq_cliente_documento ON cliente (tipo_documento, numero_documento)
    WHERE numero_documento IS NOT NULL;
CREATE INDEX ix_cliente_tipo ON cliente (tipo);
CREATE INDEX ix_cliente_nombre ON cliente (lower(nombre));
CREATE INDEX ix_cliente_created_by ON cliente (created_by);
CREATE INDEX ix_cliente_updated_by ON cliente (updated_by);

CREATE TABLE proveedor (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    nit             VARCHAR(30),
    telefono        VARCHAR(20),
    correo          VARCHAR(254),
    ciudad          VARCHAR(80),
    moneda_habitual VARCHAR(3)   NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    CONSTRAINT ck_proveedor_nombre CHECK (length(trim(nombre)) > 0),
    CONSTRAINT ck_proveedor_moneda_habitual CHECK (moneda_habitual IN ('USD', 'COP', 'VES')),
    CONSTRAINT ck_proveedor_telefono CHECK (telefono IS NULL OR telefono ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT fk_proveedor_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_proveedor_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_proveedor_nombre ON proveedor (lower(nombre));
CREATE INDEX ix_proveedor_created_by ON proveedor (created_by);
CREATE INDEX ix_proveedor_updated_by ON proveedor (updated_by);
