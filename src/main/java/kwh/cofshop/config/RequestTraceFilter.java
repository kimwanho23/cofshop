package kwh.cofshop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

// 요청마다 추적 ID를 로그(MDC)와 응답 헤더에 남김. 시큐리티 필터의 로그도 포함되도록 가장 먼저 실행
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_KEY = "traceId";

    // 외부 입력이므로 로그 위조(줄바꿈 삽입 등)를 막기 위해 형식을 제한
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(REQUEST_ID_HEADER));
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(REQUEST_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String resolveTraceId(String requestId) {
        if (requestId != null && VALID_REQUEST_ID.matcher(requestId).matches()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
