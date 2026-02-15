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
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static kwh.cofshop.global.exception.errorcodes.BusinessErrorCode.REVIEW_ALREADY_EXISTS;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ItemRepository itemRepository;
    private final MemberReadPort memberReadPort;

    @Transactional
    public ReviewResponseDto save(Long itemId, ReviewRequestDto requestDto, Long memberId) {
        Member member = memberReadPort.getById(memberId);
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

    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto reviewRequestDto, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }

        Long oldRating = review.getRating();
        Long newRating = reviewRequestDto.getRating();

        review.updateContent(reviewRequestDto.getContent());
        review.updateRating(reviewRequestDto.getRating());

        Item item = review.getItem();
        item.updateReviewRating(oldRating, newRating);
        return reviewMapper.toResponseDto(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }

        Long rating = review.getRating();
        reviewRepository.delete(review);
        review.getItem().deleteReviewRating(rating);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsByItem(Long itemId, Pageable pageable) {
        return reviewRepository.findByItemId(itemId, pageable)
                .map(reviewMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByItemId(Long itemId) {
        return reviewRepository.getReviewsByItemId(itemId).stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }
}
