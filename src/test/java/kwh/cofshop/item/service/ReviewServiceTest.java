package kwh.cofshop.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.repository.ReviewRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@AutoConfigureMockMvc
class ReviewServiceTest extends TestSettingUtils {

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
    void createReview() throws Exception {
        Item item = createTestItem();

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        ReviewRequestDto reviewRequestDto = getReviewRequestDto(item, (long) (Math.random() * 5) + 1);
        ReviewResponseDto responseDto = reviewService.save( item.getId(), reviewRequestDto, member.getId()); // 리뷰 저장
        String reviewJson = objectMapper.writeValueAsString(responseDto);
        log.info("review Json : {}", reviewJson);
    }

    @Test
    @DisplayName("리뷰 중복 작성 시 BusinessException 발생")
    @Transactional
    void createDuplicateReview() throws Exception {
        // Given
        Item item = createTestItem();
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        ReviewRequestDto reviewRequestDto = getReviewRequestDto(item, (long) (Math.random() * 5) + 1);

        // 첫 번째 리뷰 저장
        reviewService.save(item.getId(), reviewRequestDto, member.getId());

        // 리뷰 중복 저장 시도
        Assertions.assertThrows(BusinessException.class,
                ()-> reviewService.save(item.getId(), reviewRequestDto, member.getId()));
    }



    @Test
    @DisplayName("다수 리뷰 작성 테스트")
    @Transactional
    void createReviewRand() throws Exception {

        for (int i = 0; i < 10; i++) {
            Item item = itemRepository.findById(2L).orElseThrow();
            Member member = memberRepository.findByEmail("randomMember" + i + "@gmail.com").orElseThrow();
            ReviewRequestDto reviewRequestDto = getReviewRequestDto(item, (long) (Math.random() * 5) + 1);
            reviewService.save(item.getId(), reviewRequestDto, member.getId()); // 리뷰 저장
        }
    }


    @Test
    @DisplayName("리뷰 제거")
    @Transactional
    void deleteReview() throws Exception {

        Item item = itemRepository.findById(2L)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        Review byItem = reviewRepository.findByItemAndMember(item.getId(), member.getId());

        reviewRepository.delete(byItem);

    }

    @Test
    @DisplayName("특정 상품의 리뷰 페이징 조회")
    @Transactional
    void ReviewListPage() throws Exception {

        Item item = itemRepository.findById(2L)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<ReviewResponseDto> reviewsByItem = reviewService.getReviewsByItem(item.getId(), pageable);
        Page<ReviewResponseDto> reviewsByItem2 = reviewService.getReviewsByItem(item.getId(), pageable);
        log.info(objectMapper.writeValueAsString(reviewsByItem));


    }

    @Test
    @DisplayName("특정 상품의 리뷰 전체 조회")
    @Transactional
    void ReviewList() throws Exception {
        Item item = itemRepository.findById(2L)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        List<ReviewResponseDto> reviewsByItemId = reviewService.getReviewsByItemId(item.getId());
        log.info(objectMapper.writeValueAsString(reviewsByItemId));
    }


    private static ReviewRequestDto getReviewRequestDto(Item item, Long rating) {
        ReviewRequestDto reviewRequestDto  = new ReviewRequestDto();
        reviewRequestDto.setContent("1 ~ 5의 랜덤 리뷰 값");
        reviewRequestDto.setRating(rating);
        reviewRequestDto.setItem(item.getId());
        return reviewRequestDto;
    }

}