package kwh.cofshop.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.DataIntegrityViolationErrorCode;
import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import kwh.cofshop.global.exception.errorcodes.InternalServerErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logKnownException("BusinessException", e.getErrorCode(), request, e);
        return buildErrorResponse(e.getErrorCode());
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e, HttpServletRequest request) {
        logKnownException("BadRequestException", e.getErrorCode(), request, e);
        return buildErrorResponse(e.getErrorCode());
    }

    @ExceptionHandler(UnauthorizedRequestException.class)
    protected ResponseEntity<ErrorResponse> handleUnauthorizedRequestException(
            UnauthorizedRequestException e,
            HttpServletRequest request
    ) {
        logKnownException("UnauthorizedRequestException", e.getErrorCode(), request, e);
        return buildErrorResponse(e.getErrorCode());
    }

    @ExceptionHandler(ForbiddenRequestException.class)
    protected ResponseEntity<ErrorResponse> handleForbiddenRequestException(
            ForbiddenRequestException e,
            HttpServletRequest request
    ) {
        logKnownException("ForbiddenRequestException", e.getErrorCode(), request, e);
        return buildErrorResponse(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (details.isBlank()) {
            details = BadRequestErrorCode.INPUT_INVALID_VALUE.getMessage();
        }

        log.warn(
                "MethodArgumentNotValidException code={} status={} path={} traceId={} details={}",
                BadRequestErrorCode.INPUT_INVALID_VALUE.getCode(),
                BadRequestErrorCode.INPUT_INVALID_VALUE.getHttpStatus().value(),
                resolvePath(request),
                resolveTraceId(request),
                details
        );

        return ResponseEntity
                .status(BadRequestErrorCode.INPUT_INVALID_VALUE.getHttpStatus())
                .body(ErrorResponse.of(BadRequestErrorCode.INPUT_INVALID_VALUE, details));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e,
            HttpServletRequest request
    ) {
        logKnownException(
                "DataIntegrityViolationException",
                DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE,
                request,
                e
        );

        return ResponseEntity
                .status(DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE.getHttpStatus())
                .body(ErrorResponse.of(DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error(
                "UnhandledException path={} traceId={}",
                resolvePath(request),
                resolveTraceId(request),
                e
        );
        return ResponseEntity
                .status(InternalServerErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(InternalServerErrorCode.INTERNAL_SERVER_ERROR));
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    private void logKnownException(String type, ErrorCode errorCode, HttpServletRequest request, Exception e) {
        log.warn(
                "{} code={} status={} path={} traceId={} message={}",
                type,
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                resolvePath(request),
                resolveTraceId(request),
                e.getMessage()
        );
    }

    private String resolvePath(HttpServletRequest request) {
        return request == null ? "N/A" : request.getRequestURI();
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }

        if (request != null) {
            String b3TraceId = request.getHeader("X-B3-TraceId");
            if (b3TraceId != null && !b3TraceId.isBlank()) {
                return b3TraceId;
            }

            String requestId = request.getHeader("X-Request-Id");
            if (requestId != null && !requestId.isBlank()) {
                return requestId;
            }
        }

        return "N/A";
    }
}
