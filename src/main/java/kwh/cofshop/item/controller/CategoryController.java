package kwh.cofshop.item.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.item.dto.CategoryPathResponseDto;
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
    @Operation(summary = "하위 카테고리 조회", description = "조회 결과")
    @GetMapping("/{categoryId}/children")
    public List<CategoryResponseDto> getCategoryChildren(@PathVariable Long categoryId) {
        List<CategoryResponseDto> children = categoryService.getCategoryChild(categoryId);
        return children;
    }

    // 카테고리 경로
    @Operation(summary = "카테고리 경로 목록", description = "상위 카테고리부터 하위 카테고리를 조회합니다.")
    @GetMapping("/{categoryId}/path")
    public List<CategoryPathResponseDto> getCategoryPath(@PathVariable Long categoryId) {
        List<CategoryPathResponseDto> categoryPath = categoryService.getCategoryPath(categoryId);
        return categoryPath;
    }

    // 전체 카테고리 목록 (상위 카테고리와 하위 카테고리의 관계가 트리 구조와 유사함)
    @Operation(summary = "전체 카테고리 조회", description = "전체 카테고리를 조회합니다.")
    @GetMapping
    public List<CategoryResponseDto> getAllCategories() {
        List<CategoryResponseDto> responseDto = categoryService.getAllCategory();
        return responseDto;
    }

    //////////// @POST
    // 카테고리 등록 (관리자)
    @Operation(summary = "카테고리 등록", description = "관리자 전용입니다.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> createCategory(
            @RequestBody @Valid CategoryRequestDto requestDto) {
        CategoryResponseDto responseDto = categoryService.createCategory(requestDto);

        return ResponseEntity
                .created(URI.create("/api/categories/" + responseDto.getId()))
                .body(responseDto);
    }

    //////////// @PUT, PATCH


    //////////// @DELETE

    // 카테고리 삭제 (관리자)
    @Operation(summary = "카테고리 삭제", description = "관리자 전용입니다.")
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }


}
