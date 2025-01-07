package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.repository.ItemOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ItemOptionService {

    private final ItemOptionRepository itemOptionRepository;

    @Transactional
    public List<ItemOption> saveItemOptions(Item item, List<ItemOptionRequestDto> optionRequestDto) {
        List<ItemOption> itemOptions = optionRequestDto.stream()
                .map(dto -> ItemOption.createOption(
                        dto.getDescription(),
                        dto.getAdditionalPrice(),
                        dto.getOptionNo(),
                        dto.getStock(),
                        item
                ))
                .toList();
        return itemOptionRepository.saveAll(itemOptions);
    }

}
