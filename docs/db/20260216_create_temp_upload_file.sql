CREATE TABLE IF NOT EXISTS temp_upload_file (
    temp_upload_file_id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    upload_file_name VARCHAR(255) NOT NULL,
    store_file_name VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (temp_upload_file_id),
    UNIQUE KEY uk_temp_upload_file_store_file_name (store_file_name),
    KEY idx_temp_upload_file_expires_at (expires_at),
    KEY idx_temp_upload_file_owner_id (owner_id)
);
