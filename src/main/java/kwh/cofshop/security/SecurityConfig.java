package kwh.cofshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;


@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RefreshTokenService refreshTokenService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(request -> {
                            String path = request.getRequestURI();
                            return "POST".equals(request.getMethod())
                                    && ("/api/auth/reissue".equals(path) || "/api/auth/logout".equals(path));
                        }))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/members/signup", "/", "/api/item/search", "/payments/sample",
                                "/api/reviews/**", "/api/auth/login", "/api/auth/reissue", "/api/auth/logout", "/api/auth/csrf",
                                "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/**", "/item/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)  // 인증 실패 처리
                        .accessDeniedHandler(customAccessDeniedHandler)  // 권한 부족 처리
                )
                .logout((logout) -> logout
                        .logoutSuccessUrl("/api/auth/logout")
                        .invalidateHttpSession(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 무상태
                .addFilterBefore(new CustomLogoutFilter(refreshTokenService, jwtTokenProvider), LogoutFilter.class)
                .addFilterBefore(new JwtFilter(jwtTokenProvider), CustomLoginFilter.class)
                .addFilterAt(customLoginFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CustomLoginFilter customLoginFilter() {
        CustomLoginFilter loginFilter = new CustomLoginFilter(
                authenticationManager(),
                jwtTokenProvider,
                objectMapper,
                refreshTokenService,
                applicationEventPublisher
        );
        // Required by AbstractAuthenticationProcessingFilter lifecycle.
        loginFilter.setAuthenticationManager(authenticationManager());
        loginFilter.setFilterProcessesUrl("/api/auth/login");
        return loginFilter;
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
