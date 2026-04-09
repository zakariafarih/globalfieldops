CREATE TABLE technicians (
    id              UUID            PRIMARY KEY,
    employee_code   VARCHAR(50)     NOT NULL,
    email           VARCHAR(320)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    country_code    CHAR(2)         NOT NULL,
    region          VARCHAR(100),
    availability    VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,

    CONSTRAINT uk_technicians_employee_code UNIQUE (employee_code),
    CONSTRAINT uk_technicians_email         UNIQUE (email),
    CONSTRAINT ck_technicians_country_code  CHECK  (char_length(country_code) = 2)
);

CREATE INDEX idx_technicians_country_code  ON technicians (country_code);
CREATE INDEX idx_technicians_availability  ON technicians (availability);
CREATE INDEX idx_technicians_active        ON technicians (active);
