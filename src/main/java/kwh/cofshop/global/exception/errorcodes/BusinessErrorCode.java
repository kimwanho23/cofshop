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
    ORDER_CANNOT_CANCEL("ORDER-CANNOT-CANCEL", HttpStatus.CONFLICT, "현재 상태에서는 주문을 취소할 수 없습니다."),
    ORDER_ALREADY_COMPLETED("ORDER-COMPLETED", HttpStatus.CONFLICT, "이미 완료된 주문입니다."),
    ORDER_CANNOT_CONFIRM("ORDER-CANNOT-CONFIRM", HttpStatus.CONFLICT, "배송 완료 상태에서만 구매 확정할 수 있습니다."),
    ORDER_INVALID_STATE_TRANSITION("ORDER-INVALID-STATE-TRANSITION", HttpStatus.CONFLICT, "허용되지 않은 주문 상태 변경입니다."),
    ORDER_REFUND_REQUEST_IN_PROGRESS("ORDER-REFUND-REQUEST-IN-PROGRESS", HttpStatus.CONFLICT, "환불 요청 처리 중에는 배송 상태를 변경할 수 없습니다."),
    ORDER_REFUND_REQUEST_NOT_ALLOWED("ORDER-REFUND-REQUEST-NOT-ALLOWED", HttpStatus.CONFLICT, "현재 주문 상태에서는 환불 요청을 할 수 없습니다."),
    ORDER_REFUND_REQUEST_ALREADY_REQUESTED("ORDER-REFUND-REQUEST-ALREADY-REQUESTED", HttpStatus.CONFLICT, "이미 처리 중인 환불 요청이 있습니다."),
    ORDER_REFUND_REQUEST_NOT_REQUESTED("ORDER-REFUND-REQUEST-NOT-REQUESTED", HttpStatus.CONFLICT, "접수된 환불 요청이 없습니다."),
    ORDER_REFUND_REQUEST_NOT_APPROVED("ORDER-REFUND-REQUEST-NOT-APPROVED", HttpStatus.CONFLICT, "환불 승인된 요청만 환불할 수 있습니다."),
    ORDER_REFUND_REQUEST_INVALID_STATE_TRANSITION("ORDER-REFUND-REQUEST-INVALID-STATE-TRANSITION", HttpStatus.CONFLICT, "허용되지 않은 환불 요청 상태 변경입니다."),

    // 회원 관련
    MEMBER_NOT_FOUND("MEMBER-404", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS("MEMBER-409", HttpStatus.CONFLICT, "이미 존재하는 회원 이메일입니다."),
    MEMBER_NOT_ADMIN("MEMBER-403", HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
    INVALID_POINT_OPERATION("POINT-400", HttpStatus.BAD_REQUEST, "포인트 요청 값이 유효하지 않습니다."),
    INSUFFICIENT_POINT("POINT-409", HttpStatus.CONFLICT, "보유 포인트가 부족합니다."),

    // 상품/옵션 관련
    ITEM_NOT_FOUND("ITEM-404", HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    ITEM_OPTION_NOT_FOUND("ITEM-OPTION-404", HttpStatus.NOT_FOUND, "상품 옵션을 찾을 수 없습니다."),
    ITEM_OUT_OF_STOCK("ITEM-OUT-OF-STOCK", HttpStatus.CONFLICT, "재고가 부족합니다."),
    ITEM_LIMIT_EXCEEDED("ITEM-LIMIT-EXCEEDED", HttpStatus.CONFLICT, "상품 구매 가능 수량을 초과했습니다."),

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
    PAYMENT_PROVIDER_ERROR("PAY-502", HttpStatus.BAD_GATEWAY, "결제사 연동 중 오류가 발생했습니다."),
    PAYMENT_UID_DISCREPANCY("PAY-UID-MISMATCH", HttpStatus.NOT_FOUND, "결제 UID가 일치하지 않습니다."),
    PAYMENT_AMOUNT_DISCREPANCY("PAY-AMOUNT-MISMATCH", HttpStatus.NOT_FOUND, "결제 금액이 일치하지 않습니다."),
    PAYMENT_ALREADY_PAID("PAY-ALREADY-PAID", HttpStatus.CONFLICT, "이미 결제 완료된 건입니다."),
    PAYMENT_CANNOT_REFUND("CANNOT-REFUND", HttpStatus.CONFLICT, "환불이 불가능합니다"),
    PAYMENT_ALREADY_CANCELLED("ALREADY_CANCELLED", HttpStatus.CONFLICT, "이미 취소된 결제입니다."),
    PAYMENT_REFUND_FAIL("PAY-REFUND-FAILED", HttpStatus.CONFLICT, "환불에 실패했습니다."),
    PAYMENT_FAIL("PAY-FAILED", HttpStatus.CONFLICT, "결제에 실패했습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

}
