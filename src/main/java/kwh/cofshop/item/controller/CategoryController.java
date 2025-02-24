package kwh.cofshop.item.controller;

import jakarta.validation.Valid;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
            @RequestBody @Valid CategoryRequestDto requestDto) {
        CategoryResponseDto responseDto = categoryService.createCategory(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(responseDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.OK(null));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategoryTree() {
        List<CategoryResponseDto> responseDto = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.OK(responseDto));
    }
}
