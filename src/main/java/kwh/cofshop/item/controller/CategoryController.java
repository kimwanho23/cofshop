package kwh.cofshop.item.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.item.dto.CategoryPathDto;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {

    private final CategoryService categoryService;

    //////////// @GET


    // 특정 카테고리의 자식 카테고리
    @GetMapping("/{categoryId}/children")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategoryChildren(@PathVariable Long categoryId) {
        List<CategoryResponseDto> children = categoryService.getCategoryChild(categoryId);
        return ResponseEntity.ok(ApiResponse.OK(children));
    }

    // 카테고리 경로
    @GetMapping("/{categoryId}/path")
    public ResponseEntity<ApiResponse<List<CategoryPathDto>>> getCategoryPath(@PathVariable Long categoryId) {
        List<CategoryPathDto> categoryPath = categoryService.getCategoryPath(categoryId);
        return ResponseEntity.ok(ApiResponse.OK(categoryPath));
    }

    // 전체 카테고리 목록 (상위 카테고리와 하위 카테고리의 관계가 트리 구조와 유사함)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllCategories() {
        List<CategoryResponseDto> responseDto = categoryService.getAllCategory();
        return ResponseEntity.ok(ApiResponse.OK(responseDto));
    }

    //////////// @POST
    // 카테고리 등록 (관리자)
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
            @RequestBody @Valid CategoryRequestDto requestDto) {
        CategoryResponseDto responseDto = categoryService.createCategory(requestDto);

        return ResponseEntity
                .created(URI.create("/api/categories/" + responseDto.getId()))
                .body(ApiResponse.Created(responseDto));
    }

    //////////// @PUT, PATCH



    //////////// @DELETE

    // 카테고리 삭제 (관리자)
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }


}
