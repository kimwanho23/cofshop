-- 회원별 최근 로그인 이력 조회용 인덱스
CREATE INDEX idx_member_login_history_member_login_dt ON member_login_history (member_id, login_dt);
-- 보관 기간이 지난 이력 삭제용 인덱스
CREATE INDEX idx_member_login_history_login_dt ON member_login_history (login_dt);
