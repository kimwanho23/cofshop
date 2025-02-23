package kwh.cofshop.item.controller;

import kwh.cofshop.ControllerTestSetting;
import kwh.cofshop.item.domain.ImgType;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.service.ItemService;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
class ItemControllerTest extends ControllerTestSetting {

    @Autowired
    private ItemService itemService;

    @Test
    @DisplayName("아이템 등록 통합 테스트")
    @Transactional
    void addItem() throws Exception {
        // 1. 멤버 토큰

        ItemRequestDto requestDto = getItemRequestDto();
        List<MockMultipartFile> imageFiles = getImageFiles();
        List<ItemImgRequestDto> itemImgRequestDto = getImgRequestDto();

        // 3. DTO에 데이터 설정
        requestDto.setItemImgRequestDto(itemImgRequestDto);
        requestDto.setItemOptionRequestDto(getItemOptionRequestDto());


        // 4. JSON 직렬화 (DTO 객체를 문자열로 변환)
        String requestDtoJson = objectMapper.writeValueAsString(requestDto);
        MockMultipartFile itemRequestDtoPart = new MockMultipartFile("itemRequestDto",
                "itemRequestDto.json", "application/json", requestDtoJson.getBytes(StandardCharsets.UTF_8));

        // 5. MockMvc 요청 수행 (MultipartFile 포함)
        mockMvc.perform(multipart("/api/item/create")
                        .file(itemRequestDtoPart)
                        .file(imageFiles.get(0))
                        .file(imageFiles.get(1))
                        .file(imageFiles.get(2))
                        .header("Authorization", "Bearer " + getToken())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andDo(print());
    }


    @Test
    @DisplayName("검색 테스트")
    @Transactional
    void SearchItem() throws Exception {
        // 1. 아이템 검색
        ItemSearchRequestDto itemSearchRequestDto = new ItemSearchRequestDto();
        itemSearchRequestDto.setItemName("커피");

        // 2. JSON 직렬화 (DTO 객체를 문자열로 변환)
        String requestDtoJson = objectMapper.writeValueAsString(itemSearchRequestDto);

        mockMvc.perform(multipart("/api/item/search")
                        .content(requestDtoJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // 이미지 파일
    private List<MockMultipartFile> getImageFiles() {
        List<MockMultipartFile> images = new ArrayList<>();
        images.add(new MockMultipartFile("images", "test.jpg", "image/jpeg", "test data".getBytes()));
        images.add(new MockMultipartFile("images", "test1.jpg", "image/jpeg", "test data 1".getBytes()));
        images.add(new MockMultipartFile("images", "test2.jpg", "image/jpeg", "test data 2".getBytes()));
        return images;
    }

    // 이미지 DTO
    private static List<ItemImgRequestDto> getImgRequestDto() {
        List<ItemImgRequestDto> imgRequestDto = new ArrayList<>();

        ItemImgRequestDto repDto = new ItemImgRequestDto();
        repDto.setImgType(ImgType.REPRESENTATIVE);
        imgRequestDto.add(repDto);

        ItemImgRequestDto subDto1 = new ItemImgRequestDto();
        subDto1.setImgType(ImgType.SUB);
        imgRequestDto.add(subDto1);

        ItemImgRequestDto subDto2 = new ItemImgRequestDto();
        subDto2.setImgType(ImgType.SUB);
        imgRequestDto.add(subDto2);

        return imgRequestDto;
    }


    // 옵션 DTO
    private static List<ItemOptionRequestDto> getItemOptionRequestDto() {
        return List.of(
                createOption("Small Size", 0, 100, 1),
                createOption("Large Size", 500, 50, 2)
        );
    }

    // 옵션 만들기
    private static ItemOptionRequestDto createOption(String description, int additionalPrice, int stock, int optionNo) {
        ItemOptionRequestDto option = new ItemOptionRequestDto();
        option.setDescription(description);
        option.setAdditionalPrice(additionalPrice);
        option.setStock(stock);
        option.setOptionNo(optionNo);
        return option;
    }

    private static ItemRequestDto getItemRequestDto() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setItemName("커피 원두");
        requestDto.setPrice(15000);
        requestDto.setCategories("원두커피");
        requestDto.setOrigin("아르헨티나");
        requestDto.setDiscount(0); // 할인율
        requestDto.setDeliveryFee(1000); // 배송비
        requestDto.setItemLimit(5); // 수량 제한

        return requestDto;
    }
}