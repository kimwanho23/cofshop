package kwh.cofshop.security.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.JwtTokenProvider;
import kwh.cofshop.security.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    // 토큰 재발급
    public ResponseEntity<TokenDto> reissue(HttpServletRequest request) {
        String refreshToken = extractRefreshToken(request);

        if (refreshToken == null) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_NOT_EXIST);
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);

        if (!refreshTokenService.matches(memberId, refreshToken)) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = generateAccessToken(member); // 새로운 AccessToken 발급
        refreshTokenService.save(memberId, refreshToken);

        return ResponseEntity.ok(TokenDto.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build());
    }

    public String generateAccessToken(Member member) {
        return jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), List.of(member.getRole()));
    }

    // 쿠키에서 refreshToken 추출
    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
