package kwh.cofshop.security.service;

import kwh.cofshop.member.event.MemberSessionInvalidatedEvent;
import kwh.cofshop.security.token.AccessTokenRevocationStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberSessionInvalidationListener {

    private final RefreshTokenService refreshTokenService;
    private final AccessTokenRevocationStore accessTokenRevocationStore;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidateRefreshToken(MemberSessionInvalidatedEvent event) {
        if (event == null || event.memberId() == null) {
            return;
        }

        accessTokenRevocationStore.revokeIssuedBeforeNow(event.memberId());
        refreshTokenService.delete(event.memberId());
        log.info("Refresh token invalidated. memberId={}", event.memberId());
    }
}
