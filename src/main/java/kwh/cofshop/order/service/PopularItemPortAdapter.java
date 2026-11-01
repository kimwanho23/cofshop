package kwh.cofshop.order.service;

import kwh.cofshop.item.api.PopularItemPort;
import kwh.cofshop.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
class PopularItemPortAdapter implements PopularItemPort {

    private static final int CACHED_SIZE = 100;

    private final OrderItemRepository orderItemRepository;

    @Value("${item.popular.cache-seconds:60}")
    private long cacheSeconds;

    private volatile CachedIds cached;

    // 비로그인 공개 API이고 전체 주문 상품을 집계하므로 순위만 잠시 보관한다. 상품 정보는 호출마다 새로 읽는다.
    @Override
    @Transactional(readOnly = true)
    public List<Long> getPopularItemIds(int limit) {
        List<Long> ids = topIds();
        return ids.subList(0, Math.min(limit, ids.size()));
    }

    private List<Long> topIds() {
        CachedIds current = cached;
        if (current != null && current.isValid()) {
            return current.ids();
        }
        synchronized (this) {
            current = cached;
            if (current != null && current.isValid()) {
                return current.ids();
            }
            List<Long> ids = List.copyOf(orderItemRepository.getPopularItemIds(CACHED_SIZE));
            cached = new CachedIds(ids, Instant.now().plusSeconds(cacheSeconds));
            return ids;
        }
    }

    private record CachedIds(List<Long> ids, Instant expiresAt) {
        private boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
