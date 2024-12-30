package kwh.cofshop.item.controller;

import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.item.dto.request.*;
import kwh.cofshop.item.dto.response.ItemCreateResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.service.ItemService;
import kwh.cofshop.item.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ReviewService reviewService;


    @PostMapping("/create") // 상품 올리기
    public ResponseEntity<ApiResponse<ItemCreateResponseDto>> uploadItem(
            @RequestBody ItemCreateRequestDto requestDto) throws IOException {

        ItemCreateResponseDto responseDto = itemService.save(requestDto); // 아이템 및 이미지 저장
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(responseDto));
    }



    @PostMapping("/review")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> addReview(
            @RequestBody ReviewRequestDto reviewRequestDto) throws IOException {
        ReviewResponseDto responseDto = reviewService.save(reviewRequestDto); // 리뷰 등록
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(responseDto));
    }
}
