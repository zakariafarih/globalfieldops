CREATE TABLE technician_skills (
    id                  UUID            PRIMARY KEY,
    technician_id       UUID            NOT NULL,
    skill_name          VARCHAR(100)    NOT NULL,
    proficiency_level   VARCHAR(20),
    created_at          TIMESTAMPTZ     NOT NULL,

    CONSTRAINT fk_technician_skills_technician
        FOREIGN KEY (technician_id)
        REFERENCES technicians (id)
        ON DELETE CASCADE,

    CONSTRAINT uk_technician_skills_technician_id_skill_name
        UNIQUE (technician_id, skill_name)
);

CREATE INDEX idx_technician_skills_technician_id ON technician_skills (technician_id);
