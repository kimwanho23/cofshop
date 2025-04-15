package kwh.cofshop.global.response;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ApiResponse<T>(ApiHeader header, ApiBody<T> body) {

    // 200 OK 메서드
    public static <T> ApiResponse<T> OK(T data) {
        return new ApiResponse<>(
                new ApiHeader(HttpStatus.OK, "S-OK"),
                new ApiBody<>("API LOAD SUCCESSFUL", LocalDateTime.now(), data)
        );
    }

    // 201 Created 메서드
    public static <T> ApiResponse<T> Created(T data) {
        return new ApiResponse<>(
                new ApiHeader(HttpStatus.CREATED, "C-OK"),
                new ApiBody<>("CREATED SUCCESSFUL", LocalDateTime.now(), data)
        );
    }
}