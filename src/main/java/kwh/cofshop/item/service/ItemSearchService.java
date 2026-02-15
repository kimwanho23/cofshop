/*
package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.ItemDocument;
import kwh.cofshop.item.dto.request.ItemElasticSearchRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.mapper.ItemDocumentMapper;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.repository.elasticSearch.ItemElasticSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ItemSearchService {

    private final ItemRepository itemRepository;
    private final ItemElasticSearchRepository itemElasticSearchRepository;
    private final ItemDocumentMapper itemDocumentMapper;

    public List<ItemResponseDto> searchItemByItemName(ItemElasticSearchRequestDto requestDto) {

        List<ItemDocument> documents = itemElasticSearchRepository.searchItems(requestDto);

        return documents.stream()
                .map(itemDocumentMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public void bulkIndexItems() {
        List<Item> items = itemRepository.findAll(); // 모든 item 정보

        List<ItemDocument> documents = items.stream()
                .map(ItemDocument::of)
                .toList(); //document 변환

        itemElasticSearchRepository.saveAll(documents);
    }
}

*/