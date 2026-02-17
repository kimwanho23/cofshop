package kwh.cofshop.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import kwh.cofshop.security.filter.CustomLoginFilter;
import kwh.cofshop.security.filter.CustomLogoutFilter;
import kwh.cofshop.security.filter.JwtFilter;
import kwh.cofshop.security.handler.CustomAccessDeniedHandler;
import kwh.cofshop.security.handler.CustomAuthenticationEntryPoint;
import kwh.cofshop.security.provider.CustomAuthenticationProvider;
import kwh.cofshop.security.service.RefreshTokenService;
import kwh.cofshop.security.token.JwtTokenProvider;
import kwh.cofshop.security.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
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
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RefreshTokenService refreshTokenService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomUserDetailsService customUserDetailsService;
    private final Validator validator;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(request -> {
                            String path = request.getRequestURI();
                            return "POST".equals(request.getMethod())
                                    && ("/api/auth/reissue".equals(path) || "/api/auth/logout".equals(path));
                        }))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/images/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/ws-chat/**").permitAll()
                        .requestMatchers("/api/members/signup", "/", "/api/item/search", "/payments/sample",
                                "/api/reviews/**", "/api/auth/login", "/api/auth/reissue", "/api/auth/logout", "/api/auth/csrf",
                                "/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/**", "/item/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .logout(logout -> logout
                        .logoutSuccessUrl("/api/auth/logout")
                        .invalidateHttpSession(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                applicationEventPublisher,
                validator
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
        return new CustomAuthenticationProvider(passwordEncoder(), customUserDetailsService);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
