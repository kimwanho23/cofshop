package kwh.cofshop.member.service;

import kwh.cofshop.member.domain.MemberLoginHistory;
import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.member.mapper.MemberLoginHistoryMapper;
import kwh.cofshop.member.repository.MemberLoginHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberLoginHistoryServiceTest {

    @Mock
    private MemberLoginHistoryRepository memberLoginHistoryRepository;

    @Mock
    private MemberLoginHistoryMapper memberLoginHistoryMapper;

    @InjectMocks
    private MemberLoginHistoryService memberLoginHistoryService;

    @Test
    @DisplayName("로그인 이력 저장")
    void saveLoginHistory() {
        MemberLoginEvent event = MemberLoginEvent.builder()
                .memberId(1L)
                .loginDt(LocalDateTime.now())
                .build();
        MemberLoginHistory entity = MemberLoginHistory.builder().build();

        when(memberLoginHistoryMapper.toEntity(event)).thenReturn(entity);

        memberLoginHistoryService.saveLoginHistory(event);

        verify(memberLoginHistoryRepository).save(entity);
    }

    @Test
    @DisplayName("로그인 이력 조회")
    void getUserLoginHistory() {
        MemberLoginHistory history = MemberLoginHistory.builder().build();
        when(memberLoginHistoryRepository.findAllByMemberIdOrderByLoginDtDesc(1L)).thenReturn(List.of(history));
        when(memberLoginHistoryMapper.toResponseDto(history)).thenReturn(MemberLoginEvent.builder().build());

        List<MemberLoginEvent> results = memberLoginHistoryService.getUserLoginHistory(1L);

        assertThat(results).hasSize(1);
        verify(memberLoginHistoryRepository).findAllByMemberIdOrderByLoginDtDesc(1L);
    }
}
