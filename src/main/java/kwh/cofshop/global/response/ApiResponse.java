package kwh.cofshop.global.response;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(ApiHeader header, ApiBody<T> body) {

    // 200 OK 메서드
    public static <T> ApiResponse<T> OK(T data) {
        return new ApiResponse<>(
                new ApiHeader(HttpStatus.OK, "S-1"),
                new ApiBody<>(data, "API LOAD SUCCESSFUL")
        );
    }

    // 201 Created 메서드
    public static <T> ApiResponse<T> Created(T data) {
        return new ApiResponse<>(
                new ApiHeader(HttpStatus.CREATED, "C-1"),
                new ApiBody<>(data, "CREATED SUCCESSFUL")
        );
    }
}