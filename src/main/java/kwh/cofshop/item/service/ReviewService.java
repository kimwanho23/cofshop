package kwh.cofshop.item.service;

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

    @Transactional
    public ReviewResponseDto save(ReviewRequestDto requestDto) {
        Member member = getMember();
        Item item = findItemById(requestDto.getItemId());
        Review review = Review.createReview(
                requestDto.getRating(),
                requestDto.getContent(),
                member,
                item
        );

        reviewRepository.save(review);
        return reviewMapper.toResponseDto(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        List<Review> reviews = item.getReviews();

        // Mapper를 이용해 DTO로 변환
        return reviews.stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    private Member getMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("인증 성공: {}", authentication.getName());

        return memberRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
    }

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
    }
}
