package kwh.cofshop.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.DataIntegrityViolationErrorCode;
import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import kwh.cofshop.global.exception.errorcodes.InternalServerErrorCode;
import kwh.cofshop.global.exception.errorcodes.WebRequestErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, BadRequestErrorCode> CONSTRAINT_ERROR_CODE_MAP = Map.of(
            "SupportedPgProvider", BadRequestErrorCode.UNSUPPORTED_PG_PROVIDER,
            "SupportedPaymentMethod", BadRequestErrorCode.UNSUPPORTED_PAY_METHOD
    );

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
        BadRequestErrorCode validationErrorCode = resolveValidationErrorCode(e);

        String details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (details.isBlank()) {
            details = validationErrorCode.getMessage();
        }

        log.warn(
                "MethodArgumentNotValidException code={} status={} path={} traceId={} details={}",
                validationErrorCode.getCode(),
                validationErrorCode.getHttpStatus().value(),
                resolvePath(request),
                resolveTraceId(request),
                details
        );

        return ResponseEntity
                .status(validationErrorCode.getHttpStatus())
                .body(ErrorResponse.of(validationErrorCode, details));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e,
            HttpServletRequest request
    ) {
        logWithoutMessage(
                "DataIntegrityViolationException",
                DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE,
                request
        );

        return ResponseEntity
                .status(DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE.getHttpStatus())
                .body(ErrorResponse.of(DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        logWithoutMessage("HttpMessageNotReadableException", BadRequestErrorCode.INPUT_INVALID_VALUE, request);

        return ResponseEntity
                .status(BadRequestErrorCode.INPUT_INVALID_VALUE.getHttpStatus())
                .body(ErrorResponse.of(BadRequestErrorCode.INPUT_INVALID_VALUE));
    }

    // 메서드 보안(@PreAuthorize) 거부는 컨트롤러 호출 중에 발생하므로 여기서 처리
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        logKnownException("AccessDeniedException", ForbiddenErrorCode.ACCESS_DENIED, request, e);
        return buildErrorResponse(ForbiddenErrorCode.ACCESS_DENIED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        logKnownException("NoResourceFoundException", WebRequestErrorCode.RESOURCE_NOT_FOUND, request, e);
        return buildErrorResponse(WebRequestErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        logKnownException("HttpRequestMethodNotSupportedException", WebRequestErrorCode.METHOD_NOT_ALLOWED, request, e);
        return buildErrorResponse(WebRequestErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        logKnownException("HttpMediaTypeNotSupportedException", WebRequestErrorCode.UNSUPPORTED_MEDIA_TYPE, request, e);
        return buildErrorResponse(WebRequestErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    protected ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException e,
            HttpServletRequest request
    ) {
        logKnownException("HttpMediaTypeNotAcceptableException", WebRequestErrorCode.NOT_ACCEPTABLE, request, e);
        return buildErrorResponse(WebRequestErrorCode.NOT_ACCEPTABLE);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class
    })
    protected ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e, HttpServletRequest request) {
        logWithoutMessage(e.getClass().getSimpleName(), BadRequestErrorCode.INPUT_INVALID_VALUE, request);
        return buildErrorResponse(BadRequestErrorCode.INPUT_INVALID_VALUE);
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

    // 예외 메시지에 사용자 입력값(본문 값, 중복된 이메일 등)이 포함될 수 있는 경우 사용
    private void logWithoutMessage(String type, ErrorCode errorCode, HttpServletRequest request) {
        log.warn(
                "{} code={} status={} path={} traceId={}",
                type,
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                resolvePath(request),
                resolveTraceId(request)
        );
    }

    private String resolvePath(HttpServletRequest request) {
        return request == null ? "N/A" : request.getRequestURI();
    }

    private BadRequestErrorCode resolveValidationErrorCode(MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .map(this::resolveConstraintErrorCode)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(BadRequestErrorCode.INPUT_INVALID_VALUE);
    }

    private Optional<BadRequestErrorCode> resolveConstraintErrorCode(FieldError fieldError) {
        if (fieldError.getCode() != null) {
            Optional<BadRequestErrorCode> code = resolveConstraintErrorCode(fieldError.getCode());
            if (code.isPresent()) {
                return code;
            }
        }

        String[] codes = fieldError.getCodes();
        if (codes == null) {
            return Optional.empty();
        }
        return Arrays.stream(codes)
                .filter(code -> code != null)
                .map(this::resolveConstraintErrorCode)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<BadRequestErrorCode> resolveConstraintErrorCode(String code) {
        return CONSTRAINT_ERROR_CODE_MAP.entrySet().stream()
                .filter(entry -> code.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
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
