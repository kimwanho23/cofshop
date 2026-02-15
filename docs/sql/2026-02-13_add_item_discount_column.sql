-- 기존 운영 DB(이미 테이블이 있는 경우)용 증분 스키마 패치
-- 빈 DB 초기 세팅은 JPA_DDL_AUTO=create 1회 부팅 후 validate 복귀 권장
-- 대상: MySQL 8+
ALTER TABLE `item`
    ADD COLUMN IF NOT EXISTS `discount` INT NOT NULL DEFAULT 0 AFTER `price`;
