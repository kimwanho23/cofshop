package kwh.cofshop.member.domain;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    @DisplayName("포인트 복구: point가 null이어도 정상 복구")
    void restorePoint_nullPoint() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "point", null);

        member.restorePoint(100);

        assertThat(member.getPoint()).isEqualTo(100);
    }

    @Test
    @DisplayName("포인트 복구: int 최대값 초과 시 예외")
    void restorePoint_overflow() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "point", Integer.MAX_VALUE);

        assertThatThrownBy(() -> member.restorePoint(1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.INVALID_POINT_OPERATION);
    }

    @Test
    @DisplayName("포인트 갱신: int 최대값 초과 시 예외")
    void updatePoint_overflow() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "point", Integer.MAX_VALUE);

        assertThatThrownBy(() -> member.updatePoint(1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.INVALID_POINT_OPERATION);
    }
}
