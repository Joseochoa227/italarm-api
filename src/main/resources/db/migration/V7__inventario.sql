-- Inventario: kárdex, historial de costo, seriales, ajustes, inventario inicial e idempotencia
-- (secciones 3.4, 3.7, 3.8, 3.18; BP-09 a BP-11; RT-07).

-- Consecutivos por tipo de documento (BP-11): AJ-001 y II-001. El formato se aplica al mostrar.
CREATE SEQUENCE seq_ajuste START WITH 1;
CREATE SEQUENCE seq_inventario_inicial START WITH 1;

-- Kárdex: solo se insertan filas; nunca se modifican ni se borran (BP-10).
CREATE TABLE movimiento_inventario (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    producto_id           BIGINT         NOT NULL,
    fecha                 DATE           NOT NULL,
    registrado_en         TIMESTAMPTZ    NOT NULL,
    tipo                  VARCHAR(25)    NOT NULL,
    documento_tipo        VARCHAR(20)    NOT NULL,
    documento_id          BIGINT         NOT NULL,
    documento_consecutivo VARCHAR(20)    NOT NULL,
    entrada               NUMERIC(14, 3) NOT NULL DEFAULT 0,
    salida                NUMERIC(14, 3) NOT NULL DEFAULT 0,
    saldo                 NUMERIC(14, 3) NOT NULL,
    costo_unitario_usd    NUMERIC(19, 4),
    detalle               VARCHAR(300),
    usuario_id            BIGINT,
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('COMPRA', 'ANULACION_COMPRA', 'AJUSTE_ENTRADA',
                                                  'AJUSTE_SALIDA', 'INVENTARIO_INICIAL')),
    CONSTRAINT ck_movimiento_cantidades CHECK (entrada >= 0 AND salida >= 0 AND (entrada > 0) <> (salida > 0)),
    CONSTRAINT ck_movimiento_saldo CHECK (saldo >= 0),
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX ix_movimiento_producto ON movimiento_inventario (producto_id, id);
CREATE INDEX ix_movimiento_documento ON movimiento_inventario (documento_tipo, documento_id);
CREATE INDEX ix_movimiento_fecha ON movimiento_inventario (fecha);
CREATE INDEX ix_movimiento_usuario ON movimiento_inventario (usuario_id);

-- Historial de cambios de costo (RF-57, RF-67). Solo se insertan filas.
CREATE TABLE historial_costo (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    producto_id           BIGINT         NOT NULL,
    fecha                 DATE           NOT NULL,
    registrado_en         TIMESTAMPTZ    NOT NULL,
    documento_tipo        VARCHAR(20)    NOT NULL,
    documento_id          BIGINT         NOT NULL,
    documento_consecutivo VARCHAR(20)    NOT NULL,
    moneda_factura        VARCHAR(3),
    tasa_factura          NUMERIC(19, 6),
    costo_factura         NUMERIC(19, 4),
    costo_factura_usd     NUMERIC(19, 6),
    costo_anterior        NUMERIC(19, 4),
    costo_nuevo           NUMERIC(19, 4),
    regla                 VARCHAR(20)    NOT NULL,
    usuario_id            BIGINT,
    CONSTRAINT ck_historial_regla CHECK (regla IN ('SUBE', 'PROMEDIO', 'SIN_STOCK', 'AJUSTE',
                                                   'INVENTARIO_INICIAL', 'ANULACION')),
    CONSTRAINT ck_historial_moneda CHECK (moneda_factura IS NULL OR moneda_factura IN ('USD', 'COP', 'VES')),
    CONSTRAINT fk_historial_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX ix_historial_producto ON historial_costo (producto_id, id);
CREATE INDEX ix_historial_documento ON historial_costo (documento_tipo, documento_id);
CREATE INDEX ix_historial_usuario ON historial_costo (usuario_id);

-- Seriales (sección 3.4). Únicos por producto, salvo los de compras anuladas (P-22).
CREATE TABLE serial (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    producto_id          BIGINT      NOT NULL,
    numero               VARCHAR(80) NOT NULL,
    estado               VARCHAR(15) NOT NULL,
    fecha_entrada        DATE        NOT NULL,
    entrada_tipo         VARCHAR(20) NOT NULL,
    entrada_id           BIGINT      NOT NULL,
    entrada_consecutivo  VARCHAR(20) NOT NULL,
    salida_tipo          VARCHAR(20),
    salida_id            BIGINT,
    salida_consecutivo   VARCHAR(20),
    vencimiento_garantia DATE,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           BIGINT,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           BIGINT,
    CONSTRAINT ck_serial_numero CHECK (numero = upper(trim(numero)) AND length(numero) > 0),
    CONSTRAINT ck_serial_estado CHECK (estado IN ('EN_BODEGA', 'VENDIDO', 'INSTALADO', 'DADO_DE_BAJA', 'ANULADO')),
    CONSTRAINT fk_serial_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_serial_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_serial_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE UNIQUE INDEX uq_serial_producto_numero ON serial (producto_id, numero) WHERE estado <> 'ANULADO';
CREATE INDEX ix_serial_numero ON serial (numero);
CREATE INDEX ix_serial_producto_estado ON serial (producto_id, estado);
CREATE INDEX ix_serial_entrada ON serial (entrada_tipo, entrada_id);
CREATE INDEX ix_serial_created_by ON serial (created_by);
CREATE INDEX ix_serial_updated_by ON serial (updated_by);

-- Historial de cada serial (RF-24). Solo se insertan filas.
CREATE TABLE movimiento_serial (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    serial_id             BIGINT       NOT NULL,
    fecha                 DATE         NOT NULL,
    registrado_en         TIMESTAMPTZ  NOT NULL,
    tipo                  VARCHAR(20)  NOT NULL,
    documento_tipo        VARCHAR(20)  NOT NULL,
    documento_id          BIGINT       NOT NULL,
    documento_consecutivo VARCHAR(20)  NOT NULL,
    detalle               VARCHAR(300),
    usuario_id            BIGINT,
    CONSTRAINT ck_movimiento_serial_tipo CHECK (tipo IN ('ENTRADA', 'BAJA', 'ANULACION')),
    CONSTRAINT fk_movimiento_serial_serial FOREIGN KEY (serial_id) REFERENCES serial (id),
    CONSTRAINT fk_movimiento_serial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX ix_movimiento_serial_serial ON movimiento_serial (serial_id, id);
CREATE INDEX ix_movimiento_serial_usuario ON movimiento_serial (usuario_id);

-- Ajustes de inventario (RF-58 a RF-62). No se editan ni se anulan (RF-70, P-24).
CREATE TABLE ajuste (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero             BIGINT         NOT NULL,
    fecha              DATE           NOT NULL,
    producto_id        BIGINT         NOT NULL,
    tipo               VARCHAR(10)    NOT NULL,
    motivo             VARCHAR(15)    NOT NULL,
    descripcion        VARCHAR(300),
    cantidad           NUMERIC(14, 3) NOT NULL,
    costo_unitario_usd NUMERIC(19, 4) NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by         BIGINT,
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by         BIGINT,
    CONSTRAINT uq_ajuste_numero UNIQUE (numero),
    CONSTRAINT ck_ajuste_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT ck_ajuste_motivo CHECK (motivo IN ('PERDIDA', 'DANO', 'CONTEO_FISICO', 'GARANTIA', 'OTRO')),
    CONSTRAINT ck_ajuste_descripcion CHECK (motivo <> 'OTRO' OR descripcion IS NOT NULL),
    CONSTRAINT ck_ajuste_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_ajuste_costo CHECK (costo_unitario_usd >= 0),
    CONSTRAINT fk_ajuste_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_ajuste_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_ajuste_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_ajuste_producto ON ajuste (producto_id);
CREATE INDEX ix_ajuste_fecha ON ajuste (fecha);
CREATE INDEX ix_ajuste_created_by ON ajuste (created_by);
CREATE INDEX ix_ajuste_updated_by ON ajuste (updated_by);

-- Inventario inicial cargado desde Excel (sección 3.18).
CREATE TABLE inventario_inicial (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero              BIGINT       NOT NULL,
    fecha               DATE         NOT NULL,
    archivo_nombre      VARCHAR(200),
    productos_creados   INTEGER      NOT NULL DEFAULT 0,
    clientes_creados    INTEGER      NOT NULL DEFAULT 0,
    proveedores_creados INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    CONSTRAINT uq_inventario_inicial_numero UNIQUE (numero),
    CONSTRAINT fk_inventario_inicial_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_inventario_inicial_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_inventario_inicial_created_by ON inventario_inicial (created_by);
CREATE INDEX ix_inventario_inicial_updated_by ON inventario_inicial (updated_by);

CREATE TABLE linea_inventario_inicial (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventario_inicial_id BIGINT         NOT NULL,
    producto_id           BIGINT         NOT NULL,
    cantidad              NUMERIC(14, 3) NOT NULL,
    costo_unitario_usd    NUMERIC(19, 4) NOT NULL,
    CONSTRAINT uq_linea_inventario_inicial UNIQUE (inventario_inicial_id, producto_id),
    CONSTRAINT ck_linea_inventario_inicial_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_linea_inventario_inicial_costo CHECK (costo_unitario_usd > 0),
    CONSTRAINT fk_linea_inventario_inicial_documento FOREIGN KEY (inventario_inicial_id) REFERENCES inventario_inicial (id),
    CONSTRAINT fk_linea_inventario_inicial_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);
CREATE INDEX ix_linea_inventario_inicial_producto ON linea_inventario_inicial (producto_id);

-- Idempotency-Key (RT-07): un doble toque en el celular no crea dos documentos.
CREATE TABLE idempotencia (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL,
    operacion    VARCHAR(40)  NOT NULL,
    clave        VARCHAR(100) NOT NULL,
    documento_id BIGINT,
    creada_en    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_idempotencia UNIQUE (usuario_id, operacion, clave),
    CONSTRAINT fk_idempotencia_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
