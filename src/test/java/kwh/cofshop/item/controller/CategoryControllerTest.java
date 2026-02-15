package kwh.cofshop.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.dto.CategoryPathResponseDto;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import kwh.cofshop.item.service.CategoryService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = StandaloneMockMvcFactory.build(categoryController);
    }

    @Test
    @DisplayName("카테고리 전체 조회")
    void getAllCategories() throws Exception {
        CategoryResponseDto responseDto = new CategoryResponseDto();
        responseDto.setId(1L);

        when(categoryService.getAllCategory()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("하위 카테고리 조회")
    void getCategoryChildren() throws Exception {
        when(categoryService.getCategoryChild(1L)).thenReturn(List.of(new CategoryResponseDto()));

        mockMvc.perform(get("/api/categories/1/children"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("카테고리 경로 조회")
    void getCategoryPath() throws Exception {
        CategoryPathResponseDto pathDto = new CategoryPathResponseDto() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getName() {
                return "원두";
            }

            @Override
            public Long getParentCategoryId() {
                return null;
            }
        };
        when(categoryService.getCategoryPath(1L)).thenReturn(List.of(pathDto));

        mockMvc.perform(get("/api/categories/1/path"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("카테고리 생성")
    void createCategory() throws Exception {
        CategoryResponseDto responseDto = new CategoryResponseDto();
        responseDto.setId(10L);

        when(categoryService.createCategory(org.mockito.ArgumentMatchers.any())).thenReturn(responseDto);

        CategoryRequestDto requestDto = new CategoryRequestDto();
        requestDto.setName("원두");
        requestDto.setDepth(1);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("카테고리 삭제")
    void deleteCategory() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}
