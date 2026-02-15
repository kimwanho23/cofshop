package kwh.cofshop.cart.event;

import kwh.cofshop.cart.service.CartService;
import kwh.cofshop.member.event.MemberCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventListener {

    private final CartService cartService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void createCart(MemberCreatedEvent event) { // 회원가입 시 장바구니 생성
        log.info("회원가입 완료 - 장바구니 생성");
        cartService.createCart(event.memberId());
    }
}
