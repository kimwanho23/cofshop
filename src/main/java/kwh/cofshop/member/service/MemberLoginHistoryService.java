package kwh.cofshop.member.service;


import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberLoginHistory;
import kwh.cofshop.member.dto.MemberLoginHistoryDto;
import kwh.cofshop.member.dto.MemberRequestDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberLoginHistoryService {

    private final MemberLoginHistoryRepository memberLoginHistoryRepository;
    private final MemberMapper memberMapper;

/*    @Transactional
    public MemberLoginHistory save(MemberLoginHistoryDto dto){
        return memberLoginHistoryRepository.save(memberMapper.toEntity(dto));*/

/*    public List<MemberLoginHistoryDto> getUserLoginHistoryDto(String userId) {
        List<MemberLoginHistory> loginHistories = memberLoginHistoryRepository.findAll();
        return loginHistories.stream()
                .map(memberMapper::toResponseDto)
                .toList();
    }*/
}
