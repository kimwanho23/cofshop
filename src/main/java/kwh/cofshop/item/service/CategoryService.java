package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.mapper.CategoryMapper;
import kwh.cofshop.item.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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


    // 모든 부모 카테코리 조회 - 계층형 구조로 자식 카테고리까지 가져온다
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findAllByParentIsNull();
        return rootCategories.stream()
                .map(categoryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));
        categoryRepository.delete(category);
    }
}
