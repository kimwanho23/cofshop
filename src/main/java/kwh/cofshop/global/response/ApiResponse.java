package kwh.cofshop.global.response;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(ApiHeader header, ApiBody<T> body) {

    // 성공 응답 생성 메서드
    public static <T> ApiResponse<T> OK(T data) {
        return new ApiResponse<>(
                new ApiHeader(HttpStatus.OK, "S-1"),
                new ApiBody<>(data, "API LOAD SUCCESSFUL")
        );
    }

    public static <T> ApiResponse<T> Created(T data) {
        return new ApiResponse<>(
                new ApiHeader(HttpStatus.CREATED, "C-1"),
                new ApiBody<>(data, "CREATED SUCCESSFUL")
        );
    }

}