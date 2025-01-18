
package kwh.cofshop.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import kwh.cofshop.global.TokenDto;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.time.Duration;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtTokenProvider {

    private final Key key;
    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "bearer";
    private static final long AUTH_TOKEN_EXPIRATION = Duration.ofHours(1).toMillis();  // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7).toMillis(); // 7일

    public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private Key getSigningKey() {
        return this.key;
    }

    // JWT 토큰 복호화, 토큰 정보 추출
    public Authentication getAuthentication(String accessToken) {

        Claims claims = getClaims(accessToken);

        // 권한 정보 추출
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        CustomUserDetails principal = CustomUserDetails.builder()
                .email(claims.getSubject())
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public Claims getClaims(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
    }

    // Access Token 생성
    public TokenDto createAuthToken(Authentication authentication) {

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())            // 사용자 식별자
                .claim(AUTHORITIES_KEY, authorities)    // 권한 정보
                .setIssuedAt(new Date(System.currentTimeMillis()))  // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + AUTH_TOKEN_EXPIRATION))  // 만료 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)  // 서명 알고리즘과 비밀키
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())  // 사용자 식별자만 포함
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        return TokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("토큰 만료됨: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("잘못된 토큰: {}", e.getMessage());
        }
        return false;
    }

}

