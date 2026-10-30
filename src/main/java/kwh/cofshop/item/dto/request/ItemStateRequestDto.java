package kwh.cofshop.item.dto.request;

import jakarta.validation.constraints.NotNull;
import kwh.cofshop.item.domain.ItemState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemStateRequestDto {

    @NotNull
    private ItemState itemState;
}
