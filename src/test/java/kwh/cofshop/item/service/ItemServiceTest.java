package kwh.cofshop.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.domain.ImgType;
import kwh.cofshop.item.dto.request.*;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
class ItemServiceTest {

    /////////////////// Mapper

    @Autowired
    private ObjectMapper objectMapper;

    /////////////////// Service

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemImgService itemImgService;

    @Autowired
    private ItemOptionService itemOptionService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("아이템 등록 테스트")
    @Transactional
    void createItem() throws Exception {

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        // 1. ItemRequestDto
        ItemRequestDto requestDto = getItemRequestDto();

        // 2. ImgRequestDto, MultiPartFile
        List<MultipartFile> imageFiles = generateImageFiles();


        // 3. ItemOptionRequestDto
        List<ItemOptionRequestDto> itemOptionRequestDto = getItemOptionRequestDto();
        List<ItemImgRequestDto> itemImgRequestDto = getImgRequestDto();

        requestDto.setItemImgRequestDto(itemImgRequestDto);
        requestDto.setItemOptionRequestDto(itemOptionRequestDto); // 옵션 설정


        ItemResponseDto responseDto = itemService.saveItem(requestDto, member.getId(), imageFiles);

        // ResponseDto JSON 변환
        String itemJson = objectMapper.writeValueAsString(responseDto);
        log.info("Item JSON: {}", itemJson);
    }

    @Test
    @DisplayName("아이템 검색 테스트")
    @Transactional
    void SearchItems() throws Exception {
        ItemSearchRequestDto itemSearchRequestDto = new ItemSearchRequestDto();
        itemSearchRequestDto.setCategory(null);
        itemSearchRequestDto.setItemName("커피");
        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemSearchResponseDto> itemSearchResponseDtoList = itemService.searchItem(itemSearchRequestDto, pageable);
        log.info(objectMapper.writeValueAsString(itemSearchResponseDtoList));
    }

    // 이미지 파일
    private List<MultipartFile> generateImageFiles() {
        List<MultipartFile> images = new ArrayList<>();
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
    private List<ItemOptionRequestDto> getItemOptionRequestDto() {
        return List.of(
                createOption("Small Size", 0, 100, 1),
                createOption("Large Size", 500, 50, 2)
        );
    }

    // 옵션 만들기
    private ItemOptionRequestDto createOption(String description, int additionalPrice, int stock, int optionNo) {
        ItemOptionRequestDto option = new ItemOptionRequestDto();
        option.setDescription(description);
        option.setAdditionalPrice(additionalPrice);
        option.setStock(stock);
        option.setOptionNo(optionNo);
        return option;
    }

    private ItemRequestDto getItemRequestDto() {
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
