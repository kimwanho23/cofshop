package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.mapper.CategoryMapper;
import kwh.cofshop.item.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // 카테고리 생성
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto dto) {

        Category parent = null;
        if (dto.getParentCategoryId() != null) {
            parent = categoryRepository.findById(dto.getParentCategoryId())
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));
        }

        Category category = Category.builder()
                .name(dto.getName())
                .parent(parent)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponseDto(savedCategory);
    }

    //특정 카테고리 조회
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));
        return categoryMapper.toResponseDto(category);
    }

/*
    public List<CategoryResponseDto> getAllCategoryTest() {
        List<Category> allCategory = categoryRepository.findAllCategoryWithChild();

        return allCategory.stream().
                map(categoryMapper::toResponseDto).toList();
    }
*/

    // 모든 카테고리 조회
    public List<CategoryResponseDto> getAllCategory() {
        List<Category> allCategory = categoryRepository.findAll();

        Map<Long, CategoryResponseDto> categoryMap = new HashMap<>();
        List<CategoryResponseDto> allCategoryResponseDto = new ArrayList<>(); // 담기 위한 DTO

        for (Category category : allCategory) {
            CategoryResponseDto dto = new CategoryResponseDto();
            dto.setId(category.getId());
            dto.setParentCategoryId(category.getParent() != null ? category.getParent().getId() : null);
            dto.setName(category.getName());
            dto.setDepth(category.getDepth());
            dto.setChildren(new ArrayList<>());
            categoryMap.put(dto.getId(), dto);
        }

        for (CategoryResponseDto dto : categoryMap.values()) {
            if (dto.getParentCategoryId() == null) {
                allCategoryResponseDto.add(dto);
            } else {
                CategoryResponseDto parent = categoryMap.get(dto.getParentCategoryId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
            }
        }
        return allCategoryResponseDto;
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));
        categoryRepository.delete(category);
    }
}
