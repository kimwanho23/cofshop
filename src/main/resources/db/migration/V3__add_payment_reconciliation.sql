-- PG 대사 대상 조회와 재확인 순서 관리
ALTER TABLE payment_entity ADD COLUMN last_reconciled_at DATETIME(6) NULL;
CREATE INDEX idx_payment_status_requested_at ON payment_entity (status, requested_at);
