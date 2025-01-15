package kwh.cofshop.item.controller;

import kwh.cofshop.ControllerTestSetting;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.repository.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // POST 요청
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ReviewControllerTest extends ControllerTestSetting {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("리뷰 등록 통합 테스트")
    @Transactional
    void addReview_success() throws Exception {
        // 1. 테스트용 데이터 준비
        Item item = itemRepository.findById(1L).orElseThrow();
        ReviewRequestDto requestDto = getReviewRequestDto(item);
        String requestJson = objectMapper.writeValueAsString(requestDto);

        log.info(requestJson);
        // 2. 요청 수행
        mockMvc.perform(post("/api/review/createReview")
                        .header("Authorization", "Bearer " + getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.data.content").value("리뷰 평점 테스트")) // 경로 수정
                .andExpect(jsonPath("$.body.data.rating").value(5)) // 경로 수정
                .andDo(print());
    }

    private static ReviewRequestDto getReviewRequestDto(Item item) {
        ReviewRequestDto reviewRequestDto  = new ReviewRequestDto();
        reviewRequestDto.setContent("리뷰 평점 테스트");
        reviewRequestDto.setRating(5L);
        reviewRequestDto.setItem(item.getId());
        return reviewRequestDto;
    }
}