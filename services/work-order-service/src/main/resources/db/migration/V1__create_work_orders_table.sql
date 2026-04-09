CREATE TABLE work_orders (
    id                      UUID            PRIMARY KEY,
    title                   VARCHAR(150)    NOT NULL,
    summary                 VARCHAR(1000)   NOT NULL,
    country_code            CHAR(2)         NOT NULL,
    region                  VARCHAR(100),
    priority                VARCHAR(20)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL,
    assigned_technician_id  UUID,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,

    CONSTRAINT ck_work_orders_country_code CHECK (char_length(country_code) = 2)
);

CREATE INDEX idx_work_orders_status ON work_orders (status);
CREATE INDEX idx_work_orders_priority ON work_orders (priority);
CREATE INDEX idx_work_orders_country_code ON work_orders (country_code);
CREATE INDEX idx_work_orders_assigned_technician_id ON work_orders (assigned_technician_id);