package kwh.cofshop.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.repository.ReviewRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // POST 요청
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test@gmail.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("리뷰 등록 통합 테스트")
    void addReview_success() throws Exception {
        // 1. 테스트용 데이터 준비
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Item item = itemRepository.findById(1L).orElseThrow();

        ReviewRequestDto requestDto = getReviewRequestDto(item, member);

        String requestJson = new ObjectMapper().writeValueAsString(requestDto);

        log.info(requestJson);
        // 2. 요청 수행
        mockMvc.perform(post("/api/review/createReview")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGdtYWlsLmNvbSIsInJvbGVzIjpbeyJhdXRob3JpdHkiOiJST0xFX01FTUJFUiJ9XSwiaWF0IjoxNzM1NzQxNTg0LCJleHAiOjE3MzU3NDUxODR9.JSCNRdXEytzUgrc4Y3r1fvmvQvprZWciFlUiiNwc-FA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.data.content").value("리뷰 평점 테스트")) // 경로 수정
                .andExpect(jsonPath("$.body.data.rating").value(5)) // 경로 수정
                .andDo(print());
    }

    private static ReviewRequestDto getReviewRequestDto(Item item, Member member) {
        ReviewRequestDto reviewRequestDto  = new ReviewRequestDto();
        reviewRequestDto.setContent("리뷰 평점 테스트");
        reviewRequestDto.setRating(5L);
        reviewRequestDto.setItem(item.getItemId());
        reviewRequestDto.setMember(member.getEmail());
        return reviewRequestDto;
    }

}