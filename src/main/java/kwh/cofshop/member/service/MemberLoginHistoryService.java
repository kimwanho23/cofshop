package kwh.cofshop.member.service;


import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
