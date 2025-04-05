package kwh.cofshop.item.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.service.ReviewService;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ReviewController {

    private final ReviewService reviewService;

    //////////// @GET
    // 한 상품의 리뷰 목록
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDto>>> reviewList(
            @PathVariable Long itemId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReviewResponseDto> responseDto = reviewService.getReviewsByItem(itemId, pageable);
        return ResponseEntity.ok(ApiResponse.OK(responseDto));
    }

    //////////// @POST
    // 리뷰 등록
    @PostMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> addReview(
            @PathVariable Long itemId,
            @Parameter(hidden = true) @LoginMember CustomUserDetails customUserDetails,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto) {

        ReviewResponseDto responseDto = reviewService.save(itemId, reviewRequestDto, customUserDetails.getId());
        return ResponseEntity.created(URI.create("/api/reviews/" + responseDto.getReviewId()))
                .body(ApiResponse.Created(responseDto));
    }

    //////////// @PUT, PATCH
    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> updateReview(
            @PathVariable Long reviewId,
            @Parameter(hidden = true) @LoginMember CustomUserDetails customUserDetails,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto) {

        ReviewResponseDto responseDto = reviewService.updateReview(reviewId, reviewRequestDto, customUserDetails.getId());
        return ResponseEntity.ok(ApiResponse.OK(responseDto));
    }

    //////////// @DELETE
    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @Parameter(hidden = true) @LoginMember CustomUserDetails customUserDetails) {

        reviewService.deleteReview(reviewId, customUserDetails.getId());
        return ResponseEntity.noContent().build();
    }


}
