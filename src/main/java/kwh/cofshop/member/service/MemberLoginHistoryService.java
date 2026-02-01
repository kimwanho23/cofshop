package kwh.cofshop.member.service;


import kwh.cofshop.member.domain.MemberLoginHistory;
import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.member.mapper.MemberLoginHistoryMapper;
import kwh.cofshop.member.repository.MemberLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberLoginHistoryService {

    private final MemberLoginHistoryRepository memberLoginHistoryRepository;
    private final MemberLoginHistoryMapper memberLoginHistoryMapper;

    @Transactional
    public void saveLoginHistory(MemberLoginEvent event) {
        memberLoginHistoryRepository.save(memberLoginHistoryMapper.toEntity(event));
    }

    public List<MemberLoginEvent> getUserLoginHistory(Long memberId) {
        List<MemberLoginHistory> loginHistories = memberLoginHistoryRepository.findAll();
        return loginHistories.stream()
                .map(memberLoginHistoryMapper::toResponseDto)
                .toList();
    }


}
