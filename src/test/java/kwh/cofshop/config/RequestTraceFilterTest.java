package kwh.cofshop.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("추적 ID: 없으면 생성해 요청 중 MDC와 응답 헤더에 남기고 끝나면 제거")
    void generatesTraceId() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInRequest = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), response,
                (request, servletResponse) -> traceIdInRequest.set(MDC.get(RequestTraceFilter.TRACE_ID_KEY)));

        assertThat(traceIdInRequest.get()).isNotBlank();
        assertThat(response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER)).isEqualTo(traceIdInRequest.get());
        assertThat(MDC.get(RequestTraceFilter.TRACE_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("추적 ID: 형식이 맞는 요청 헤더는 그대로 사용")
    void usesValidRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "abc-123_x.y");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER)).isEqualTo("abc-123_x.y");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad\r\nFAKE LOG", "with space", "{\"json\":1}"})
    @DisplayName("추적 ID: 로그 위조가 가능한 요청 헤더는 버리고 새로 생성")
    void rejectsInvalidRequestId(String requestId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, requestId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER)).isNotEqualTo(requestId);
    }

    @Test
    @DisplayName("비동기 전달: 작업 스레드에 MDC를 복사하고 끝나면 원래 값으로 복원")
    void mdcTaskDecorator_propagatesContext() throws Exception {
        MDC.put(RequestTraceFilter.TRACE_ID_KEY, "trace-1");
        AtomicReference<String> traceIdInTask = new AtomicReference<>();
        AtomicReference<String> traceIdAfterTask = new AtomicReference<>();
        Runnable decorated = new MdcTaskDecorator().decorate(() ->
                traceIdInTask.set(MDC.get(RequestTraceFilter.TRACE_ID_KEY)));

        Thread worker = new Thread(() -> {
            decorated.run();
            traceIdAfterTask.set(MDC.get(RequestTraceFilter.TRACE_ID_KEY));
        });
        worker.start();
        worker.join();

        assertThat(traceIdInTask.get()).isEqualTo("trace-1");
        assertThat(traceIdAfterTask.get()).isNull();
    }
}
