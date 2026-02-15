package kwh.cofshop.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.service.ReviewService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = StandaloneMockMvcFactory.build(
                reviewController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("리뷰 등록")
    void addReview() throws Exception {
        ReviewResponseDto responseDto = new ReviewResponseDto();
        responseDto.setReviewId(1L);

        when(reviewService.save(anyLong(), any(), anyLong())).thenReturn(responseDto);

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setRating(5L);
        requestDto.setContent("좋아요");

        mockMvc.perform(post("/api/reviews/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("리뷰 목록 조회")
    void reviewList() throws Exception {
        when(reviewService.getReviewsByItem(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(new ReviewResponseDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/reviews/items/1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("리뷰 수정")
    void updateReview() throws Exception {
        when(reviewService.updateReview(anyLong(), any(), anyLong())).thenReturn(new ReviewResponseDto());

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setRating(4L);
        requestDto.setContent("괜찮아요");

        mockMvc.perform(put("/api/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("리뷰 삭제")
    void deleteReview() throws Exception {
        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isNoContent());
    }
}
