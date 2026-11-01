-- 자동 구매 확정 기준 시각과 환불 실패 시 되돌릴 주문 상태
ALTER TABLE orders
    ADD COLUMN delivered_at DATETIME(6) NULL,
    ADD COLUMN state_before_refund ENUM ('COMPLETED','CANCELLED','DELIVERED','PAID','PAYMENT_PENDING','PREPARING_FOR_SHIPMENT','REFUND_PENDING','SHIPPING','WAITING_FOR_PAY') NULL;

UPDATE orders SET delivered_at = last_modified_date WHERE order_state = 'DELIVERED';

CREATE INDEX idx_orders_state_delivered_at ON orders (order_state, delivered_at);
