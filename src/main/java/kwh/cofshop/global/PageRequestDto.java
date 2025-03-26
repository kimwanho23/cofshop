package kwh.cofshop.global;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PageRequestDto {
    private int page = 1;
    private int size = 10;
    private Sort.Direction direction = Sort.Direction.ASC;
    private String sortBy = "id"; // 기본 정렬 필드

    public Pageable toPageable() {
        return PageRequest.of(page - 1, size, direction, sortBy);
    }


}