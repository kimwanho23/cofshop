package kwh.cofshop.member.event.Listener;

import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.member.service.MemberLoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginEventListener {

    private final MemberLoginHistoryService memberLoginHistoryService;

    @Async
    @EventListener
    public void handleLoginSuccess(MemberLoginEvent dto) {
        memberLoginHistoryService.saveLoginHistory(dto);
    }
}