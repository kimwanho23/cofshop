package kwh.cofshop.global.exception;

import kwh.cofshop.support.StandaloneMockMvcFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 클라이언트 잘못이 500으로 처리되면 5xx 알림이 오동작하므로 상태 코드를 고정
class GlobalExceptionHandlerWebTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcFactory.build(new TestController());
    }

    @Test
    @DisplayName("없는 정적 리소스 경로는 404")
    void noResource_404() throws Exception {
        mockMvc.perform(get("/test/no-resource")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405")
    void methodNotAllowed_405() throws Exception {
        mockMvc.perform(delete("/test/ok")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("지원하지 않는 요청 형식은 415")
    void unsupportedMediaType_415() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("필수 파라미터 누락은 400")
    void missingParameter_400() throws Exception {
        mockMvc.perform(get("/test/number")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파라미터 타입 오류는 400")
    void typeMismatch_400() throws Exception {
        mockMvc.perform(get("/test/number").param("value", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("메서드 보안 거부는 403")
    void accessDenied_403() throws Exception {
        mockMvc.perform(get("/test/denied")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("예상하지 못한 오류는 500 유지")
    void unexpected_500() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").exists());
    }

    @RestController
    static class TestController {

        @GetMapping("/test/ok")
        String ok() {
            return "ok";
        }

        @PostMapping(value = "/test/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        String body(@RequestBody String body) {
            return body;
        }

        @GetMapping("/test/number")
        String number(@RequestParam int value) {
            return String.valueOf(value);
        }

        @GetMapping("/test/no-resource")
        String noResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "test/no-resource");
        }

        @GetMapping("/test/denied")
        String denied() {
            throw new AuthorizationDeniedException("denied", new AuthorizationDecision(false));
        }

        @GetMapping("/test/error")
        String error() {
            throw new IllegalStateException("boom");
        }
    }
}
