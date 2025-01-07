package kwh.cofshop.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.dto.request.*;
import kwh.cofshop.item.dto.response.ItemCreateResponseDto;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    @Commit
    void createItem() throws Exception {

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        // 1. ItemRequestDto
        ItemRequestDto requestDto = getItemRequestDto();

        // 2. ItemImgRequestDto
        ItemImgRequestDto imgRequestDto = getItemImgRequestDto();

        // 3. ItemOptionRequestDto
        List<ItemOptionRequestDto> itemOptionRequestDto = getItemOptionRequestDto();

        ItemCreateRequestDto itemCreateRequestDto = new ItemCreateRequestDto(); // ItemCreateDto 설정
        itemCreateRequestDto.setItemRequestDto(requestDto);
        itemCreateRequestDto.setItemImgRequestDto(imgRequestDto);
        itemCreateRequestDto.setItemOptionRequestDto(itemOptionRequestDto);

        ItemCreateResponseDto responseDto = itemService.saveItem(itemCreateRequestDto, member);

        // ResponseDto JSON 변환
        String itemJson = objectMapper.writeValueAsString(responseDto);
        log.info("Item JSON: {}", itemJson);
    }


    // 이미지 파일 임의 생성
    private static ItemImgRequestDto getItemImgRequestDto() {
        MockMultipartFile repImage = new MockMultipartFile("repImage", "test.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile subImage1 = new MockMultipartFile("subImages", "test1.jpg", "image/jpeg", "test data 1".getBytes());
        MockMultipartFile subImage2 = new MockMultipartFile("subImages", "test2.jpg", "image/jpeg", "test data 2".getBytes());
        List<MultipartFile> subImages = List.of(subImage1, subImage2);

        ItemImgRequestDto imgRequestDto = new ItemImgRequestDto();
        imgRequestDto.setRepImage(repImage);
        imgRequestDto.setSubImages(subImages);
        return imgRequestDto;
    }

    // 옵션 DTO
    private List<ItemOptionRequestDto> getItemOptionRequestDto() {
        return List.of(
                createOption("Small Size", 0, 100),
                createOption("Large Size", 500, 50)
        );
    }

    // 옵션 만들기
    private ItemOptionRequestDto createOption(String description, int additionalPrice, int stock) {
        ItemOptionRequestDto option = new ItemOptionRequestDto();
        option.setDescription(description);
        option.setAdditionalPrice(additionalPrice);
        option.setStock(stock);
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
