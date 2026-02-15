package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ItemSearchMapper;
import kwh.cofshop.item.repository.CategoryRepository;
import kwh.cofshop.item.repository.ItemCategoryRepository;
import kwh.cofshop.item.repository.ItemImgRepository;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.repository.OrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemSearchMapper itemSearchMapper;

    @Mock
    private ItemImgService itemImgService;

    @Mock
    private ItemOptionService itemOptionService;

    @Mock
    private ItemCategoryService itemCategoryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ItemCategoryRepository itemCategoryRepository;

    @Mock
    private ItemOptionRepository itemOptionRepository;

    @Mock
    private ItemImgRepository itemImgRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    @DisplayName("상품 등록: 카테고리 없음")
    void saveItem_categoryNotFound() throws IOException {
        Member member = createMember(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        ItemRequestDto requestDto = createRequestDto();
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.saveItem(requestDto, 1L, List.of(new MockMultipartFile("image", "test.jpg", "image/jpeg", "data".getBytes()))))
                .isInstanceOf(BusinessException.class);
        verify(itemImgService, never()).saveItemImages(any(Item.class), any());
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    @DisplayName("상품 등록: 성공")
    void saveItem_success() throws IOException {
        Member member = createMember(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Item item = createItem();
        ReflectionTestUtils.setField(item, "id", 1L);

        ItemRequestDto requestDto = createRequestDto();
        when(itemMapper.toEntity(requestDto)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemImgService.saveItemImages(any(Item.class), any())).thenReturn(List.of(ItemImg.builder().build()));
        when(itemOptionService.saveItemOptions(any(Item.class), any())).thenReturn(List.of(ItemOption.builder().build()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().name("원두").build()));
        when(itemCategoryRepository.saveAll(any())).thenReturn(List.of(ItemCategory.builder().build()));

        ItemResponseDto responseDto = new ItemResponseDto();
        when(itemMapper.toResponseDto(item)).thenReturn(responseDto);

        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "data".getBytes());

        ItemResponseDto result = itemService.saveItem(requestDto, 1L, List.of(file));

        assertThat(result.getEmail()).isEqualTo(member.getEmail());
    }

    @Test
    @DisplayName("상품 수정: 대상 없음")
    void updateItem_notFound() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.updateItem(1L, 1L, new ItemUpdateRequestDto(), List.of()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("상품 수정: 성공")
    void updateItem_success() throws IOException {
        Item item = createItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toResponseDto(item)).thenReturn(new ItemResponseDto());

        ItemUpdateRequestDto dto = new ItemUpdateRequestDto();
        dto.setItemName("변경");
        dto.setPrice(1200);
        dto.setDeliveryFee(0);
        dto.setOrigin("브라질");
        dto.setItemLimit(10);

        ItemResponseDto result = itemService.updateItem(2L, 1L, dto, List.of());

        assertThat(result).isNotNull();
        verify(itemCategoryService).updateItemCategories(item, dto);
        verify(itemImgService).updateItemImages(item, dto, List.of());
        verify(itemOptionService).updateItemOptions(item, dto);
    }

    @Test
    @DisplayName("상품 수정: 부분 수정 시 null 필드는 유지")
    void updateItem_partialUpdateKeepsExistingValues() throws IOException {
        Item item = createItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toResponseDto(item)).thenReturn(new ItemResponseDto());

        ItemUpdateRequestDto dto = new ItemUpdateRequestDto();
        dto.setItemName("부분수정");

        itemService.updateItem(2L, 1L, dto, List.of());

        assertThat(item.getItemName()).isEqualTo("부분수정");
        assertThat(item.getPrice()).isEqualTo(1000);
        assertThat(item.getOrigin()).isEqualTo("브라질");
    }

    @Test
    @DisplayName("상품 수정: 판매자 불일치")
    void updateItem_forbiddenWhenNotSeller() {
        Item item = createItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemUpdateRequestDto dto = new ItemUpdateRequestDto();
        dto.setItemName("변경");

        assertThatThrownBy(() -> itemService.updateItem(999L, 1L, dto, List.of()))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    @DisplayName("상품 검색")
    void searchItem() {
        Item item = createItem();
        ItemSearchResponseDto responseDto = new ItemSearchResponseDto();

        when(itemRepository.searchItems(any(), any())).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));
        when(itemSearchMapper.toResponseDto(item)).thenReturn(responseDto);

        ItemSearchRequestDto requestDto = new ItemSearchRequestDto();

        assertThat(itemService.searchItem(requestDto, PageRequest.of(0, 20)).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("인기 상품 조회")
    void getPopularItem() {
        Item item = createItem();
        when(orderItemRepository.getPopularItems(3)).thenReturn(List.of(item));
        when(itemMapper.toResponseDto(item)).thenReturn(new ItemResponseDto());

        List<ItemResponseDto> result = itemService.getPopularItem(3);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("상품 단건 조회: 대상 없음")
    void getItem_notFound() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("상품 단건 조회: 성공")
    void getItem_success() {
        Item item = createItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toResponseDto(item)).thenReturn(new ItemResponseDto());

        ItemResponseDto result = itemService.getItem(1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("상품 삭제: 대상 없음")
    void deleteItem_notFound() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.deleteItem(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("상품 삭제: 성공")
    void deleteItem_success() {
        Item item = createItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        itemService.deleteItem(2L, 1L);

        verify(itemRepository).delete(item);
    }

    @Test
    @DisplayName("상품 삭제: 판매자 불일치")
    void deleteItem_forbiddenWhenNotSeller() {
        Item item = createItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.deleteItem(999L, 1L))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    private ItemRequestDto createRequestDto() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setItemName("커피");
        requestDto.setPrice(1000);
        requestDto.setOrigin("브라질");
        requestDto.setItemLimit(10);
        requestDto.setCategoryIds(List.of(1L));
        requestDto.setItemImgRequestDto(List.of(new ItemImgRequestDto(null, 1L, kwh.cofshop.item.domain.ImgType.REPRESENTATIVE)));
        requestDto.setItemOptionRequestDto(List.of(ItemOptionRequestDto.builder()
                .description("기본")
                .additionalPrice(100)
                .stock(10)
                .build()));
        return requestDto;
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .memberName("사용자" + id)
                .memberPwd("pw")
                .tel("01012341234")
                .build();
    }

    private Item createItem() {
        return Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(createMember(2L))
                .build();
    }
}
