-- 결제 수동 처리와 대사 불일치 이력
CREATE TABLE payment_operation_log (
    payment_operation_log_id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    admin_id BIGINT NULL,
    action ENUM ('MISMATCH_DETECTED','RECONCILE','REFUND_RETRY') NOT NULL,
    result ENUM ('FAILED','SUCCESS') NOT NULL,
    error_code VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (payment_operation_log_id),
    KEY idx_payment_operation_log_payment (payment_id, payment_operation_log_id),
    KEY idx_payment_operation_log_action (action, payment_operation_log_id)
) ENGINE=InnoDB;
