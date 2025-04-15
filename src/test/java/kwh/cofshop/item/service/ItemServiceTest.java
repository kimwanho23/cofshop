package kwh.cofshop.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.domain.*;
import kwh.cofshop.item.dto.request.*;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.mapper.CategoryMapper;
import kwh.cofshop.item.mapper.ItemImgMapper;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ItemOptionMapper;
import kwh.cofshop.item.repository.*;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
class ItemServiceTest {

    /////////////////// Mapper

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemImgMapper itemImgMapper;

    @Autowired
    private ItemOptionMapper itemOptionMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /////////////////// Service

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemOptionRepository itemOptionRepository;

    @Autowired
    private ItemCategoryRepository itemCategoryRepository;

    @Autowired
    private ItemImgRepository itemImgRepository;


    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemImgService itemImgService;

    @Autowired
    private ItemOptionService itemOptionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
        requestDto.setCategoryIds(new ArrayList<>());


        ItemResponseDto responseDto = itemService.saveItem(requestDto, member.getId(), imageFiles);

        // ResponseDto JSON 변환
        String itemJson = objectMapper.writeValueAsString(responseDto);
        log.info("Item JSON: {}", itemJson);
    }

    @Test
    @DisplayName("아이템 수정 테스트")
    @Transactional
    @Commit
    void updateItem() throws Exception {
        Long itemId = 2L;
        Item item = itemRepository.findById(itemId).orElseThrow();

        ItemUpdateRequestDto itemUpdateRequestDto = getUpdateItemRequestDto();

        List<ItemOptionRequestDto> itemOptionDto = itemOptionRepository.findByItemId(item.getId())
                .stream()
                .map(option -> new ItemOptionRequestDto(option.getId(), option.getDescription(), option.getAdditionalPrice(), option.getStock()))
                .toList();
        itemUpdateRequestDto.setExistingItemOptions(itemOptionDto);

        // 기존 이미지 정보
        List<ItemImgRequestDto> itemImgDto = itemImgRepository.findByItemId(item.getId())
                .stream()
                .map(img -> new ItemImgRequestDto(img.getId(), img.getItem().getId(), img.getImgType()))
                .toList();
        itemUpdateRequestDto.setExistingItemImgs(itemImgDto);

        // 기존 카테고리 정보
        List<Long> categoryIds = itemCategoryRepository.findByItemId(item.getId())
                .stream()
                .map(itemCategory -> itemCategory.getCategory().getId())
                .toList();
        itemUpdateRequestDto.setExistingCategoryIds(categoryIds);

        log.info(objectMapper.writeValueAsString(itemUpdateRequestDto));

        itemUpdateRequestDto.setAddItemImgs(null);
        List<MultipartFile> updateImageFiles = null;

        itemUpdateRequestDto.setAddItemOptions(getUpdateItemOptionRequestDto());
        itemUpdateRequestDto.setDeleteImgIds(null); // 기존 이미지 중 삭제할 ID
        itemUpdateRequestDto.setDeleteOptionIds(List.of(104L)); // 기존 옵션 중 삭제할 ID
        itemUpdateRequestDto.setDeleteCategoryIds(List.of());
        itemUpdateRequestDto.setAddCategoryIds(List.of(14L));

        ItemResponseDto itemResponseDto = itemService.updateItem(item.getId(), itemUpdateRequestDto, updateImageFiles);
        log.info(objectMapper.writeValueAsString(itemResponseDto));
    }

    @Test
    @DisplayName("아이템 검색 테스트")
    @Transactional
    void SearchItems() throws Exception {
        ItemSearchRequestDto itemSearchRequestDto = new ItemSearchRequestDto();
        itemSearchRequestDto.setItemName("커피");
        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemSearchResponseDto> itemSearchResponseDtoList = itemService.searchItem(itemSearchRequestDto, pageable);
        log.info(objectMapper.writeValueAsString(itemSearchResponseDtoList));
    }

    @Test
    @DisplayName("아이템 조회 테스트")
    @Transactional
    void getItem() throws Exception {
        ItemResponseDto item = itemService.getItem(2L);
        log.info(objectMapper.writeValueAsString(item));
    }

    // 이미지 파일
    private List<MultipartFile> generateImageFiles() {
        List<MultipartFile> images = new ArrayList<>();
        images.add(new MockMultipartFile("images", "test.jpg", "image/jpeg", "test data".getBytes()));
/*        images.add(new MockMultipartFile("images", "test1.jpg", "image/jpeg", "test data 1".getBytes()));
        images.add(new MockMultipartFile("images", "test2.jpg", "image/jpeg", "test data 2".getBytes()));*/
        return images;
    }

    // 수정할 이미지 파일
    private List<MultipartFile> generateUpdateImageFiles() {
        List<MultipartFile> images = new ArrayList<>();
        images.add(new MockMultipartFile("images", "test3.jpg", "image/jpeg", "test data 3".getBytes()));
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

    // 이미지 DTO
    private static List<ItemImgRequestDto> getUpdateImgRequestDto() {
        List<ItemImgRequestDto> imgRequestDto = new ArrayList<>();
        ItemImgRequestDto subDto = new ItemImgRequestDto();
        subDto.setImgType(ImgType.REPRESENTATIVE);
        imgRequestDto.add(subDto);

        return imgRequestDto;
    }

    // 옵션 DTO
    private List<ItemOptionRequestDto> getItemOptionRequestDto() {
        return List.of(
                createOption("Small Size", 0, 100, 1),
                createOption("Large Size", 500, 50, 2)
        );
    }
    private List<ItemOptionRequestDto> getUpdateItemOptionRequestDto() {
        return List.of(
                createOption("Middle Size", 300, 100, 3)

        );
    }

    // 옵션 만들기
    private ItemOptionRequestDto createOption(String description, int additionalPrice, int stock, int optionNo) {
        ItemOptionRequestDto option = new ItemOptionRequestDto();
        option.setDescription(description);
        option.setAdditionalPrice(additionalPrice);
        option.setStock(stock);
        return option;
    }


    private ItemRequestDto getItemRequestDto() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setItemName("다양한 아이템!!");
        requestDto.setPrice(20000);
        requestDto.setOrigin("아르헨티나");
        requestDto.setDiscount(0); // 할인율
        requestDto.setDeliveryFee(1000); // 배송비
        requestDto.setItemLimit(5); // 수량 제한
        return requestDto;
    }

    // 업데이트
    private ItemUpdateRequestDto getUpdateItemRequestDto() {
        ItemUpdateRequestDto requestDto = new ItemUpdateRequestDto();

        Item item = itemRepository.findById(2L).orElseThrow();
        ItemResponseDto responseDto = itemMapper.toResponseDto(item);

        // 기본 정보
        requestDto.setItemName(responseDto.getItemName());
        requestDto.setPrice(responseDto.getPrice());
        requestDto.setOrigin(responseDto.getOrigin());
        requestDto.setDiscount(responseDto.getDiscount()); // 할인율
        requestDto.setDeliveryFee(responseDto.getDeliveryFee()); // 배송비
        requestDto.setItemLimit(responseDto.getItemLimit()); // 수량 제한


        return requestDto;
    }
}
