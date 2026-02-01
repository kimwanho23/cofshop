package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BusinessErrorCode implements ErrorCode {

    // 주문 관련
    ORDER_NOT_FOUND("ORDER-404", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_CANCELLED("ORDER-CANCELLED", HttpStatus.CONFLICT, "이미 취소된 주문입니다."),
    ORDER_ALREADY_COMPLETED("ORDER-COMPLETED", HttpStatus.CONFLICT, "이미 완료된 주문입니다."),

    // 회원 관련
    MEMBER_NOT_FOUND("MEMBER-404", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS("MEMBER-404", HttpStatus.CONFLICT, "이미 존재하는 회원 이메일입니다."),

    // 상품/옵션 관련
    ITEM_NOT_FOUND("ITEM-404", HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    ITEM_OPTION_NOT_FOUND("ITEM-OPTION-404", HttpStatus.NOT_FOUND, "상품 옵션을 찾을 수 없습니다."),
    ITEM_OUT_OF_STOCK("ITEM-OUT-OF-STOCK", HttpStatus.CONFLICT, "재고가 부족합니다."),

    //쿠폰
    COUPON_NOT_FOUND("COUPON-404", HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_NOT_AVAILABLE("COUPON-NOT-AVAILABLE", HttpStatus.CONFLICT, "쿠폰을 사용할 수 없습니다."),
    COUPON_ALREADY_EXIST("COUPON-ALREADY-EXIST", HttpStatus.CONFLICT, "쿠폰이 이미 존재합니다."),
    COUPON_RUN_OUT("COUPON-RUN-OUT", HttpStatus.GONE, "쿠폰이 전부 소진되었습니다.."),

    // 장바구니
    CART_NOT_FOUND("CART-404", HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."),

    // 리뷰
    REVIEW_NOT_FOUND("REVIEW-404", HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS("REVIEW-409", HttpStatus.CONFLICT, "해당 회원의 리뷰가 이미 존재합니다."),

    // 카테고리
    CATEGORY_NOT_FOUND("CATEGORY-404", HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),

    // 채팅
    CHAT_ROOM_NOT_FOUND("CHAT-404", HttpStatus.NOT_FOUND, "고객 채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ALREADY_CLOSED("CHAT-ROOM-ALREADY-CLOSED", HttpStatus.CONFLICT, "이미 종료된 채팅입니다."),
    CHAT_ROOM_ALREADY_AGENT_EXIST("CHAT-ROOM-ALREADY-AGENT-EXIST", HttpStatus.CONFLICT, "상담사가 이미 배정되어 있습니다."),

    CHAT_MESSAGE_NOT_FOUND("CHAT-MESSAGE-404", HttpStatus.CONFLICT, "해당 채팅을 찾을 수 없습니다."),

    // 결제
    PAYMENT_NOT_FOUND("PAY-404", HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_UID_DISCREPANCY("CHAT-404", HttpStatus.NOT_FOUND, "결제 UID가 일치하지 않습니다."),
    PAYMENT_AMOUNT_DISCREPANCY("CHAT-404", HttpStatus.NOT_FOUND, "결제 금액이 일치하지 않습니다."),
    PAYMENT_CANNOT_REFUND("CANNOT-REFUND", HttpStatus.CONFLICT, "환불이 불가능합니다"),
    PAYMENT_ALREADY_CANCELLED("ALREADY_CANCELLED", HttpStatus.CONFLICT, "이미 취소된 결제입니다."),
    PAYMENT_REFUND_FAIL("REFUND_FAILED", HttpStatus.CONFLICT, "환불에 실패했습니다."),
    PAYMENT_FAIL("REFUND_FAILED", HttpStatus.CONFLICT, "결제에 실패했습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

}
