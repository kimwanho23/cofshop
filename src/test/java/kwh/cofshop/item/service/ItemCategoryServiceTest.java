package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.CategoryRepository;
import kwh.cofshop.item.repository.ItemCategoryRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ItemCategoryRepository itemCategoryRepository;

    @InjectMocks
    private ItemCategoryService itemCategoryService;

    @Test
    @DisplayName("카테고리 삭제: 빈 리스트")
    void deleteItemCategories_empty() {
        Item item = createItem();

        itemCategoryService.deleteItemCategories(item, null);

        verify(itemCategoryRepository, never()).deleteByItemIdAndCategoryIds(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("카테고리 삭제: 성공")
    void deleteItemCategories_success() {
        Item item = createItem();
        ReflectionTestUtils.setField(item, "id", 1L);

        itemCategoryService.deleteItemCategories(item, List.of(1L, 2L));

        verify(itemCategoryRepository).deleteByItemIdAndCategoryIds(1L, List.of(1L, 2L));
    }

    @Test
    @DisplayName("카테고리 추가: 대상 없음")
    void addItemCategories_notFound() {
        Item item = createItem();
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCategoryService.addItemCategories(item, List.of(1L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("카테고리 추가: 성공")
    void addItemCategories_success() {
        Item item = createItem();
        Category category = Category.builder().name("원두").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        itemCategoryService.addItemCategories(item, List.of(1L));

        verify(itemCategoryRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("카테고리 업데이트")
    void updateItemCategories() {
        Item item = createItem();
        ReflectionTestUtils.setField(item, "id", 1L);
        Category category = Category.builder().name("원두").build();
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        ItemUpdateRequestDto dto = new ItemUpdateRequestDto();
        dto.setDeleteCategoryIds(List.of(1L));
        dto.setAddCategoryIds(List.of(2L));

        itemCategoryService.updateItemCategories(item, dto);

        verify(itemCategoryRepository).deleteByItemIdAndCategoryIds(1L, List.of(1L));
        verify(itemCategoryRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
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
