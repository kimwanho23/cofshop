package kwh.cofshop;

import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

// 관리 포트 분리와 메트릭 노출을 실제 서버로 검증. 설정 값은 application.properties.template과 동일하게 유지
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureObservability(tracing = false)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.redis.password=",
        "spring.redis.ssl.enabled=false",
        "file.dir=build/test-uploads",
        "imp.api.secretkey=test-only",
        "coupon.stream.enabled=false",
        "coupon.rabbit.enabled=false",
        "coupon.outbox.enabled=false",
        "management.server.port=0",
        "management.endpoints.web.exposure.include=health,prometheus",
        "management.health.rabbit.enabled=false"
})
class ActuatorIntegrationTest {

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
                "test-only-key-for-actuator-integration-test-0123456789".getBytes(StandardCharsets.UTF_8)));
    }

    @MockitoBean
    private PortOnePaymentClient portOnePaymentClient;

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("관리 포트: 인증 없이 헬스 체크가 UP")
    void health_isUpOnManagementPort() {
        ResponseEntity<String> response = restTemplate.getForEntity(url(managementPort, "/actuator/health"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("관리 포트: 업무 메트릭과 기본 메트릭을 노출")
    void prometheus_exposesMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity(url(managementPort, "/actuator/prometheus"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(
                "refund_recovery_queue_size",
                "refund_recovery_dlq_size",
                "refund_approved_stuck",
                "coupon_outbox_events",
                "hikaricp_connections_pending",
                "jvm_memory_used_bytes"
        );
    }

    @Test
    @DisplayName("서비스 포트: 운영 엔드포인트를 제공하지 않음")
    void servicePort_doesNotServeActuator() {
        assertThat(restTemplate.getForEntity(url(serverPort, "/actuator/prometheus"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity(url(serverPort, "/actuator/health"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("요청 추적 ID: 시큐리티에서 거부된 응답에도 헤더가 남음")
    void requestId_presentOnRejectedResponse() {
        ResponseEntity<String> response = restTemplate.getForEntity(url(serverPort, "/api/orders/1"), String.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    @Test
    @DisplayName("관리 포트: 노출하지 않은 엔드포인트는 접근 불가")
    void managementPort_doesNotServeOtherEndpoints() {
        assertThat(restTemplate.getForEntity(url(managementPort, "/actuator/env"), String.class).getStatusCode().is2xxSuccessful())
                .isFalse();
    }

    private String url(int port, String path) {
        return "http://localhost:" + port + path;
    }
}
