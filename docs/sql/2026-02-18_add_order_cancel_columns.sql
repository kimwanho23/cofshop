ALTER TABLE orders
    ADD COLUMN cancel_reason VARCHAR(500) NULL,
    ADD COLUMN cancelled_at DATETIME(6) NULL;
