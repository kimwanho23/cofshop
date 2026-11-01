package kwh.cofshop.member.service;

import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.member.mapper.MemberLoginHistoryMapper;
import kwh.cofshop.member.repository.MemberLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberLoginHistoryService {

    private static final int MAX_HISTORY_SIZE = 100;

    private final MemberLoginHistoryRepository memberLoginHistoryRepository;
    private final MemberLoginHistoryMapper memberLoginHistoryMapper;

    @Transactional
    public void saveLoginHistory(MemberLoginEvent event) {
        memberLoginHistoryRepository.save(memberLoginHistoryMapper.toEntity(event));
    }

    @Transactional
    public int deleteLoginHistoryBefore(LocalDateTime cutoff, int batchSize) {
        List<Long> ids = memberLoginHistoryRepository.findIdsLoggedInBefore(cutoff, PageRequest.of(0, Math.max(1, batchSize)));
        if (ids.isEmpty()) {
            return 0;
        }
        memberLoginHistoryRepository.deleteAllByIdInBatch(ids);
        return ids.size();
    }

    public List<MemberLoginEvent> getUserLoginHistory(Long memberId) {
        return memberLoginHistoryRepository.findLoginEventsByMemberId(memberId, PageRequest.of(0, MAX_HISTORY_SIZE));
    }


}
