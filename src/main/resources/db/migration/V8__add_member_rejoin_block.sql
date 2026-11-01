-- 탈퇴 회원의 재가입 제한용 해시. 원문은 저장하지 않고 기간이 지나면 삭제한다
CREATE TABLE member_rejoin_block (
    member_rejoin_block_id bigint NOT NULL AUTO_INCREMENT,
    identifier_hash varchar(64) NOT NULL,
    expires_at datetime(6) NOT NULL,
    PRIMARY KEY (member_rejoin_block_id),
    CONSTRAINT uk_member_rejoin_block_hash UNIQUE (identifier_hash)
) ENGINE=InnoDB;

CREATE INDEX idx_member_rejoin_block_expires_at ON member_rejoin_block (expires_at);
