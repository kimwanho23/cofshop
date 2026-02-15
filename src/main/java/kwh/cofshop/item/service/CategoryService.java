package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.CategoryPathResponseDto;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
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
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND));
        return categoryMapper.toResponseDto(category);
    }

    //자식 카테고리 백트래킹
    public List<CategoryPathResponseDto> getCategoryPath(Long categoryId) {
        List<CategoryPathResponseDto> categoryPath = categoryRepository.findCategoryPath(categoryId);
        Collections.reverse(categoryPath);
        return categoryPath;
    }

    // 해당 카테고리가 마지막인가? 아니면 선택 옵션 제공
    public List<CategoryResponseDto> getCategoryChild(Long categoryId) {
        if (hasChildCategory(categoryId)) {
            List<Category> categories = categoryRepository.findImmediateChildrenNative(categoryId);
            return categories.stream().map(categoryMapper::toResponseDto).toList();
        }
        return Collections.emptyList();
    }


    public boolean hasChildCategory(Long categoryId) {
        return categoryRepository.existsByParentCategoryId(categoryId);
    }


    public List<CategoryResponseDto> getAllCategoryTest() {
        List<Category> allCategory = categoryRepository.findAllCategoryWithChild();

        return allCategory.stream().
                map(categoryMapper::toResponseDto).toList();
    }


    // 모든 카테고리 조회
    public List<CategoryResponseDto> getAllCategory() {
        List<Category> allCategory = categoryRepository.findAll(); // 전체 카테고리를 조회함

        Map<Long, CategoryResponseDto> categoryMap = new LinkedHashMap<>(); // HashMap -> LinkedHashMap (순서 보장)

        for (Category category : allCategory) {
            CategoryResponseDto dto = CategoryResponseDto.from(category);
            categoryMap.put(dto.getId(), dto);
        }

        List<CategoryResponseDto> allCategoryResponseDto = new ArrayList<>(); // 담기 위한 DTO
        for (CategoryResponseDto dto : categoryMap.values()) {
            if (dto.getParentCategoryId() == null) {
                allCategoryResponseDto.add(dto);
            } else {
                categoryMap.get(dto.getParentCategoryId()).getChildren().add(dto);
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
