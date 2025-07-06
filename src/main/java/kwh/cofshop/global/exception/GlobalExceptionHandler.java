package kwh.cofshop.global.exception;

import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.DataIntegrityViolationErrorCode;
import kwh.cofshop.global.exception.errorcodes.InternalServerErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 에러
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleGlobalException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    // 잘못된 요청 예외
    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e) {
        log.error("BadRequestException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())  // 400 BAD_REQUEST
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    // 잘못된 요청 예외
    @ExceptionHandler(UnauthorizedRequestException.class)
    protected ResponseEntity<ErrorResponse> handleBadRequestException(UnauthorizedRequestException e) {
        log.error("UnauthorizedRequestException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())  // 401 UNAUTHORIZED_REQUEST
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    // 유효성 에러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());
        return ResponseEntity
                .status(BadRequestErrorCode.INPUT_INVALID_VALUE.getHttpStatus())
                .body(ErrorResponse.of(BadRequestErrorCode.INPUT_INVALID_VALUE));
    }

    // 데이터 중복
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity
                .status(DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE.getHttpStatus())
                .body(ErrorResponse.of(DataIntegrityViolationErrorCode.DATA_INTEGRITY_VIOLATION_ERROR_CODE));
    }


    // 기본 예외
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException: {}", e.getMessage());
        return ResponseEntity
                .status(InternalServerErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(InternalServerErrorCode.INTERNAL_SERVER_ERROR));
    }
}
