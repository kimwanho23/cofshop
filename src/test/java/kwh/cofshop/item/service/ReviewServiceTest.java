package kwh.cofshop.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.repository.ReviewRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
class ReviewServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성")
    @Transactional
   // @Commit
    void createReview() throws Exception {
        Item item = itemRepository.findById(2L)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        ReviewRequestDto reviewRequestDto = getReviewRequestDto(item);
        ReviewResponseDto responseDto = reviewService.save(reviewRequestDto, member.getId()); // 리뷰 저장
        String reviewJson = objectMapper.writeValueAsString(responseDto);
        log.info("review Json : {}", reviewJson);

    }

    private static ReviewRequestDto getReviewRequestDto(Item item) {
        ReviewRequestDto reviewRequestDto  = new ReviewRequestDto();
        reviewRequestDto.setContent("리뷰 평점 테스트");
        reviewRequestDto.setRating(5L);
        reviewRequestDto.setItem(item.getId());
        return reviewRequestDto;
    }

    @Test
    @DisplayName("리뷰 제거")
    @Transactional
    void deleteReview() throws Exception {

        Item item = itemRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        Review byItem = reviewRepository.findByItemAndMember(item, member);

        reviewRepository.delete(byItem);

    }

}