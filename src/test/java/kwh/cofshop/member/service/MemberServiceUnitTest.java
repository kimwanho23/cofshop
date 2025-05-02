package kwh.cofshop.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@Slf4j
class MemberServiceUnitTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MemberRequestDto requestDto;

    private Member mockMember;

    private MemberResponseDto mockResponseDto;

    @BeforeEach
    void setUp() {
        requestDto = getMemberRequestDto();

        mockMember = Member.builder()
                .email("test1@gmail.com")
                .memberPwd("1234567890")
                .memberName("테스트")
                .tel("010-1234-5678")
                .build();

        // 테스트 스텁
        mockResponseDto = MemberResponseDto.builder()
                .memberId(1L)
                .email("test1@gmail.com")
                .memberName("테스트")
                .tel("010-1234-5678")
                .point(0)
                .memberState(MemberState.ACTIVE)
                .role(Role.MEMBER)
                .build();
    }


    @Test
    @DisplayName("회원가입 로직 테스트")
    void signUp() {
        // given
        when(memberRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(memberMapper.toResponseDto(any())).thenReturn(mockResponseDto);
        when(memberRepository.save(any())).thenReturn(mockMember);
        when(passwordEncoder.encode(any())).thenReturn("1234567890");

        // when
        MemberResponseDto savedMember = memberService.signUp(requestDto);

        // then
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo(mockResponseDto.getEmail());
        assertThat(savedMember.getMemberState()).isEqualTo(MemberState.ACTIVE);

        // 검증: 요청에 포함된 실제 패스워드가 encode() 호출됐는지 확인
        verify(passwordEncoder).encode(requestDto.getMemberPwd());
    }



    @Test
    @DisplayName("멤버 조회")
    void findMember() {
        // given
        // 멤버 조회를 위한 Mock 설정
        when(memberRepository.findById(any())).thenReturn(Optional.of(mockMember));
        when(memberMapper.toResponseDto(any())).thenReturn(mockResponseDto);

        // when
        // 멤버 조회 서비스 실행
        MemberResponseDto result = memberService.findMember(1L);

        // then
        // 조회된 멤버 정보 검증
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test1@gmail.com");
    }


    @Test
    @DisplayName("멤버 상태 변경")
    void changeMemberState() {
        // given
        // 멤버 상태 변경을 위한 Mock 설정
        when(memberRepository.findById(any())).thenReturn(Optional.of(mockMember));

        // When
        // 멤버 상태 변경 서비스 실행
        memberService.changeMemberState(mockMember.getId(), MemberState.SUSPENDED);

        // Then
        // 상태 변경 검증
        assertThat(mockMember.getMemberState()).isEqualTo(MemberState.SUSPENDED);
    }


    private static MemberRequestDto getMemberRequestDto() {
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("test1@gmail.com");
        requestDto.setMemberName("테스트");
        requestDto.setMemberPwd("1234567890");
        requestDto.setTel("010-1234-5678");
        return requestDto;
    }
}