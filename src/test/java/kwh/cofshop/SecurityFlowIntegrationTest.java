package kwh.cofshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import org.awaitility.Awaitility;
import kwh.cofshop.security.token.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 HTTP와 시큐리티 필터로 로그인, 토큰, 권한 흐름을 검증
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.redis.password=",
        "spring.redis.ssl.enabled=false",
        "file.dir=build/test-uploads",
        "spring.servlet.multipart.max-file-size=1MB",
        "imp.api.secretkey=test-only",
        "coupon.rabbit.enabled=false",
        "coupon.outbox.enabled=false"
})
class SecurityFlowIntegrationTest {

    private static final String PASSWORD = "password-1234";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret-key", () -> Base64.getEncoder().encodeToString(
                "test-only-key-for-security-flow-integration-test-0123456789".getBytes(StandardCharsets.UTF_8)));
    }

    @MockitoBean
    private PortOnePaymentClient portOnePaymentClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("인증: 토큰이 없거나 위조되면 401, 유효하면 통과")
    void accessToken_validation() {
        Session session = signUpAndLogin();

        assertThat(get("/api/members/" + session.memberId, null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/members/" + session.memberId, session.accessToken + "x")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/members/" + session.memberId, session.accessToken)).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("인증: 만료된 액세스·리프레시 토큰은 거부됨")
    void expiredTokens_rejected() throws Exception {
        Session session = signUpAndLogin();

        assertThat(get("/api/members/" + session.memberId, expired(session.accessToken)))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reissue(expired(session.refreshToken)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("인증: 리프레시 토큰은 액세스 토큰으로 쓸 수 없음")
    void refreshToken_cannotBeUsedAsAccessToken() {
        Session session = signUpAndLogin();

        assertThat(get("/api/members/" + session.memberId, session.refreshToken)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("재발급: 새 토큰이 발급되고 이전 리프레시 토큰은 거부됨")
    void reissue_rotatesRefreshToken() throws Exception {
        Session session = signUpAndLogin();

        ResponseEntity<String> reissued = reissue(session.refreshToken);
        assertThat(reissued.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccessToken = bearerToken(reissued);
        assertThat(get("/api/members/" + session.memberId, newAccessToken)).isEqualTo(HttpStatus.OK);

        // 같은 초에 발급되면 토큰이 같을 수 있어 다른 시각에 발급된 토큰으로 비교
        Thread.sleep(1100);
        String newRefreshToken = refreshCookie(reissued);
        ResponseEntity<String> second = reissue(newRefreshToken);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reissue(newRefreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("재발급: 액세스 토큰으로는 재발급할 수 없음")
    void reissue_rejectsAccessToken() throws Exception {
        Session session = signUpAndLogin();

        assertThat(reissue(session.accessToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("재발급: CSRF 토큰이 없으면 거부됨")
    void reissue_requiresCsrf() {
        Session session = signUpAndLogin();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refreshToken=" + session.refreshToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/reissue", HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    @DisplayName("재발급: 비밀번호를 바꾸면 기존 리프레시 토큰이 거부됨")
    void reissue_rejectedAfterPasswordChange() throws Exception {
        Session session = signUpAndLogin();

        ResponseEntity<String> changed = send(HttpMethod.PATCH, "/api/members/me/password", session.accessToken,
                Map.of("password", "new-password-5678"));
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(reissue(session.refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("액세스 토큰: 탈퇴·정지·비밀번호 변경 후 기존 토큰이 바로 거부되고, 새로 로그인한 토큰은 동작함")
    void accessToken_rejectedAfterInvalidation() throws Exception {
        Session admin = signUpAndLoginAsAdmin();

        Session quit = signUpAndLogin();
        assertThat(send(HttpMethod.GET, "/api/members/" + quit.memberId, quit.accessToken, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        send(HttpMethod.PATCH, "/api/members/me/state", quit.accessToken, null);
        assertThat(send(HttpMethod.GET, "/api/members/" + quit.memberId, quit.accessToken, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Session suspended = signUpAndLogin();
        send(HttpMethod.PATCH, "/api/members/" + suspended.memberId + "/state?memberState=SUSPENDED", admin.accessToken, null);
        assertThat(send(HttpMethod.GET, "/api/members/" + suspended.memberId, suspended.accessToken, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Session changed = signUpAndLogin();
        send(HttpMethod.PATCH, "/api/members/me/password", changed.accessToken, Map.of("password", "new-password-5678"));
        assertThat(send(HttpMethod.GET, "/api/members/" + changed.memberId, changed.accessToken, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> relogin = send(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", changed.email, "memberPwd", "new-password-5678"));
        assertThat(relogin.getStatusCode()).as(relogin.getBody()).isEqualTo(HttpStatus.OK);
        String newToken = objectMapper.readTree(relogin.getBody()).get("accessToken").asText();
        assertThat(send(HttpMethod.GET, "/api/members/" + changed.memberId, newToken, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("탈퇴: 재발급과 로그인이 모두 거부됨")
    void quitMember_cannotReissueOrLogin() throws Exception {
        Session session = signUpAndLogin();

        assertThat(send(HttpMethod.PATCH, "/api/members/me/state", session.accessToken, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(reissue(session.refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(session.email).getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호가 틀리면 계정 존재와 상태를 알 수 없음")
    void loginFailure_doesNotRevealAccountState() {
        Session quit = signUpAndLogin();
        Session suspended = signUpAndLogin();
        Session admin = signUpAndLoginAsAdmin();
        send(HttpMethod.PATCH, "/api/members/me/state", quit.accessToken, null);
        send(HttpMethod.PATCH, "/api/members/" + suspended.memberId + "/state?memberState=SUSPENDED", admin.accessToken, null);

        for (String email : List.of("nobody@example.com", admin.email, quit.email, suspended.email)) {
            ResponseEntity<String> response = send(HttpMethod.POST, "/api/auth/login", null,
                    Map.of("email", email, "memberPwd", "wrong-password"));
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).contains("U-LOGIN");
        }

        assertThat(login(quit.email).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // 탈퇴하면 이메일을 지우므로 없는 계정과 같은 응답
        assertThat(login(quit.email).getBody()).contains("U-LOGIN");
        assertThat(login(suspended.email).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(suspended.email).getBody()).contains("U-1");
    }

    @Test
    @DisplayName("로그아웃: 이후 리프레시 토큰이 거부됨")
    void logout_invalidatesRefreshToken() throws Exception {
        Session session = signUpAndLogin();

        ResponseEntity<String> loggedOut = postWithCsrf("/api/auth/logout", session.refreshToken);
        assertThat(loggedOut.getStatusCode().is2xxSuccessful()).as(loggedOut.getBody()).isTrue();

        assertThat(reissue(session.refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("정지: 관리자가 정지하면 재발급이 거부됨")
    void suspendedMember_cannotReissue() throws Exception {
        Session member = signUpAndLogin();
        Session admin = signUpAndLoginAsAdmin();

        ResponseEntity<String> suspended = send(HttpMethod.PATCH,
                "/api/members/" + member.memberId + "/state?memberState=SUSPENDED", admin.accessToken, null);
        assertThat(suspended.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(reissue(member.refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("권한: 일반 회원은 관리자 API에서 403, 관리자는 통과")
    void adminApis_requireAdminRole() {
        Session member = signUpAndLogin();
        Session admin = signUpAndLoginAsAdmin();

        for (String path : List.of("/api/admin/payments/issues", "/api/members", "/api/members/history/" + member.memberId)) {
            assertThat(get(path, member.accessToken)).as(path).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(get(path, admin.accessToken)).as(path).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("권한: 일반 회원은 상품 등록·삭제와 포인트 변경이 거부됨")
    void memberCannotManageItemsOrPoints() {
        Session member = signUpAndLogin();

        assertThat(send(HttpMethod.DELETE, "/api/item/1", member.accessToken, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.PATCH, "/api/item/1/state", member.accessToken, Map.of("itemState", "STOP")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.PATCH, "/api/members/" + member.memberId + "/point?amount=1000", member.accessToken, null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("권한: 다른 회원의 회원 정보, 주문, 결제에 접근할 수 없음")
    void otherMembersData_notAccessible() {
        Session owner = signUpAndLogin();
        Session other = signUpAndLogin();
        Long orderId = insertOrder(owner.memberId);
        Long paymentId = insertPayment(owner.memberId, orderId);

        assertThat(get("/api/members/" + owner.memberId, other.accessToken)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/orders/" + orderId, other.accessToken)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/orders/" + orderId, owner.accessToken)).isEqualTo(HttpStatus.OK);
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + orderId + "/cancel", other.accessToken,
                Map.of("cancelReason", "다른 회원")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.POST, "/api/payments/" + paymentId + "/refund", other.accessToken,
                Map.of("amount", 1000)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.POST, "/api/payments/" + paymentId + "/verify", other.accessToken,
                Map.of("impUid", "imp", "merchantUid", "merchant", "amount", 1000)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("통계: 매출이 포함된 API는 관리자만 조회 가능")
    void statistics_salesAdminOnly() {
        Session member = signUpAndLogin();
        Session admin = signUpAndLoginAsAdmin();
        String today = java.time.LocalDate.now().toString();

        for (String path : List.of(
                "/api/statistics/daily-sales?date=" + today,
                "/api/statistics/last-7days",
                "/api/statistics/sales-between?start=" + today + "&end=" + today)) {
            assertThat(get(path, member.accessToken)).as(path).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(get(path, admin.accessToken)).as(path).isEqualTo(HttpStatus.OK);
        }
        assertThat(get("/api/statistics/sales-between?start=2000-01-01&end=" + today, admin.accessToken))
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("로그인 이력: User-Agent가 아주 길어도 기록됨")
    void loginHistory_longUserAgent() {
        Session session = signUpAndLogin();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.USER_AGENT, "a".repeat(1000));

        ResponseEntity<String> response = restTemplate.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", session.email, "memberPwd", PASSWORD), headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM member_login_history WHERE member_id = ? AND CHAR_LENGTH(device) = 255",
                        Integer.class, session.memberId)).isEqualTo(1));
    }

    @Test
    @DisplayName("파일 업로드: 관리자만 가능하고 위장 파일과 초과 크기는 거부됨")
    void tempUpload_adminOnlyAndValidated() throws Exception {
        Session member = signUpAndLogin();
        Session admin = signUpAndLoginAsAdmin();
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

        assertThat(upload(member.accessToken, png).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(upload(admin.accessToken, "<script>x</script>".getBytes(StandardCharsets.UTF_8)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        // 제한(1MB)은 넘지만 설정 파일이 없는 CI의 Tomcat 기본값(2MB)에서도 연결이 끊기지 않는 크기
        assertThat(upload(admin.accessToken, new byte[1536 * 1024]).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

        ResponseEntity<String> uploaded = upload(admin.accessToken, png);
        assertThat(uploaded.getStatusCode()).as(uploaded.getBody()).isEqualTo(HttpStatus.OK);
        String url = objectMapper.readTree(uploaded.getBody()).get(0).get("imageUrl").asText();

        ResponseEntity<byte[]> served = restTemplate.getForEntity(url, byte[].class);
        assertThat(served.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(served.getHeaders().getContentType().isCompatibleWith(MediaType.IMAGE_PNG)).isTrue();
        assertThat(served.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    @DisplayName("요청 오류: 없는 경로와 잘못된 요청은 500이 아님")
    void clientErrors_notServerErrors() {
        Session session = signUpAndLogin();

        assertThat(get("/api/does-not-exist", session.accessToken)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/orders/not-a-number", session.accessToken)).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<String> upload(String accessToken, byte[] content) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        partHeaders.setContentDisposition(ContentDisposition.formData().name("images").filename("a.png").build());
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("images", new HttpEntity<>(content, partHeaders));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange("/api/files/temp", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    // 실제 토큰과 클레임·서명 키가 같고 만료 시각만 지난 토큰
    private String expired(String token) {
        Key signingKey = ReflectionTestUtils.invokeMethod(jwtTokenProvider, "getSigningKey");
        return Jwts.builder()
                .setClaims(jwtTokenProvider.getClaims(token))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    private Long insertOrder(Long memberId) {
        jdbcTemplate.update("""
                INSERT INTO orders (member_id, merchant_uid, order_date, order_year, order_month, order_day,
                                    order_state, delivery_fee, total_price, final_price)
                VALUES (?, ?, NOW(6), 2026, 1, 1, 'PAID', 0, 1000, 1000)
                """, memberId, "merchant-" + UUID.randomUUID());
        return jdbcTemplate.queryForObject("SELECT MAX(order_id) FROM orders", Long.class);
    }

    private Long insertPayment(Long memberId, Long orderId) {
        jdbcTemplate.update("""
                INSERT INTO payment_entity (order_id, member_id, merchant_uid, price, status, buyer_email, buyer_name,
                                            buyer_tel, pg_provider, pay_method, requested_at)
                VALUES (?, ?, ?, 1000, 'PAID', 'a@example.com', 'a', '01000000000', 'kakaopay', 'card', NOW(6))
                """, orderId, memberId, "merchant-" + UUID.randomUUID());
        return jdbcTemplate.queryForObject("SELECT MAX(payment_id) FROM payment_entity", Long.class);
    }

    private Session signUpAndLoginAsAdmin() {
        Session session = signUpAndLogin();
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE member_id = ?", session.memberId);
        return loginSession(session.email);
    }

    private Session signUpAndLogin() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = unique + "@example.com";
        ResponseEntity<String> signup = send(HttpMethod.POST, "/api/members/signup", null, Map.of(
                "email", email,
                "memberName", "회원" + unique,
                "memberPwd", PASSWORD,
                "tel", String.format("010%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000_000))
        ));
        assertThat(signup.getStatusCode().is2xxSuccessful()).as(signup.getBody()).isTrue();
        return loginSession(email);
    }

    private Session loginSession(String email) {
        ResponseEntity<String> response = login(email);
        assertThat(response.getStatusCode()).as(response.getBody()).isEqualTo(HttpStatus.OK);
        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            return new Session(email, body.get("memberId").asLong(), bearerToken(response), refreshCookie(response));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ResponseEntity<String> login(String email) {
        return send(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "memberPwd", PASSWORD));
    }

    private ResponseEntity<String> reissue(String refreshToken) throws Exception {
        return postWithCsrf("/api/auth/reissue", refreshToken);
    }

    private ResponseEntity<String> postWithCsrf(String path, String refreshToken) throws Exception {
        ResponseEntity<String> csrf = restTemplate.getForEntity("/api/auth/csrf", String.class);
        String csrfToken = objectMapper.readTree(csrf.getBody()).get("token").asText();
        String csrfCookie = csrf.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .findFirst()
                .map(cookie -> cookie.split(";", 2)[0])
                .orElseThrow();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refreshToken=" + refreshToken + "; " + csrfCookie);
        headers.add("X-XSRF-TOKEN", csrfToken);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }

    private HttpStatusCode get(String path, String accessToken) {
        return send(HttpMethod.GET, path, accessToken, null).getStatusCode();
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private String bearerToken(ResponseEntity<String> response) {
        return response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION).substring("Bearer ".length());
    }

    private String refreshCookie(ResponseEntity<String> response) {
        return response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(cookie -> cookie.startsWith("refreshToken="))
                .findFirst()
                .map(cookie -> cookie.split(";", 2)[0].substring("refreshToken=".length()))
                .orElseThrow();
    }

    private record Session(String email, Long memberId, String accessToken, String refreshToken) {
    }
}
