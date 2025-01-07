package kwh.cofshop.item.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemCreateRequestDto {

    private ItemRequestDto itemRequestDto;
    private ItemImgRequestDto itemImgRequestDto;
    private List<ItemOptionRequestDto> itemOptionRequestDto;
}
