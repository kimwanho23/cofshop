package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.mapper.ReviewMapper;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.repository.ReviewRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    // 리뷰 생성
    @Transactional
    public ReviewResponseDto save(Long itemId, ReviewRequestDto requestDto, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();

        if (reviewRepository.existsByItemIdAndMemberId(itemId, memberId)) {
            throw new BusinessException(BusinessErrorCode.REVIEW_ALERADY_EXIST); // 해당 회원의 리뷰 존재
        }

        Review review = Review.createReview(
                requestDto.getRating(),
                requestDto.getContent(),
                member,
                item
        );

        reviewRepository.save(review);
        updateItemReviewStats(item.getId());
        return reviewMapper.toResponseDto(review);
    }

    // 리뷰 수정
    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto reviewRequestDto, Long memberId) {
        // 1. 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // 2. 작성자 확인
        if (!review.getMember().getId().equals(memberId)) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.MEMBER_UNAUTHORIZED);
        }
        // 3. 리뷰 내용 업데이트
        review.updateContent(reviewRequestDto.getContent());
        review.updateRating(reviewRequestDto.getRating());

        updateItemReviewStats(review.getItem().getId());

        // 4. 업데이트된 리뷰를 반환
        return reviewMapper.toResponseDto(review);
    }

    // Item 평균 평점 및 리뷰 개수 업데이트
    private void updateItemReviewStats(Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        List<Review> reviews = reviewRepository.findByItemId(itemId);

        int reviewCount = reviews.size();
        double averageRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        item.updateReviewStats(averageRating, reviewCount);
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));

        if (!review.getMember().getId().equals(memberId)) {
            throw new BusinessException(UnauthorizedErrorCode.MEMBER_UNAUTHORIZED);
        }
        reviewRepository.delete(review);
    }


    // Item의 리뷰 조회
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsByItem(Long itemId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByItemId(itemId, pageable);

        return reviews.map(reviewMapper::toResponseDto);
    }

}
