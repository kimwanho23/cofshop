package kwh.cofshop.item.controller;

import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.item.dto.request.*;
import kwh.cofshop.item.dto.response.ItemResponseDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ItemResponseDto>> uploadItem(
            @LoginMember Member member,
            @RequestPart("itemRequestDto") @Valid ItemRequestDto itemRequestDto,
            @RequestPart("images") List<MultipartFile> images) throws Exception {

        // 서비스 호출 (DTO + 파일 리스트 전달)
        ItemResponseDto responseDto = itemService.saveItem(itemRequestDto, member, images);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(responseDto));
    }


    // 상품 검색
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
