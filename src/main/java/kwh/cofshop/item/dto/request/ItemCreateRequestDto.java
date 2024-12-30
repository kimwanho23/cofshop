package kwh.cofshop.item.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemCreateRequestDto {

    ItemRequestDto itemRequestDto;
    ItemImgRequestDto itemImgRequestDto;
    List<ItemOptionRequestDto> itemOptionRequestDto;
}
