package kwh.cofshop.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ApiBody <T>{

    private String message; // 메시지

    private LocalDateTime timestamp; // 응답 시각

    private T data; // DTO 데이터 등의 정보
}
