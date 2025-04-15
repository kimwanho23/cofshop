package kwh.cofshop.coupon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class MemberCouponServiceTest extends TestSettingUtils {

    @Autowired
    private MemberCouponService memberCouponService;

    @Autowired
    private MemberRepository memberRepository;



    @Test
    @DisplayName("쿠폰 발급")
    void createCoupon() throws JsonProcessingException {
        Member member = memberRepository.findById(2L).orElseThrow();

        MemberCouponResponseDto memberCoupon = memberCouponService.createMemberCoupon(member.getId(), 1L);// 쿠폰 발급
        log.info(objectMapper.writeValueAsString(memberCoupon));

    }

}