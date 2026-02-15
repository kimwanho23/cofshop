`CREATE TABLE coupon_issue_outbox (
    coupon_issue_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
    member_coupon_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    published_at DATETIME NULL,
    create_date DATETIME NOT NULL,
    last_modified_date DATETIME NOT NULL,
    PRIMARY KEY (coupon_issue_outbox_id),
    KEY idx_coupon_outbox_status_id (status, coupon_issue_outbox_id)
);
