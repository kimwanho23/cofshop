package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.CategoryPathDto;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.mapper.CategoryMapper;
import kwh.cofshop.item.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 생성: 부모 없음")
    void createCategory_withoutParent() {
        CategoryRequestDto dto = new CategoryRequestDto();
        dto.setName("원두");

        Category saved = Category.builder().name("원두").build();
        CategoryResponseDto responseDto = new CategoryResponseDto();

        when(categoryRepository.save(org.mockito.ArgumentMatchers.any(Category.class))).thenReturn(saved);
        when(categoryMapper.toResponseDto(saved)).thenReturn(responseDto);

        CategoryResponseDto result = categoryService.createCategory(dto);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("카테고리 생성: 부모 없음")
    void createCategory_parentNotFound() {
        CategoryRequestDto dto = new CategoryRequestDto();
        dto.setName("원두");
        dto.setParentCategoryId(10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("카테고리 조회: 대상 없음")
    void getCategoryById_notFound() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("카테고리 조회: 성공")
    void getCategoryById_success() {
        Category category = Category.builder().name("원두").build();
        CategoryResponseDto responseDto = new CategoryResponseDto();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponseDto(category)).thenReturn(responseDto);

        CategoryResponseDto result = categoryService.getCategoryById(1L);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("카테고리 경로 조회")
    void getCategoryPath() {
        CategoryPathDto path1 = new CategoryPathDto() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getName() {
                return "하위";
            }

            @Override
            public Long getParentCategoryId() {
                return 2L;
            }
        };
        CategoryPathDto path2 = new CategoryPathDto() {
            @Override
            public Long getId() {
                return 2L;
            }

            @Override
            public String getName() {
                return "상위";
            }

            @Override
            public Long getParentCategoryId() {
                return null;
            }
        };

        List<CategoryPathDto> paths = new ArrayList<>();
        paths.add(path1);
        paths.add(path2);

        when(categoryRepository.findCategoryPath(1L)).thenReturn(paths);

        List<CategoryPathDto> result = categoryService.getCategoryPath(1L);

        assertThat(result.get(0).getName()).isEqualTo("상위");
        assertThat(result.get(1).getName()).isEqualTo("하위");
    }

    @Test
    @DisplayName("카테고리 하위 조회: 없음")
    void getCategoryChild_none() {
        when(categoryRepository.existsByParentCategoryId(1L)).thenReturn(false);

        List<CategoryResponseDto> result = categoryService.getCategoryChild(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("카테고리 하위 조회: 있음")
    void getCategoryChild_hasChild() {
        when(categoryRepository.existsByParentCategoryId(1L)).thenReturn(true);

        Category child = Category.builder().name("하위").build();
        when(categoryRepository.findImmediateChildrenNative(1L)).thenReturn(List.of(child));
        when(categoryMapper.toResponseDto(child)).thenReturn(new CategoryResponseDto());

        List<CategoryResponseDto> result = categoryService.getCategoryChild(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("전체 카테고리 조회(테스트)")
    void getAllCategoryTest() {
        Category category = Category.builder().name("원두").build();
        when(categoryRepository.findAllCategoryWithChild()).thenReturn(List.of(category));
        when(categoryMapper.toResponseDto(category)).thenReturn(new CategoryResponseDto());

        List<CategoryResponseDto> result = categoryService.getAllCategoryTest();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("전체 카테고리 조회")
    void getAllCategory() {
        Category parent = Category.builder().name("상위").build();
        Category child = Category.builder().name("하위").parent(parent).build();
        ReflectionTestUtils.setField(parent, "id", 1L);
        ReflectionTestUtils.setField(child, "id", 2L);

        when(categoryRepository.findAll()).thenReturn(List.of(parent, child));

        List<CategoryResponseDto> result = categoryService.getAllCategory();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("카테고리 삭제: 대상 없음")
    void deleteCategory_notFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("카테고리 삭제: 성공")
    void deleteCategory_success() {
        Category category = Category.builder().name("원두").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(category);
    }
}