-- Tasas de cambio (sección 3.5): TRM automática (USD/COP) y tasa del bolívar manual (USD/VES).

-- Una tasa por par y por día: es la base de la idempotencia de la tarea de la TRM (BP-15).
CREATE TABLE tasa_cambio (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    par            VARCHAR(7)     NOT NULL,
    fecha          DATE           NOT NULL,
    valor          NUMERIC(19, 6) NOT NULL,
    fuente         VARCHAR(20)    NOT NULL,
    registrada_por BIGINT,
    registrada_en  TIMESTAMPTZ    NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by     BIGINT,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by     BIGINT,
    CONSTRAINT uq_tasa_cambio_par_fecha UNIQUE (par, fecha),
    CONSTRAINT ck_tasa_cambio_par CHECK (par IN ('USD_COP', 'USD_VES')),
    CONSTRAINT ck_tasa_cambio_valor CHECK (valor > 0),
    CONSTRAINT ck_tasa_cambio_fuente CHECK (fuente IN ('SUPERFINANCIERA', 'MANUAL')),
    -- La tasa del bolívar siempre es manual (sección 7); la manual siempre tiene usuario.
    CONSTRAINT ck_tasa_cambio_bolivar_manual CHECK (par = 'USD_COP' OR fuente = 'MANUAL'),
    CONSTRAINT ck_tasa_cambio_usuario CHECK ((fuente = 'MANUAL') = (registrada_por IS NOT NULL)),
    CONSTRAINT fk_tasa_cambio_registrada_por FOREIGN KEY (registrada_por) REFERENCES usuario (id),
    CONSTRAINT fk_tasa_cambio_created_by FOREIGN KEY (created_by) REFERENCES usuario (id),
    CONSTRAINT fk_tasa_cambio_updated_by FOREIGN KEY (updated_by) REFERENCES usuario (id)
);
CREATE INDEX ix_tasa_cambio_registrada_por ON tasa_cambio (registrada_por);
CREATE INDEX ix_tasa_cambio_created_by ON tasa_cambio (created_by);
CREATE INDEX ix_tasa_cambio_updated_by ON tasa_cambio (updated_by);

-- Historial de correcciones (RF-36). Solo se insertan filas; nunca se modifican.
-- automatica: la TRM oficial reemplazó a una manual del mismo día (P-13).
CREATE TABLE correccion_tasa (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tasa_id        BIGINT         NOT NULL,
    valor_anterior NUMERIC(19, 6) NOT NULL,
    valor_nuevo    NUMERIC(19, 6) NOT NULL,
    motivo         VARCHAR(300),
    automatica     BOOLEAN        NOT NULL DEFAULT FALSE,
    corregida_por  BIGINT,
    corregida_en   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_correccion_tasa_usuario CHECK (automatica OR corregida_por IS NOT NULL),
    CONSTRAINT fk_correccion_tasa_tasa FOREIGN KEY (tasa_id) REFERENCES tasa_cambio (id),
    CONSTRAINT fk_correccion_tasa_usuario FOREIGN KEY (corregida_por) REFERENCES usuario (id)
);
CREATE INDEX ix_correccion_tasa_tasa ON correccion_tasa (tasa_id);
CREATE INDEX ix_correccion_tasa_usuario ON correccion_tasa (corregida_por);

-- Registro de cada ejecución de la tarea de la TRM (BP-15).
CREATE TABLE ejecucion_tarea_trm (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_objetivo DATE         NOT NULL,
    inicio         TIMESTAMPTZ  NOT NULL,
    fin            TIMESTAMPTZ  NOT NULL,
    resultado      VARCHAR(10)  NOT NULL,
    intento        INTEGER      NOT NULL,
    detalle        VARCHAR(500),
    CONSTRAINT ck_ejecucion_tarea_trm_resultado CHECK (resultado IN ('EXITO', 'FALLO', 'OMITIDA'))
);
CREATE INDEX ix_ejecucion_tarea_trm_fecha ON ejecucion_tarea_trm (fecha_objetivo);
