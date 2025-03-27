package kwh.cofshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.config.CorsConfig;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;


@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final CorsConfig corsConfig;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/member/signup", "/",  "/login", "/api/item/search",
                                "/api/review/**",  "/api/auth/login", "/api/auth/reissue", "/api/auth/logout",
                                "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**", "/api*").permitAll()
                        .requestMatchers("/api/**", "/item/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())  // 인증 실패 처리
                        .accessDeniedHandler(new CustomAccessDeniedHandler())  // 권한 부족 처리
                )
                .logout((logout) -> logout
                        .logoutSuccessUrl("/logout")
                        .invalidateHttpSession(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 무상태
                .addFilterBefore(new CustomLogoutFilter(refreshTokenRepository, jwtTokenProvider), LogoutFilter.class)
                .addFilterBefore(new JwtFilter(jwtTokenProvider), CustomLoginFilter.class)
                .addFilterAt(new CustomLoginFilter(authenticationManager(), jwtTokenProvider, objectMapper, refreshTokenRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(customAuthenticationProvider());
    }

    @Bean
    public CustomAuthenticationProvider customAuthenticationProvider() {
        return new CustomAuthenticationProvider(passwordEncoder(), customUserDetailsService());
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호를 암호화된 문자열로 변환
    }

    @Bean
    public CustomUserDetailsService customUserDetailsService() {
        return new CustomUserDetailsService(memberRepository); // CustomUserDetailsService 빈 등록
    }



}
