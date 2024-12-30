package kwh.cofshop.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApiHeader {
    private HttpStatus status; //200, 400 등의 HTTP 상태 코드
    private String codeName; // 코드 이름 (에러 코드명은 우리가 지정해주자!)

}
