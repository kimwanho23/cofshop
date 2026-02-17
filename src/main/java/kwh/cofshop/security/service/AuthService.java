package kwh.cofshop.security.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.security.cookie.RefreshTokenCookiePolicy;
import kwh.cofshop.security.dto.TokenResponseDto;
import kwh.cofshop.security.token.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberReadPort memberReadPort;

    public TokenResponseDto reissue(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = extractRefreshToken(request);

            if (refreshToken == null) {
                throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_NOT_EXIST);
            }

            if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
                throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
            }

            Long memberId = jwtTokenProvider.getMemberId(refreshToken);

            if (!refreshTokenService.matches(memberId, refreshToken)) {
                throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
            }

            Member member = memberReadPort.getById(memberId);
            validateMemberStateForReissue(member);

            String newAccessToken = generateAccessToken(member);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getEmail());
            refreshTokenService.save(memberId, newRefreshToken);
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    RefreshTokenCookiePolicy.issue(newRefreshToken, isSecureRequest(request)).toString()
            );

            return TokenResponseDto.builder()
                    .grantType("Bearer")
                    .accessToken(newAccessToken)
                    .refreshToken(null)
                    .build();
        } catch (UnauthorizedRequestException e) {
            expireRefreshTokenCookie(response, request);
            throw e;
        }
    }

    public String generateAccessToken(Member member) {
        return jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), List.of(member.getRole()));
    }

    private void validateMemberStateForReissue(Member member) {
        MemberState memberState = member.getMemberState();
        if (memberState == MemberState.SUSPENDED) {
            refreshTokenService.delete(member.getId());
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.MEMBER_SUSPENDED);
        }
        if (memberState == MemberState.QUIT) {
            refreshTokenService.delete(member.getId());
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.MEMBER_QUIT);
        }
    }

    private String extractRefreshToken(HttpServletRequest request) {
        return RefreshTokenCookiePolicy.extract(request);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && "https".equalsIgnoreCase(forwardedProto);
    }

    private void expireRefreshTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                RefreshTokenCookiePolicy.expire(isSecureRequest(request)).toString()
        );
    }

}

