package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static kwh.cofshop.global.exception.errorcodes.BusinessErrorCode.REVIEW_ALREADY_EXISTS;

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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));

        Review review = Review.createReview(
                requestDto.getRating(),
                requestDto.getContent(),
                member,
                item
        );

        try {
            reviewRepository.save(review);
            item.addReviewRating(requestDto.getRating());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(REVIEW_ALREADY_EXISTS);
        }
        return reviewMapper.toResponseDto(review);
    }


    // 리뷰 수정
    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto reviewRequestDto, Long memberId) {
        // 1. 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.REVIEW_NOT_FOUND));

        // 2. 작성자 확인
        if (!review.getMember().getId().equals(memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }

        Long oldRating = review.getRating(); // 현재 평점
        Long newRating = reviewRequestDto.getRating(); // 수정할 평점

        // 3. 리뷰 내용 업데이트
        review.updateContent(reviewRequestDto.getContent());
        review.updateRating(reviewRequestDto.getRating());

        Item item = review.getItem();
        item.updateReviewRating(oldRating, newRating);

        // 4. 업데이트된 리뷰를 반환
        return reviewMapper.toResponseDto(review);
    }


    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }

        Long rating = review.getRating();
        reviewRepository.delete(review);
        Item item = review.getItem();
        item.deleteReviewRating(rating);
    }


    // Item의 리뷰 조회(페이징)
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsByItem(Long itemId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByItemId(itemId, pageable);

        return reviews.map(reviewMapper::toResponseDto);
    }

    // Item의 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByItemId(Long itemId) {
        List<Review> reviews = reviewRepository.getReviewsByItemId(itemId);

        return reviews.stream().map(reviewMapper::toResponseDto).toList();
    }


    // Item 평균 평점 및 리뷰 개수 업데이트
    private void updateItemReviewStats(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        Double averageRating = reviewRepository.findAverageRatingByItemId(itemId);
        long reviewCount = reviewRepository.countByItemId(itemId);

        item.updateReviewStats(averageRating, reviewCount);
    }

}
