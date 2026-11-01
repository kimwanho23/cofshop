-- 탈퇴 회원의 거래기록 중 개인정보를 분리 보관하는 테이블 (전자상거래법 시행령 제6조, 개인정보 보호법 제21조 제3항)
CREATE TABLE order_personal_archive (
    order_personal_archive_id bigint NOT NULL AUTO_INCREMENT,
    order_id bigint NOT NULL,
    buyer_email varchar(255),
    buyer_name varchar(50),
    buyer_tel varchar(255),
    city varchar(255),
    street varchar(255),
    zip_code varchar(255),
    delivery_request varchar(255),
    archived_at datetime(6) NOT NULL,
    expires_at datetime(6) NOT NULL,
    PRIMARY KEY (order_personal_archive_id),
    CONSTRAINT uk_order_personal_archive_order UNIQUE (order_id),
    CONSTRAINT fk_order_personal_archive_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
) ENGINE=InnoDB;

CREATE INDEX idx_order_personal_archive_expires_at ON order_personal_archive (expires_at);

-- 탈퇴 시 결제 기록의 구매자 정보를 비운다
ALTER TABLE payment_entity
    MODIFY buyer_email varchar(255) NULL,
    MODIFY buyer_name varchar(255) NULL,
    MODIFY buyer_tel varchar(255) NULL;
