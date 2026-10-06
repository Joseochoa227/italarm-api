-- Cotizaciones (sección 3.11): estados, versiones (RF-88) y enlace con la venta o instalación
-- generada (RF-95, RF-74).

CREATE SEQUENCE seq_cotizacion START WITH 1;

CREATE TABLE cotizacion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero              BIGINT         NOT NULL,
    -- P-46: COT-0001 v2 al editarla en evaluación.
    numero_version      INTEGER        NOT NULL DEFAULT 1,
    tipo                VARCHAR(15)    NOT NULL,
    fecha               DATE           NOT NULL,
    validez_dias        INTEGER        NOT NULL,
    vence               DATE           NOT NULL,
    cliente_id          BIGINT         NOT NULL,
    -- Copia de los datos del cliente para el PDF (P-35).
    cliente_tipo        VARCHAR(15)    NOT NULL,
    cliente_nombre      VARCHAR(150)   NOT NULL,
    cliente_documento   VARCHAR(40),
    cliente_telefono    VARCHAR(20),
    cliente_direccion   VARCHAR(200),
    cliente_ciudad      VARCHAR(80),
    moneda              VARCHAR(3)     NOT NULL,
    -- Tasas de la fecha de la cotización (RN-04, P-49).
    trm                 NUMERIC(19, 6),
    fecha_trm           DATE,
    tasa_ves            NUMERIC(19, 6),
    fecha_tasa_ves      DATE,
    descripcion         VARCHAR(2000),
    material            NUMERIC(19, 4) NOT NULL,
    mano_obra           NUMERIC(19, 4) NOT NULL,
    subtotal            NUMERIC(19, 4) NOT NULL,
    descuento_tipo      VARCHAR(10)    NOT NULL,
    descuento_valor     NUMERIC(19, 4) NOT NULL,
    descuento           NUMERIC(19, 4) NOT NULL,
    total               NUMERIC(19, 4) NOT NULL,
    -- Costo y utilidad estimados con el costo vigente al cotizar.
    costo               NUMERIC(19, 4) NOT NULL,
    utilidad            NUMERIC(19, 4) NOT NULL,
    porcentaje_utilidad NUMERIC(9, 2),
    total_usd           NUMERIC(19, 4) NOT NULL,
    costo_usd           NUMERIC(19, 4) NOT NULL,
    utilidad_usd        NUMERIC(19, 4) NOT NULL,
    observaciones       VARCHAR(500),
    monedas_comprobante VARCHAR(20),
    estado              VARCHAR(15)    NOT NULL DEFAULT 'BORRADOR',
    enviada_en          TIMESTAMPTZ,
    aprobada_en         TIMESTAMPTZ,
    rechazada_en        TIMESTAMPTZ,
    motivo_rechazo      VARCHAR(15),
    detalle_rechazo     VARCHAR(300),
    vencida_el          DATE,
    -- Venta o instalación generada (RF-95), según el tipo.
    documento_id        BIGINT,
    documento_numero    VARCHAR(20),
    convertida_en       TIMESTAMPTZ,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    CONSTRAINT uq_cotizacion_numero UNIQUE (numero),
    CONSTRAINT ck_cotizacion_tipo CHECK (tipo IN ('VENTA', 'INSTALACION')),
    CONSTRAINT ck_cotizacion_version CHECK (numero_version >= 1),
    -- RF-83: 8, 15 o 30 días.
    CONSTRAINT ck_cotizacion_validez CHECK (validez_dias IN (8, 15, 30)
        AND vence = fecha + validez_dias),
    CONSTRAINT ck_cotizacion_moneda CHECK (moneda IN ('USD', 'COP', 'VES')),
    CONSTRAINT ck_cotizacion_tasa_cop CHECK (moneda <> 'COP' OR trm IS NOT NULL),
    CONSTRAINT ck_cotizacion_tasa_ves CHECK (moneda <> 'VES' OR tasa_ves IS NOT NULL),
    CONSTRAINT ck_cotizacion_cliente_tipo CHECK (cliente_tipo IN ('INSTALADOR', 'CLIENTE_FINAL')),
    CONSTRAINT ck_cotizacion_descuento_tipo CHECK (descuento_tipo IN ('PORCENTAJE', 'VALOR')),
    CONSTRAINT ck_cotizacion_descuento CHECK (descuento_valor >= 0 AND descuento >= 0 AND descuento <= subtotal),
    CONSTRAINT ck_cotizacion_valores CHECK (material >= 0 AND mano_obra >= 0
        AND subtotal = material + mano_obra AND total = subtotal - descuento AND total_usd >= 0),
    CONSTRAINT ck_cotizacion_costo CHECK (costo >= 0 AND costo_usd >= 0),
    -- Una cotización de venta de material no lleva mano de obra.
    CONSTRAINT ck_cotizacion_mano_obra CHECK (tipo = 'INSTALACION' OR mano_obra = 0),
    CONSTRAINT ck_cotizacion_estado CHECK (estado IN ('BORRADOR', 'EN_EVALUACION', 'APROBADA',
        'CONVERTIDA', 'RECHAZADA', 'VENCIDA')),
    CONSTRAINT ck_cotizacion_motivo CHECK (motivo_rechazo IS NULL
        OR motivo_rechazo IN ('PRECIO', 'COMPETENCIA', 'OTRO')),
    -- RF-95: una cotización convertida está enlazada a su documento, y solo ella.
    CONSTRAINT ck_cotizacion_conversion CHECK ((estado = 'CONVERTIDA') = (documento_id IS NOT NULL)
        AND (documento_id IS NULL) = (documento_numero IS NULL)),
    CONSTRAINT fk_cotizacion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_cotizacion_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_cotizacion_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_cotizacion_fecha ON cotizacion (fecha);
CREATE INDEX ix_cotizacion_cliente ON cotizacion (cliente_id);
-- Tarea de vencimiento (RN-13) y filtro de las por vencer (RF-91).
CREATE INDEX ix_cotizacion_estado_vence ON cotizacion (estado, vence);
CREATE INDEX ix_cotizacion_created_by ON cotizacion (created_by);
CREATE INDEX ix_cotizacion_updated_by ON cotizacion (updated_by);

-- Una línea por producto en cada cotización.
CREATE TABLE linea_cotizacion (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cotizacion_id      BIGINT         NOT NULL,
    producto_id        BIGINT         NOT NULL,
    codigo             VARCHAR(30)    NOT NULL,
    descripcion        VARCHAR(150)   NOT NULL,
    unidad             VARCHAR(10)    NOT NULL,
    cantidad           NUMERIC(14, 3) NOT NULL,
    precio_unitario    NUMERIC(19, 4) NOT NULL,
    precio_sugerido    NUMERIC(19, 4) NOT NULL,
    subtotal           NUMERIC(19, 4) NOT NULL,
    -- Costo vigente al cotizar, para avisar al convertir si cambió (RF-96).
    costo_unitario_usd NUMERIC(19, 4) NOT NULL,
    CONSTRAINT uq_linea_cotizacion_producto UNIQUE (cotizacion_id, producto_id),
    CONSTRAINT ck_linea_cotizacion_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_linea_cotizacion_precio CHECK (precio_unitario >= 0 AND precio_sugerido >= 0 AND subtotal >= 0),
    CONSTRAINT ck_linea_cotizacion_costo CHECK (costo_unitario_usd >= 0),
    CONSTRAINT fk_linea_cotizacion_cotizacion FOREIGN KEY (cotizacion_id) REFERENCES cotizacion (id),
    CONSTRAINT fk_linea_cotizacion_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);
CREATE INDEX ix_linea_cotizacion_producto ON linea_cotizacion (producto_id);

-- Versiones anteriores de una cotización editada en evaluación (RF-88, P-46).
CREATE TABLE version_cotizacion (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cotizacion_id  BIGINT      NOT NULL,
    numero_version INTEGER     NOT NULL,
    contenido      JSONB       NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     BIGINT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by     BIGINT,
    CONSTRAINT uq_version_cotizacion UNIQUE (cotizacion_id, numero_version),
    CONSTRAINT fk_version_cotizacion_cotizacion FOREIGN KEY (cotizacion_id) REFERENCES cotizacion (id),
    CONSTRAINT fk_version_cotizacion_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_version_cotizacion_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_version_cotizacion_created_by ON version_cotizacion (created_by);
CREATE INDEX ix_version_cotizacion_updated_by ON version_cotizacion (updated_by);

-- Cotización de origen de la venta o instalación (RF-74). Columnas creadas en V9 y V10.
ALTER TABLE venta ADD CONSTRAINT fk_venta_cotizacion
    FOREIGN KEY (cotizacion_id) REFERENCES cotizacion (id);
ALTER TABLE instalacion ADD CONSTRAINT fk_instalacion_cotizacion
    FOREIGN KEY (cotizacion_id) REFERENCES cotizacion (id);
-- RN-14: una cotización tiene como máximo un documento generado activo.
CREATE UNIQUE INDEX uq_venta_cotizacion_activa ON venta (cotizacion_id)
    WHERE cotizacion_id IS NOT NULL AND estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_instalacion_cotizacion_activa ON instalacion (cotizacion_id)
    WHERE cotizacion_id IS NOT NULL AND estado = 'ACTIVA';
CREATE INDEX ix_venta_cotizacion ON venta (cotizacion_id);
CREATE INDEX ix_instalacion_cotizacion ON instalacion (cotizacion_id);
