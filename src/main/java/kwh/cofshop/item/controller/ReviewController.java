package kwh.cofshop.item.controller;

import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.service.ReviewService;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/createReview") // 상품 리뷰 , 한 상품에 리뷰는 하나만 작성 가능
    public ResponseEntity<ApiResponse<ReviewResponseDto>> addReview(
            @LoginMember Member member,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto) throws IOException {

        ReviewResponseDto responseDto = reviewService.save(reviewRequestDto, member); // 리뷰 등록
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(responseDto));
    }
}
