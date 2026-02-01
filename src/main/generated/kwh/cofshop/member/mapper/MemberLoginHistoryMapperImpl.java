package kwh.cofshop.member.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.member.domain.MemberLoginHistory;
import kwh.cofshop.member.event.MemberLoginEvent;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:10+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class MemberLoginHistoryMapperImpl implements MemberLoginHistoryMapper {

    @Override
    public MemberLoginHistory toEntity(MemberLoginEvent memberLoginHistoryDto) {
        if ( memberLoginHistoryDto == null ) {
            return null;
        }

        MemberLoginHistory.MemberLoginHistoryBuilder memberLoginHistory = MemberLoginHistory.builder();

        memberLoginHistory.memberId( memberLoginHistoryDto.getMemberId() );
        memberLoginHistory.loginDt( memberLoginHistoryDto.getLoginDt() );
        memberLoginHistory.ipAddress( memberLoginHistoryDto.getIpAddress() );
        memberLoginHistory.device( memberLoginHistoryDto.getDevice() );

        return memberLoginHistory.build();
    }

    @Override
    public MemberLoginEvent toResponseDto(MemberLoginHistory memberLoginHistory) {
        if ( memberLoginHistory == null ) {
            return null;
        }

        MemberLoginEvent.MemberLoginEventBuilder memberLoginEvent = MemberLoginEvent.builder();

        memberLoginEvent.memberId( memberLoginHistory.getMemberId() );
        memberLoginEvent.loginDt( memberLoginHistory.getLoginDt() );
        memberLoginEvent.ipAddress( memberLoginHistory.getIpAddress() );
        memberLoginEvent.device( memberLoginHistory.getDevice() );

        return memberLoginEvent.build();
    }
}
