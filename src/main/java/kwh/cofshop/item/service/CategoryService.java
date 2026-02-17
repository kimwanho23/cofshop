package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.repository.projection.CategoryPathProjection;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryPathResponseDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.mapper.CategoryMapper;
import kwh.cofshop.item.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND));
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
    public CategoryResponseDto getCategoryById(Long categoryId) {
        CategoryResponseDto categoryResponseDto = categoryRepository.findCategoryResponseById(categoryId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND));
        categoryResponseDto.setChildren(new ArrayList<>());
        return categoryResponseDto;
    }

    //자식 카테고리 백트래킹
    public List<CategoryPathResponseDto> getCategoryPath(Long categoryId) {
        List<CategoryPathProjection> categoryPath = categoryRepository.findCategoryPath(categoryId);
        List<CategoryPathResponseDto> responses = new ArrayList<>(categoryPath.size());
        for (int i = categoryPath.size() - 1; i >= 0; i--) {
            CategoryPathProjection projection = categoryPath.get(i);
            responses.add(new CategoryPathResponseDto(
                    projection.getId(),
                    projection.getName(),
                    projection.getParentCategoryId()
            ));
        }
        return responses;
    }

    // 해당 카테고리가 마지막인가? 아니면 선택 옵션 제공
    public List<CategoryResponseDto> getCategoryChild(Long categoryId) {
        List<CategoryResponseDto> childCategories = categoryRepository.findChildCategoryResponses(categoryId);
        childCategories.forEach(child -> child.setChildren(new ArrayList<>()));
        return childCategories;
    }


    public boolean hasChildCategory(Long categoryId) {
        return categoryRepository.existsByParentCategoryId(categoryId);
    }


    public List<CategoryResponseDto> getAllCategoryTest() {
        List<CategoryResponseDto> categories = categoryRepository.findAllCategoryResponses();
        categories.forEach(category -> category.setChildren(new ArrayList<>()));
        return categories;
    }


    // 모든 카테고리 조회
    public List<CategoryResponseDto> getAllCategory() {
        List<CategoryResponseDto> allCategory = categoryRepository.findAllCategoryResponses();

        Map<Long, CategoryResponseDto> categoryMap = new LinkedHashMap<>(); // HashMap -> LinkedHashMap (순서 보장)

        for (CategoryResponseDto category : allCategory) {
            category.setChildren(new ArrayList<>());
            categoryMap.put(category.getId(), category);
        }

        List<CategoryResponseDto> allCategoryResponseDto = new ArrayList<>(); // 담기 위한 DTO
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
        return allCategoryResponseDto; // DTO 리턴
    }


    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.delete(category);
    }
}
