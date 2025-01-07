package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BusinessErrorCode implements ErrorCode {

    BUSINESS_ERROR_CODE("I-1", HttpStatus.BAD_REQUEST, "비즈니스 오류"),
    OUT_OF_STOCK("I-2", HttpStatus.CONFLICT, "재고 부족"),
    ORDER_ALREADY_CANCELLED("I-3", HttpStatus.CONFLICT, "이미 취소된 주문입니다."),
    ORDER_ALREADY_COMPLETED("I-4", HttpStatus.CONFLICT, "이미 완료된 주문입니다."),
    ORDER_NOT_FOUND("I-5", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.")
    ;

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

}
