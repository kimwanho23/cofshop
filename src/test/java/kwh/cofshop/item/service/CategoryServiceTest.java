package kwh.cofshop.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.request.CategoryPathRequestDto;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.mapper.CategoryMapper;
import kwh.cofshop.item.repository.CategoryRepository;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class CategoryServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    /////////////////// Service

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;


    @Test
    @DisplayName("카테고리 생성")
    @Transactional
    void createParentCategory() throws Exception {
        // 부모 카테고리 생성
        CategoryRequestDto categoryRequestDto = new CategoryRequestDto();
        categoryRequestDto.setName("커피");
        CategoryResponseDto parentCategory = categoryService.createCategory(categoryRequestDto);
        log.info(objectMapper.writeValueAsString(parentCategory));

        // 자식 카테고리 1 생성
        CategoryRequestDto childCategoryDto1 = new CategoryRequestDto();
        childCategoryDto1.setParentCategoryId(parentCategory.getId());
        childCategoryDto1.setName("원두커피");
        CategoryResponseDto childCategoryResponseDto1 = categoryService.createCategory(childCategoryDto1);
        log.info(objectMapper.writeValueAsString(childCategoryResponseDto1));

        // 자식 카테고리 2 생성
        CategoryRequestDto childCategoryDto2 = new CategoryRequestDto();
        childCategoryDto2.setParentCategoryId(parentCategory.getId());
        childCategoryDto2.setName("캡슐커피");
        CategoryResponseDto childCategoryResponseDto2 = categoryService.createCategory(childCategoryDto2);
        log.info(objectMapper.writeValueAsString(childCategoryResponseDto2));

        // 자식 카테고리 1-2 생성
        CategoryRequestDto childCategoryDto1to2 = new CategoryRequestDto();
        childCategoryDto1to2.setParentCategoryId(childCategoryResponseDto1.getId());
        childCategoryDto1to2.setName("에스프레소");
        CategoryResponseDto childCategoryResponseDto1to2 = categoryService.createCategory(childCategoryDto1to2);
        log.info(objectMapper.writeValueAsString(childCategoryResponseDto1to2));

        CategoryResponseDto updatedParentCategory = categoryService.getCategoryById(parentCategory.getId());
        log.info(objectMapper.writeValueAsString(updatedParentCategory));
    }


    @Test
    @DisplayName("특정 카테고리 조회")
    @Transactional
    void getIndividualCategory() throws Exception {
        CategoryResponseDto categoryById = categoryService.getCategoryById(15L);
        log.info(objectMapper.writeValueAsString(categoryById));
    }

    @Test
    @DisplayName("특정 카테고리 경로 조회")
    @Transactional
    void getCategoryPath() throws Exception {
        List<CategoryPathRequestDto> categoryPath = categoryService.getCategoryPath(15L);
        log.info(objectMapper.writeValueAsString(categoryPath));
    }

    @Test
    @DisplayName("특정 카테고리의 자식 조회")
    @Transactional
    void getCategoryChild() throws Exception {
        List<CategoryResponseDto> childCategories = categoryService.getCategoryChild(13L);
        log.info(objectMapper.writeValueAsString(childCategories));
    }



    @Test
    @DisplayName("전체 카테고리 목록")
    @Transactional
    void getAllCategory() throws Exception {
        long startTime = System.nanoTime(); //  실행 시작 시간 기록
        List<CategoryResponseDto> allCategory = categoryService.getAllCategory();
        log.info(objectMapper.writeValueAsString(allCategory));
        long endTime = System.nanoTime(); //  실행 종료 시간 기록
        log.info("실행 시간: {} ms", (endTime - startTime) / 1_000_000); // 실행 시간 로깅
    }

}