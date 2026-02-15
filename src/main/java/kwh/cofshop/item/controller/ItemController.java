package kwh.cofshop.item.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.global.annotation.LoginMember;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ItemController {

    private final ItemService itemService;
    //////////// @GET

    // 상품 정보 조회
    @Operation(summary = "상품 정보 조회", description = "조회 결과")
    @GetMapping("/{itemId}")
    public ItemResponseDto inquiryItem(@PathVariable Long itemId) {
        ItemResponseDto responseDto = itemService.getItem(itemId);
        return responseDto;
    }

    // 많이 팔린 상품 조회 (기본적으로 10개씩 조회)
    @Operation(summary = "인기 상품 조회", description = "판매순으로 10개씩 조회합니다.")
    @GetMapping("/populars")
    public List<ItemResponseDto> popularItems(
            @RequestParam(defaultValue = "10") int limit) {
        List<ItemResponseDto> popularItem = itemService.getPopularItem(limit);
        return popularItem;
    }


    //////////// @POST

    // 상품 등록
    @Operation(summary = "상품 등록", description = "판매상품을 등록합니다.")
    @PostMapping("")
    public ResponseEntity<ItemResponseDto> uploadItem(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestPart("itemRequestDto") @Valid ItemRequestDto itemRequestDto,
            @RequestPart("images") List<MultipartFile> images) throws Exception {

        // 서비스 호출 (DTO + 파일 리스트 전달)
        ItemResponseDto responseDto = itemService.saveItem(itemRequestDto, memberId, images);

        return ResponseEntity.created(URI.create("/api/item/" + responseDto.getId()))
                .body(responseDto);
    }


    // 상품 검색
    @Operation(summary = "상품 검색", description = "검색 결과")
    @PostMapping("/search")
    public Page<ItemSearchResponseDto> searchItems(
            @Valid @RequestBody ItemSearchRequestDto itemSearchRequestDto) {
        Page<ItemSearchResponseDto> itemSearchResponseDto =
                itemService.searchItem(itemSearchRequestDto, itemSearchRequestDto.toPageable());
        return itemSearchResponseDto;
    }


    //////////// @PUT, PATCH
    // 상품 수정
    @Operation(summary = "상품 정보 수정", description = "상품 정보를 수정합니다.")
    @PutMapping("/{itemId}")
    public ItemResponseDto updateItem(
            @PathVariable Long itemId,
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestPart("itemRequestDto") @Valid ItemUpdateRequestDto itemRequestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {

        List<MultipartFile> safeImages = images == null ? Collections.emptyList() : images;
        ItemResponseDto updatedItem = itemService.updateItem(memberId, itemId, itemRequestDto, safeImages);
        return updatedItem;
    }

    //////////// @DELETE
    // 상품 삭제
    @Operation(summary = "상품 삭제", description = "상품 정보를 삭제합니다.")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId,
            @Parameter(hidden = true) @LoginMember Long memberId) {
        itemService.deleteItem(memberId, itemId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
