package kwh.cofshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 같은 MySQL·Redis·스토리지를 쓰는 앱 2개를 띄워 서버 간 동작을 검증
@Testcontainers(disabledWithoutDocker = true)
class MultiInstanceIntegrationTest {

    private static final String PASSWORD = "password-1234";
    private static final String BUCKET = "cofshop-images";
    private static final String S3_ACCESS_KEY = "test-access";
    private static final String S3_SECRET_KEY = "test-secret-value";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    // S3 API만 흉내 내므로 인증과 버킷 공개 정책은 검증하지 않음
    @Container
    static GenericContainer<?> s3 = new GenericContainer<>("adobe/s3mock:4.11.0")
            .withExposedPorts(9090)
            .waitingFor(Wait.forHttp("/").forPort(9090));

    private static ConfigurableApplicationContext first;
    private static ConfigurableApplicationContext second;
    private static String firstUrl;
    private static String secondUrl;
    private static JdbcTemplate jdbcTemplate;

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startInstances() {
        createBucket();
        first = startInstance();
        second = startInstance();
        firstUrl = "http://localhost:" + first.getEnvironment().getProperty("local.server.port");
        secondUrl = "http://localhost:" + second.getEnvironment().getProperty("local.server.port");
        jdbcTemplate = first.getBean(JdbcTemplate.class);
    }

    @AfterAll
    static void stopInstances() {
        if (second != null) {
            second.close();
        }
        if (first != null) {
            first.close();
        }
    }

    @Test
    @DisplayName("채팅: 다른 서버에 연결된 구독자도 메시지를 한 번만 받음")
    void chat_deliveredAcrossInstances() throws Exception {
        String admin = loginAsAdmin();
        String member = signUpAndLogin();
        ResponseEntity<String> room = send(HttpMethod.POST, firstUrl + "/api/chat-rooms", member, null);
        assertThat(room.getStatusCode()).as(room.getBody()).isEqualTo(HttpStatus.CREATED);
        long roomId = objectMapper.readTree(room.getBody()).get("roomId").asLong();
        Long agentId = jdbcTemplate.queryForObject("SELECT MAX(member_id) FROM member WHERE role = 'ADMIN'", Long.class);
        assertThat(send(HttpMethod.PATCH, firstUrl + "/api/chat-rooms/" + roomId + "/join?agentId=" + agentId, admin, null)
                .getStatusCode().is2xxSuccessful()).isTrue();

        BlockingQueue<JsonNode> onFirst = new LinkedBlockingQueue<>();
        BlockingQueue<JsonNode> onSecond = new LinkedBlockingQueue<>();
        StompSession memberOnSecond = connect(secondUrl, member);
        StompSession agentOnFirst = connect(firstUrl, admin);
        subscribe(memberOnSecond, roomId, onSecond);
        subscribe(agentOnFirst, roomId, onFirst);
        Thread.sleep(500); // SUBSCRIBE는 응답이 없어 등록될 시간을 둠

        agentOnFirst.send("/app/chat.sendMessage", Map.of(
                "roomId", roomId, "message", "안녕하세요", "messageType", "TEXT"));

        JsonNode received = onSecond.poll(10, TimeUnit.SECONDS);
        assertThat(received).as("다른 서버의 구독자").isNotNull();
        assertThat(received.get("message").asText()).isEqualTo("안녕하세요");
        assertThat(onFirst.poll(10, TimeUnit.SECONDS)).as("같은 서버의 구독자").isNotNull();

        assertThat(onSecond.poll(1, TimeUnit.SECONDS)).as("중복 수신 없음").isNull();
        assertThat(onFirst.poll(1, TimeUnit.SECONDS)).as("중복 수신 없음").isNull();
        memberOnSecond.disconnect();
        agentOnFirst.disconnect();
    }

    @Test
    @DisplayName("파일: 한 서버에 올린 이미지를 공유 스토리지에서 받고, 다른 서버에서 지울 수 있음")
    void files_sharedAcrossInstances() throws Exception {
        String admin = loginAsAdmin();
        ResponseEntity<String> uploaded = upload(firstUrl, admin);
        assertThat(uploaded.getStatusCode()).as(uploaded.getBody()).isEqualTo(HttpStatus.OK);
        JsonNode file = objectMapper.readTree(uploaded.getBody()).get(0);
        URI imageUrl = URI.create(file.get("imageUrl").asText());
        assertThat(imageUrl.toString()).startsWith(s3Url());

        ResponseEntity<byte[]> served = restTemplate.getForEntity(imageUrl, byte[].class);
        assertThat(served.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(served.getBody()).isEqualTo(PNG);
        assertThat(served.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);

        ResponseEntity<String> deleted = send(HttpMethod.DELETE,
                secondUrl + "/api/files/temp/" + file.get("tempFileId").asLong(), admin, null);
        assertThat(deleted.getStatusCode()).as(deleted.getBody()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(restTemplate.getForEntity(imageUrl, byte[].class).getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    @DisplayName("인증: 한 서버에서 받은 토큰을 다른 서버에서 쓰고, 재발급도 공유됨")
    void tokens_sharedAcrossInstances() throws Exception {
        String email = signUp();
        ResponseEntity<String> login = login(firstUrl, email);
        String accessToken = accessToken(login);
        long memberId = objectMapper.readTree(login.getBody()).get("memberId").asLong();
        String refreshToken = refreshCookie(login);

        assertThat(send(HttpMethod.GET, secondUrl + "/api/members/" + memberId, accessToken, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        Thread.sleep(1100); // 같은 초에 발급되면 토큰이 같을 수 있음
        ResponseEntity<String> reissued = reissue(secondUrl, refreshToken);
        assertThat(reissued.getStatusCode()).as(reissued.getBody()).isEqualTo(HttpStatus.OK);
        assertThat(reissue(firstUrl, refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("재고: 두 서버로 나뉜 동시 주문도 재고만큼만 성공")
    void orders_doNotOversellAcrossInstances() throws Exception {
        String admin = loginAsAdmin();
        long itemId = createItem(admin, 5);
        long optionId = send(HttpMethod.GET, firstUrl + "/api/item/" + itemId, admin, null).getBody() == null ? 0
                : objectMapper.readTree(send(HttpMethod.GET, firstUrl + "/api/item/" + itemId, admin, null).getBody())
                .get("itemOptions").get(0).get("id").asLong();

        List<Callable<HttpStatus>> orders = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String baseUrl = i % 2 == 0 ? firstUrl : secondUrl;
            String buyer = signUpAndLogin();
            orders.add(() -> HttpStatus.valueOf(send(HttpMethod.POST, baseUrl + "/api/orders", buyer, Map.of(
                    "address", Map.of("city", "서울", "street", "테헤란로", "zipCode", "06236"),
                    "orderItems", List.of(Map.of("itemId", itemId, "optionId", optionId, "quantity", 1))
            )).getStatusCode().value()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<HttpStatus> statuses = new ArrayList<>();
        try {
            for (Future<HttpStatus> future : executor.invokeAll(orders)) {
                statuses.add(future.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(statuses).filteredOn(status -> status == HttpStatus.CREATED).hasSize(5);
        assertThat(statuses).noneMatch(HttpStatus::is5xxServerError);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT stock FROM item_option WHERE item_option_id = ?", Integer.class, optionId)).isZero();
    }

    private long createItem(String admin, int stock) throws Exception {
        ResponseEntity<String> category = send(HttpMethod.POST, firstUrl + "/api/categories", admin,
                Map.of("name", "카테고리" + unique()));
        assertThat(category.getStatusCode().is2xxSuccessful()).as(category.getBody()).isTrue();
        long categoryId = objectMapper.readTree(category.getBody()).get("id").asLong();

        ResponseEntity<String> uploaded = upload(firstUrl, admin);
        long tempFileId = objectMapper.readTree(uploaded.getBody()).get(0).get("tempFileId").asLong();

        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_JSON);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("itemRequest", new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                "itemName", "상품" + unique(),
                "price", 1000,
                "deliveryFee", 0,
                "origin", "브라질",
                "itemLimit", 10,
                "categoryIds", List.of(categoryId),
                "itemImages", List.of(Map.of("tempFileId", tempFileId, "imgType", "REPRESENTATIVE")),
                "itemOptions", List.of(Map.of("description", "기본", "additionalPrice", 0, "stock", stock))
        )), partHeaders));
        ResponseEntity<String> created = restTemplate.exchange(firstUrl + "/api/item", HttpMethod.POST,
                new HttpEntity<>(body, multipartHeaders(admin)), String.class);
        assertThat(created.getStatusCode()).as(created.getBody()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(created.getBody()).get("id").asLong();
    }

    private StompSession connect(String baseUrl, String accessToken) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);
        return client.connectAsync(
                URI.create(baseUrl.replace("http://", "ws://") + "/ws-chat/websocket"),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(10, TimeUnit.SECONDS);
    }

    private void subscribe(StompSession session, long roomId, BlockingQueue<JsonNode> received) {
        session.subscribe("/topic/chatroom." + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((JsonNode) payload);
            }
        });
    }

    private ResponseEntity<String> upload(String baseUrl, String accessToken) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        partHeaders.setContentDisposition(ContentDisposition.formData().name("images").filename("a.png").build());
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("images", new HttpEntity<>(PNG, partHeaders));
        return restTemplate.exchange(baseUrl + "/api/files/temp", HttpMethod.POST,
                new HttpEntity<>(body, multipartHeaders(accessToken)), String.class);
    }

    private HttpHeaders multipartHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private ResponseEntity<String> reissue(String baseUrl, String refreshToken) throws Exception {
        ResponseEntity<String> csrf = restTemplate.getForEntity(baseUrl + "/api/auth/csrf", String.class);
        String csrfToken = objectMapper.readTree(csrf.getBody()).get("token").asText();
        String csrfCookie = csrf.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .findFirst()
                .map(cookie -> cookie.split(";", 2)[0])
                .orElseThrow();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refreshToken=" + refreshToken + "; " + csrfCookie);
        headers.add("X-XSRF-TOKEN", csrfToken);
        return restTemplate.exchange(baseUrl + "/api/auth/reissue", HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }

    private String loginAsAdmin() {
        String email = signUp();
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE email = ?", email);
        return accessToken(login(firstUrl, email));
    }

    private String signUpAndLogin() {
        return accessToken(login(firstUrl, signUp()));
    }

    private String signUp() {
        String unique = unique();
        String email = unique + "@example.com";
        ResponseEntity<String> response = send(HttpMethod.POST, firstUrl + "/api/members/signup", null, Map.of(
                "email", email, "memberName", "회원" + unique, "memberPwd", PASSWORD, "tel", "01012345678"));
        assertThat(response.getStatusCode().is2xxSuccessful()).as(response.getBody()).isTrue();
        return email;
    }

    private ResponseEntity<String> login(String baseUrl, String email) {
        ResponseEntity<String> response = send(HttpMethod.POST, baseUrl + "/api/auth/login", null,
                Map.of("email", email, "memberPwd", PASSWORD));
        assertThat(response.getStatusCode()).as(response.getBody()).isEqualTo(HttpStatus.OK);
        return response;
    }

    private String accessToken(ResponseEntity<String> login) {
        return login.getHeaders().getFirst(HttpHeaders.AUTHORIZATION).substring("Bearer ".length());
    }

    private String refreshCookie(ResponseEntity<String> login) {
        return login.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(cookie -> cookie.startsWith("refreshToken="))
                .findFirst()
                .map(cookie -> cookie.split(";", 2)[0].substring("refreshToken=".length()))
                .orElseThrow();
    }

    private ResponseEntity<String> send(HttpMethod method, String url, String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static ConfigurableApplicationContext startInstance() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.port", "0");
        properties.put("management.server.port", "0");
        properties.put("spring.datasource.url", mysql.getJdbcUrl());
        properties.put("spring.datasource.driver-class-name", mysql.getDriverClassName());
        properties.put("spring.datasource.username", mysql.getUsername());
        properties.put("spring.datasource.password", mysql.getPassword());
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
        properties.put("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("spring.redis.host", redis.getHost());
        properties.put("spring.redis.port", redis.getMappedPort(6379));
        properties.put("spring.redis.password", "");
        properties.put("spring.redis.ssl.enabled", "false");
        properties.put("jwt.secret-key", Base64.getEncoder().encodeToString(
                "test-only-key-for-multi-instance-integration-test-0123456789".getBytes(StandardCharsets.UTF_8)));
        properties.put("imp.api.secretkey", "test-only");
        properties.put("coupon.rabbit.enabled", "false");
        properties.put("file.storage", "s3");
        properties.put("file.s3.bucket", BUCKET);
        properties.put("file.s3.region", "us-east-1");
        properties.put("file.s3.endpoint", s3Url());
        properties.put("file.s3.access-key", S3_ACCESS_KEY);
        properties.put("file.s3.secret-key", S3_SECRET_KEY);
        properties.put("file.s3.path-style", "true");
        properties.put("file.s3.public-url", s3Url() + "/" + BUCKET);
        // 테스트 리소스 설정보다 우선하도록 명령행 인자로 전달
        String[] args = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(CofshopApplication.class).run(args);
    }

    private static void createBucket() {
        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3Url()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(S3_ACCESS_KEY, S3_SECRET_KEY)))
                .httpClient(software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.create())
                .forcePathStyle(true)
                .build()) {
            s3Client.createBucket(builder -> builder.bucket(BUCKET));
        }
    }

    private static String s3Url() {
        return "http://" + s3.getHost() + ":" + s3.getMappedPort(9090);
    }
}
