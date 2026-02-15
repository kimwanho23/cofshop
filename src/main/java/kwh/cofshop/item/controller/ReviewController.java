package kwh.cofshop.item.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    @Operation(summary = "리뷰 목록 조회", description = "조회 결과")
    @GetMapping("/items/{itemId}")
    public Page<ReviewResponseDto> reviewList(
            @PathVariable Long itemId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return reviewService.getReviewsByItem(itemId, pageable);
    }

    //////////// @POST
    // 리뷰 등록
    @Operation(summary = "리뷰 등록", description = "한 상품에 리뷰를 등록합니다.")
    @PostMapping("/items/{itemId}")
    public ResponseEntity<ReviewResponseDto> addReview(
            @PathVariable Long itemId,
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto) {

        ReviewResponseDto responseDto = reviewService.save(itemId, reviewRequestDto, memberId);
        return ResponseEntity.created(URI.create("/api/reviews/" + responseDto.getReviewId()))
                .body(responseDto);
    }

    //////////// @PUT, PATCH
    // 리뷰 수정
    @Operation(summary = "리뷰 수정", description = "등록했던 리뷰 정보를 변경합니다.")
    @PutMapping("/{reviewId}")
    public ReviewResponseDto updateReview(
            @PathVariable Long reviewId,
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto) {

        return reviewService.updateReview(reviewId, reviewRequestDto, memberId);
    }

    //////////// @DELETE
    // 리뷰 삭제
    @Operation(summary = "리뷰 삭제", description = "리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @Parameter(hidden = true) @LoginMember Long memberId) {

        reviewService.deleteReview(reviewId, memberId);
        return ResponseEntity.noContent().build();
    }


}
