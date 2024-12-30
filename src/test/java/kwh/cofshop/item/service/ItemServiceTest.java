package kwh.cofshop.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ReviewRequestDto;
import kwh.cofshop.item.dto.response.ItemCreateResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.item.mapper.ItemCreateMapper;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ReviewMapper;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
class ItemServiceTest {

    @Autowired
    private ObjectMapper objectMapper;  // JSON 변환 도구

    @Autowired
    private ItemMapper itemMapper;  // JSON 변환 도구

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ItemImgService itemImgService;

    @Autowired
    private ItemOptionService itemOptionService;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ItemCreateMapper itemCreateMapper;


    @Test
    @DisplayName("아이템 등록 테스트")
    @Transactional
    void createItem() throws Exception {

        // 1. 판매자 조회
        Member seller = memberRepository.findByEmail("test@gmail.com")
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        // 2. ItemRequestDto 생성 및 엔티티 변환
        ItemRequestDto requestDto = getItemRequestDto();
        Item entity = itemMapper.toEntity(requestDto);
        entity.setSeller(seller);

        // 3. Item 저장
        Item savedItem = itemRepository.save(entity);

        // 4. ItemImgRequestDto 생성, 이미지 저장
        ItemImgRequestDto imgRequestDto = getItemImgRequestDto();
        itemImgService.saveItemImages(savedItem, imgRequestDto);

        // 4. ItemOptionRequestDto 생성, 옵션 저장
        List<ItemOptionRequestDto> itemOptionRequestDto = getItemOptionRequestDto();
        itemOptionService.saveItemOptions(savedItem, itemOptionRequestDto);

        // 5. ItemCreateResponseDto 생성 (응답 객체)
        ItemCreateResponseDto responseDto = itemCreateMapper.toResponseDto(savedItem);


        responseDto.getItemResponseDto().setEmail(seller.getEmail());

        // 연관관계 List 추가 시, 확장성 있게 가져갈 수 있다.
        // 최종 ResponseDto JSON 변환
        String itemJson = objectMapper.writeValueAsString(responseDto);
        log.info("Item JSON: {}", itemJson);
    }

    @Test
    @DisplayName("리뷰 작성")
    @Transactional
    void createReview() throws Exception {

        Member member = memberRepository.findByEmail("test@gmail.com")
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Item item = itemRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        ReviewRequestDto reviewRequestDto  = new ReviewRequestDto();
        reviewRequestDto.setContent("괜찮은 것 같은 상품");
        reviewRequestDto.setRating(5L);

        Review review = Review.createReview(reviewRequestDto.getRating(),
                reviewRequestDto.getContent(), member, item);

        ReviewResponseDto responseDto = reviewMapper.toResponseDto(review);


        String reviewJson = objectMapper.writeValueAsString(responseDto);
        log.info("review Json : {}", reviewJson);

    }

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

    private List<ItemOptionRequestDto> getItemOptionRequestDto() {
        return List.of(
                createOption("Small Size", 0, 100),
                createOption("Large Size", 500, 50)
        );
    }

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
