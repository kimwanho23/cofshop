package kwh.cofshop.item.controller;

import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.item.dto.request.*;
import kwh.cofshop.item.dto.response.ItemCreateResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.service.ItemService;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/create") // 상품 올리기
    public ResponseEntity<ApiResponse<ItemCreateResponseDto>> uploadItem(
            @LoginMember Member member,
            @Valid @RequestBody ItemCreateRequestDto requestDto) throws IOException {

        ItemCreateResponseDto responseDto = itemService.saveItem(requestDto,member); // 아이템 및 이미지 저장
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(responseDto));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<ItemSearchResponseDto>>> searchItems(
            @Valid @RequestBody  ItemSearchRequestDto itemSearchRequestDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ItemSearchResponseDto> itemSearchResponseDto =
                itemService.searchItem(itemSearchRequestDto, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.OK(itemSearchResponseDto));
    }

}
