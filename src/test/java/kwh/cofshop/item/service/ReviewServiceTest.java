package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.mapper.ReviewMapper;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.repository.ReviewRepository;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private MemberReadPort memberReadPort;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 생성: 회원 없음")
    void save_memberNotFound() {
        when(memberReadPort.getById(anyLong())).thenThrow(new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> reviewService.save(1L, new ReviewRequestDto(), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("리뷰 생성: 상품 없음")
    void save_itemNotFound() {
        Member member = createMember(1L);
        when(memberReadPort.getById(1L)).thenReturn(member);
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.save(1L, new ReviewRequestDto(), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("리뷰 생성: 중복")
    void save_alreadyExists() {
        Member member = createMember(1L);
        Item item = createItem();

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(reviewRepository.save(any(Review.class))).thenThrow(new DataIntegrityViolationException("dup"));

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setRating(5L);
        requestDto.setContent("좋아요");

        assertThatThrownBy(() -> reviewService.save(1L, requestDto, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("리뷰 생성: 성공")
    void save_success() {
        Member member = createMember(1L);
        Item item = createItem();
        ReflectionTestUtils.setField(item, "averageRating", 0.0);
        ReflectionTestUtils.setField(item, "reviewCount", 0L);

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(new ReviewResponseDto());

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setRating(5L);
        requestDto.setContent("좋아요");

        ReviewResponseDto result = reviewService.save(1L, requestDto, 1L);

        assertThat(result).isNotNull();
        assertThat(item.getReviewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("리뷰 수정: 대상 없음")
    void updateReview_notFound() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(1L, new ReviewRequestDto(), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("리뷰 수정: 권한 없음")
    void updateReview_notAuthorized() {
        Review review = Review.builder().member(createMember(2L)).build();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(1L, new ReviewRequestDto(), 1L))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    @DisplayName("리뷰 수정: 성공")
    void updateReview_success() {
        Member member = createMember(1L);
        Item item = createItem();
        ReflectionTestUtils.setField(item, "averageRating", 4.0);
        ReflectionTestUtils.setField(item, "reviewCount", 1L);

        Review review = Review.builder()
                .member(member)
                .item(item)
                .rating(4L)
                .content("이전")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewMapper.toResponseDto(review)).thenReturn(new ReviewResponseDto());

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setRating(5L);
        requestDto.setContent("변경");

        ReviewResponseDto result = reviewService.updateReview(1L, requestDto, 1L);

        assertThat(result).isNotNull();
        assertThat(review.getRating()).isEqualTo(5L);
        assertThat(review.getContent()).isEqualTo("변경");
        assertThat(item.getAverageRating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("리뷰 삭제: 대상 없음")
    void deleteReview_notFound() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("리뷰 삭제: 권한 없음")
    void deleteReview_notAuthorized() {
        Review review = Review.builder().member(createMember(2L)).build();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 1L))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    @DisplayName("리뷰 삭제: 성공")
    void deleteReview_success() {
        Member member = createMember(1L);
        Item item = createItem();
        ReflectionTestUtils.setField(item, "averageRating", 4.0);
        ReflectionTestUtils.setField(item, "reviewCount", 2L);

        Review review = Review.builder()
                .member(member)
                .item(item)
                .rating(4L)
                .content("삭제")
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L, 1L);

        verify(reviewRepository).delete(review);
        assertThat(item.getReviewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("리뷰 목록 조회")
    void getReviewsByItem() {
        ReviewResponseDto responseDto = new ReviewResponseDto();
        when(reviewRepository.findReviewResponsesByItemId(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(responseDto), PageRequest.of(0, 20), 1));

        assertThat(reviewService.getReviewsByItem(1L, PageRequest.of(0, 20)).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("리뷰 목록 조회(리스트)")
    void getReviewsByItemId() {
        when(reviewRepository.findReviewResponsesByItemId(1L)).thenReturn(List.of(new ReviewResponseDto()));

        assertThat(reviewService.getReviewsByItemId(1L)).hasSize(1);
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .memberName("사용자" + id)
                .memberPwd("pw")
                .tel("01012341234")
                .build();
    }

    private Item createItem() {
        return Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(createMember(2L))
                .build();
    }
}
