package kwh.cofshop.order.service;

import kwh.cofshop.order.repository.OrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularItemPortAdapterTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private PopularItemPortAdapter adapter;

    @Test
    @DisplayName("보관 시간 안에는 limit가 달라도 집계 쿼리를 한 번만 실행")
    void cached() {
        ReflectionTestUtils.setField(adapter, "cacheSeconds", 60L);
        when(orderItemRepository.getPopularItemIds(100)).thenReturn(List.of(3L, 1L, 2L));

        assertThat(adapter.getPopularItemIds(2)).containsExactly(3L, 1L);
        assertThat(adapter.getPopularItemIds(5)).containsExactly(3L, 1L, 2L);

        verify(orderItemRepository, times(1)).getPopularItemIds(100);
    }

    @Test
    @DisplayName("판매가 없어 결과가 비어도 보관")
    void cachedEmpty() {
        ReflectionTestUtils.setField(adapter, "cacheSeconds", 60L);
        when(orderItemRepository.getPopularItemIds(100)).thenReturn(List.of());

        assertThat(adapter.getPopularItemIds(10)).isEmpty();
        assertThat(adapter.getPopularItemIds(10)).isEmpty();

        verify(orderItemRepository, times(1)).getPopularItemIds(100);
    }

    @Test
    @DisplayName("보관 시간이 지나면 다시 집계")
    void expired() {
        ReflectionTestUtils.setField(adapter, "cacheSeconds", 0L);
        when(orderItemRepository.getPopularItemIds(100)).thenReturn(List.of(1L), List.of(2L));

        assertThat(adapter.getPopularItemIds(10)).containsExactly(1L);
        assertThat(adapter.getPopularItemIds(10)).containsExactly(2L);
    }

    @Test
    @DisplayName("집계 실패는 보관하지 않고 다음 호출에서 다시 시도")
    void failureNotCached() {
        ReflectionTestUtils.setField(adapter, "cacheSeconds", 60L);
        when(orderItemRepository.getPopularItemIds(100))
                .thenThrow(new IllegalStateException("db"))
                .thenReturn(List.of(1L));

        assertThatThrownBy(() -> adapter.getPopularItemIds(10)).isInstanceOf(IllegalStateException.class);
        assertThat(adapter.getPopularItemIds(10)).containsExactly(1L);
    }
}
