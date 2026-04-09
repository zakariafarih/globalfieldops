CREATE TABLE work_order_status_history (
    id             UUID            PRIMARY KEY,
    work_order_id  UUID            NOT NULL,
    from_status    VARCHAR(20),
    to_status      VARCHAR(20)     NOT NULL,
    changed_by     VARCHAR(100)    NOT NULL,
    change_reason  VARCHAR(500),
    changed_at     TIMESTAMPTZ     NOT NULL,

    CONSTRAINT fk_work_order_status_history_work_order
        FOREIGN KEY (work_order_id)
        REFERENCES work_orders (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_work_order_status_history_work_order_id ON work_order_status_history (work_order_id);
CREATE INDEX idx_work_order_status_history_changed_at ON work_order_status_history (changed_at);