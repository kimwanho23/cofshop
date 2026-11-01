package kwh.cofshop.security.token;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import kwh.cofshop.security.properties.JwtProperties;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.security.dto.TokenResponseDto;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtTokenProvider {

    private final Key key;
    private static final String AUTHORITIES_KEY = "auth";
    private static final String TOKEN_TYPE_KEY = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";
    private static final String BEARER_TYPE = "Bearer";
    private static final String ISSUED_AT_MILLIS_KEY = "issuedAtMillis";
    public static final long ACCESS_TOKEN_TTL_SECONDS = Duration.ofHours(1).toSeconds();
    private static final long AUTH_TOKEN_EXPIRATION = Duration.ofSeconds(ACCESS_TOKEN_TTL_SECONDS).toMillis();
    private static final long REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7).toMillis();

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    private Key getSigningKey() {
        return this.key;
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = getClaims(accessToken);

        Collection<? extends GrantedAuthority> authorities = parseAuthorities(claims.get(AUTHORITIES_KEY));
        Long memberId = claims.get("memberId", Long.class);

        CustomUserDetails principal = CustomUserDetails.builder()
                .id(memberId)
                .email(claims.getSubject())
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public Authentication getAuthenticationByRefreshToken(String refreshToken) {
        Claims claims = getClaims(refreshToken);
        Long memberId = claims.get("memberId", Long.class);

        CustomUserDetails principal = CustomUserDetails.builder()
                .id(memberId)
                .email(claims.getSubject())
                .authorities(List.of())
                .build();

        return new UsernamePasswordAuthenticationToken(principal, "", List.of());
    }

    public Long getTokenExpirationTime(String token) {
        return getClaims(token).getExpiration().getTime();
    }

    public Claims getClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public TokenResponseDto createAuthToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim("memberId", userDetails.getId())
                .claim(TOKEN_TYPE_KEY, TOKEN_TYPE_ACCESS)
                .claim(ISSUED_AT_MILLIS_KEY, System.currentTimeMillis())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + AUTH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("memberId", userDetails.getId())
                .claim(TOKEN_TYPE_KEY, TOKEN_TYPE_REFRESH)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        return TokenResponseDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public String createAccessToken(Long memberId, String email, List<Role> roles) {
        List<String> authorities = roles.stream()
                .map(role -> "ROLE_" + role.name())
                .toList();

        return Jwts.builder()
                .setSubject(email)
                .claim(AUTHORITIES_KEY, authorities)
                .claim("memberId", memberId)
                .claim(TOKEN_TYPE_KEY, TOKEN_TYPE_ACCESS)
                .claim(ISSUED_AT_MILLIS_KEY, System.currentTimeMillis())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + AUTH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String createRefreshToken(Long memberId, String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("memberId", memberId)
                .claim(TOKEN_TYPE_KEY, TOKEN_TYPE_REFRESH)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // iat는 초 단위라 같은 초에 일어난 탈퇴와 재로그인을 구분하지 못한다
    public long getIssuedAtMillis(String token) {
        Claims claims = getClaims(token);
        Long issuedAtMillis = claims.get(ISSUED_AT_MILLIS_KEY, Long.class);
        if (issuedAtMillis != null) {
            return issuedAtMillis;
        }
        return claims.getIssuedAt() == null ? 0L : claims.getIssuedAt().getTime();
    }

    public Long getMemberId(String token) {
        return getClaims(token).get("memberId", Long.class);
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 만료는 정상 흐름이고, 잘못된 토큰은 외부 입력이므로 오류 등급과 메시지를 남기지 않음
            log.debug("Token expired");
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getClass().getSimpleName());
        }
        return false;
    }

    public TokenStatus getTokenStatus(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (JwtException e) {
            return TokenStatus.INVALID;
        }
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    private String getTokenType(String token) {
        Object claim = getClaims(token).get(TOKEN_TYPE_KEY);
        return claim == null ? "" : claim.toString();
    }

    private Collection<? extends GrantedAuthority> parseAuthorities(Object authorityClaim) {
        if (authorityClaim == null) {
            return List.of();
        }
        if (authorityClaim instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }
        String raw = authorityClaim.toString().trim();
        if (raw.startsWith("[") && raw.endsWith("]")) {
            raw = raw.substring(1, raw.length() - 1);
        }
        if (raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public enum TokenStatus {
        VALID,
        EXPIRED,
        INVALID
    }
}

