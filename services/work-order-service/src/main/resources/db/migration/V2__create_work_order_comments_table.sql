CREATE TABLE work_order_comments (
    id             UUID            PRIMARY KEY,
    work_order_id  UUID            NOT NULL,
    author_name    VARCHAR(100)    NOT NULL,
    body           VARCHAR(2000)   NOT NULL,
    created_at     TIMESTAMPTZ     NOT NULL,

    CONSTRAINT fk_work_order_comments_work_order
        FOREIGN KEY (work_order_id)
        REFERENCES work_orders (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_work_order_comments_work_order_id ON work_order_comments (work_order_id);