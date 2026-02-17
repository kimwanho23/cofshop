package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ItemRepositoryCustom {

    Page<ItemSearchResponseDto> searchItems(ItemSearchRequestDto requestDto, Pageable pageable);

    Optional<ItemResponseDto> findItemResponseById(Long itemId);

    List<ItemResponseDto> findItemResponsesByIds(List<Long> itemIds);

}
