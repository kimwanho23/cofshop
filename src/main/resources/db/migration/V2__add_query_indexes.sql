-- 스케줄러와 임시 파일 정리가 주기적으로 조회하는 컬럼의 인덱스
CREATE INDEX idx_orders_state_order_date ON orders (order_state, order_date);
CREATE INDEX idx_orders_refund_status_processed_at ON orders (refund_request_status, refund_processed_at);
CREATE INDEX idx_temp_upload_file_expires_at ON temp_upload_file (expires_at);
CREATE INDEX idx_temp_upload_file_owner_id ON temp_upload_file (owner_id);
