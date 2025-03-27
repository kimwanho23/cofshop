package kwh.cofshop.security.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.dto.request.LoginDto;
import kwh.cofshop.member.dto.response.LoginResponseDto;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.CustomUserDetails;
import kwh.cofshop.security.JwtTokenProvider;
import kwh.cofshop.security.domain.RefreshToken;
import kwh.cofshop.security.dto.TokenDto;
import kwh.cofshop.security.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    // 토큰 재발급
    public ResponseEntity<TokenDto> reissue(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = null; // refreshToken
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            log.error(cookie.getName());
            if (cookie.getName().equals("refreshToken")) {
                refreshToken = cookie.getValue(); // 쿠키에서 refreshToken을 추출한다.

                log.info("{} 토큰은 이렇다", refreshToken);
            }
        }

        // DB에서 토큰 찾기
        RefreshToken token = refreshTokenRepository.findByRefresh(refreshToken)
                .orElseThrow(() -> new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_NOT_EXIST));

        // 만료 여부
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String email = jwtTokenProvider.getSubject(refreshToken);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        List<Role> roles = List.of(member.getRole());

        String newAccessToken = jwtTokenProvider.createAccessToken(memberId, email, roles);

        return ResponseEntity.ok(TokenDto.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build());
    }

    // 만료된 토큰 삭제
    @Scheduled(cron = "0 0 12 * * ?") // 매일 12시에 실행
    public void cleanUpExpiredRefreshTokens() {
        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        List<RefreshToken> expiredTokens = tokens.stream()
                .filter(RefreshToken::isExpired)
                .toList();

        refreshTokenRepository.deleteAll(expiredTokens);
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND)
        );
    }
}
