package kwh.cofshop.item.domain;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemOptionTest {

    @Test
    @DisplayName("옵션 생성: 음수 재고는 실패")
    void createOption_negativeStock() {
        assertThatThrownBy(() -> ItemOption.createOption("기본", 100, -1, createItem()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("옵션 생성: null 상품은 실패")
    void createOption_nullItem() {
        assertThatThrownBy(() -> ItemOption.createOption("기본", 100, 1, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("재고 증가: 0 이하 입력은 실패")
    void addStock_invalidAmount() {
        ItemOption option = createOption(10);

        assertThatThrownBy(() -> option.addStock(0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("재고 증가: int 범위 초과는 실패")
    void addStock_overflow() {
        ItemOption option = createOption(Integer.MAX_VALUE);

        assertThatThrownBy(() -> option.addStock(1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("재고 차감: 0 이하 입력은 실패")
    void removeStock_invalidAmount() {
        ItemOption option = createOption(10);

        assertThatThrownBy(() -> option.removeStock(0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("재고 차감: 보유 재고를 초과하면 실패")
    void removeStock_outOfStock() {
        ItemOption option = createOption(1);

        assertThatThrownBy(() -> option.removeStock(2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ITEM_OUT_OF_STOCK);
    }

    @Test
    @DisplayName("옵션 수정: 음수 재고는 실패")
    void updateOption_negativeStock() {
        ItemOption option = createOption(10);

        assertThatThrownBy(() -> option.updateOption("변경", 100, -1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("옵션 수정: 음수 추가금은 실패")
    void updateOption_negativeAdditionalPrice() {
        ItemOption option = createOption(10);

        assertThatThrownBy(() -> option.updateOption("변경", -1, 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("옵션 수정: 정상 입력이면 반영")
    void updateOption_success() {
        ItemOption option = createOption(10);

        option.updateOption("변경", 200, 5);

        assertThat(option.getDescription()).isEqualTo("변경");
        assertThat(option.getAdditionalPrice()).isEqualTo(200);
        assertThat(option.getStock()).isEqualTo(5);
    }

    private ItemOption createOption(int stock) {
        return ItemOption.builder()
                .description("기본")
                .additionalPrice(100)
                .stock(stock)
                .optionState(OptionState.SELL)
                .item(createItem())
                .build();
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
