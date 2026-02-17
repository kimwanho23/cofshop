ALTER TABLE orders
    ADD COLUMN refund_request_status VARCHAR(50) NULL,
    ADD COLUMN refund_request_reason VARCHAR(500) NULL,
    ADD COLUMN refund_requested_at DATETIME(6) NULL,
    ADD COLUMN refund_processed_at DATETIME(6) NULL,
    ADD COLUMN refund_processed_reason VARCHAR(500) NULL;
