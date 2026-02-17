package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemOptionServiceTest {

    @Mock
    private ItemOptionRepository itemOptionRepository;

    @InjectMocks
    private ItemOptionService itemOptionService;

    @Test
    @DisplayName("옵션 저장")
    void saveItemOptions() {
        Item item = createItem();
        ItemOptionRequestDto dto = new ItemOptionRequestDto(null, "기본", 100, 10);

        when(itemOptionRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(ItemOption.builder().build()));

        List<ItemOption> result = itemOptionService.saveItemOptions(item, List.of(dto));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("옵션 삭제: 빈 리스트")
    void deleteItemOptions_empty() {
        itemOptionService.deleteItemOptions(1L, null);

        verify(itemOptionRepository, never()).deleteByItemIdAndItemOptionId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("옵션 삭제: 성공")
    void deleteItemOptions_success() {
        itemOptionService.deleteItemOptions(1L, List.of(1L, 2L));

        verify(itemOptionRepository).deleteByItemIdAndItemOptionId(1L, List.of(1L, 2L));
    }

    @Test
    @DisplayName("기존 옵션 업데이트")
    void updateExistingItemOptions() {
        Item item = createItem();
        ReflectionTestUtils.setField(item, "id", 1L);

        ItemOption option = ItemOption.builder()
                .description("기본")
                .additionalPrice(100)
                .stock(10)
                .item(item)
                .build();
        ReflectionTestUtils.setField(option, "id", 10L);

        ItemOptionRequestDto dto = new ItemOptionRequestDto(10L, "변경", 200, 5);

        when(itemOptionRepository.findByItemId(1L)).thenReturn(List.of(option));

        itemOptionService.updateExistingItemOptions(item, List.of(dto));

        assertThat(option.getDescription()).isEqualTo("변경");
        assertThat(option.getAdditionalPrice()).isEqualTo(200);
        assertThat(option.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("새 옵션 추가")
    void addNewItemOptions() {
        Item item = createItem();

        ItemOptionRequestDto dto = new ItemOptionRequestDto(null, "신규", 100, 10);

        itemOptionService.addNewItemOptions(item, List.of(dto));

        verify(itemOptionRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("옵션 업데이트")
    void updateItemOptions() {
        Item item = createItem();
        ReflectionTestUtils.setField(item, "id", 1L);

        ItemOption option = ItemOption.builder()
                .description("기본")
                .additionalPrice(100)
                .stock(10)
                .item(item)
                .build();
        ReflectionTestUtils.setField(option, "id", 10L);
        when(itemOptionRepository.findByItemId(1L)).thenReturn(List.of(option));

        ItemUpdateRequestDto dto = new ItemUpdateRequestDto();
        dto.setDeleteOptionIds(List.of(11L));
        dto.setExistingItemOptions(List.of(new ItemOptionRequestDto(10L, "변경", 200, 5)));
        dto.setAddItemOptions(List.of(new ItemOptionRequestDto(null, "신규", 300, 3)));

        itemOptionService.updateItemOptions(item, dto);

        verify(itemOptionRepository).deleteByItemIdAndItemOptionId(1L, List.of(11L));
        verify(itemOptionRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
        assertThat(option.getDescription()).isEqualTo("변경");
    }

    private Item createItem() {
        return Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(Member.builder()
                        .id(1L)
                        .email("seller@example.com")
                        .memberName("판매자")
                        .memberPwd("pw")
                        .tel("01012341234")
                        .build())
                .build();
    }
}
