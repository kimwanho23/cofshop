package kwh.cofshop.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.domain.ImgType;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.service.ItemService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = StandaloneMockMvcFactory.build(
                itemController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("상품 검색")
    void searchItem() throws Exception {
        ItemSearchResponseDto item = new ItemSearchResponseDto();
        item.setItemName("커피");

        Page<ItemSearchResponseDto> response = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

        when(itemService.searchItem(any(), any())).thenReturn(response);

        ItemSearchRequestDto requestDto = new ItemSearchRequestDto();
        requestDto.setItemName("커피");

        mockMvc.perform(post("/api/item/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("상품 정보 조회")
    void inquiryItem() throws Exception {
        when(itemService.getItem(anyLong())).thenReturn(new ItemResponseDto());

        mockMvc.perform(get("/api/item/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인기 상품 조회")
    void popularItems() throws Exception {
        when(itemService.getPopularItem(5)).thenReturn(List.of(new ItemResponseDto()));

        mockMvc.perform(get("/api/item/populars")
                        .param("limit", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("상품 등록")
    void addItem() throws Exception {
        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(1L);

        when(itemService.saveItem(any(), anyLong(), anyList())).thenReturn(responseDto);

        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setItemName("커피 원두");
        requestDto.setPrice(1000);
        requestDto.setOrigin("브라질");
        requestDto.setItemImages(List.of(new ItemImgRequestDto(null, ImgType.REPRESENTATIVE)));
        requestDto.setItemOptions(List.of(new ItemOptionRequestDto(null, "기본", 0, 10)));
        requestDto.setCategoryIds(List.of(1L));

        String requestJson = objectMapper.writeValueAsString(requestDto);
        MockMultipartFile itemRequestDtoPart = new MockMultipartFile(
                "itemRequest",
                "itemRequest.json",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile imageFile = new MockMultipartFile(
                "images",
                "test.jpg",
                "image/jpeg",
                "test data".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/item")
                        .file(itemRequestDtoPart)
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("상품 등록: 이미지 파트 없이 임시 이미지 ID로 요청 가능")
    void addItem_withoutImages() throws Exception {
        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(1L);

        when(itemService.saveItem(any(), anyLong(), any())).thenReturn(responseDto);

        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setItemName("커피 원두");
        requestDto.setPrice(1000);
        requestDto.setOrigin("브라질");
        requestDto.setItemImages(List.of(new ItemImgRequestDto(null, 10L, ImgType.REPRESENTATIVE)));
        requestDto.setItemOptions(List.of(new ItemOptionRequestDto(null, "기본", 0, 10)));
        requestDto.setCategoryIds(List.of(1L));

        String requestJson = objectMapper.writeValueAsString(requestDto);
        MockMultipartFile itemRequestDtoPart = new MockMultipartFile(
                "itemRequest",
                "itemRequest.json",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/item")
                        .file(itemRequestDtoPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("상품 수정")
    void updateItem() throws Exception {
        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(1L);

        when(itemService.updateItem(anyLong(), anyLong(), any(), anyList())).thenReturn(responseDto);

        ItemUpdateRequestDto requestDto = new ItemUpdateRequestDto();
        requestDto.setItemName("커피 변경");

        String requestJson = objectMapper.writeValueAsString(requestDto);
        MockMultipartFile itemRequestDtoPart = new MockMultipartFile(
                "itemRequest",
                "itemRequest.json",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile imageFile = new MockMultipartFile(
                "images",
                "update.jpg",
                "image/jpeg",
                "test data".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/item/1")
                        .file(itemRequestDtoPart)
                        .file(imageFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("상품 삭제")
    void deleteItem() throws Exception {
        mockMvc.perform(delete("/api/item/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("상품 수정: 이미지 파트 없이도 가능")
    void updateItem_withoutImages() throws Exception {
        ItemResponseDto responseDto = new ItemResponseDto();
        responseDto.setId(1L);

        when(itemService.updateItem(anyLong(), anyLong(), any(), anyList())).thenReturn(responseDto);

        ItemUpdateRequestDto requestDto = new ItemUpdateRequestDto();
        requestDto.setItemName("커피 변경");

        String requestJson = objectMapper.writeValueAsString(requestDto);
        MockMultipartFile itemRequestDtoPart = new MockMultipartFile(
                "itemRequest",
                "itemRequest.json",
                "application/json",
                requestJson.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/item/1")
                        .file(itemRequestDtoPart)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }
}
