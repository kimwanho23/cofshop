package kwh.cofshop.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiBody <T>{

    private T data; // DTO 데이터 등의 정보

    private String message; // 메시지
}
