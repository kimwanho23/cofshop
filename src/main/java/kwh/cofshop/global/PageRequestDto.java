package kwh.cofshop.global;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PageRequestDto {
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    @NotNull(message = "정렬 방향은 필수입니다.")
    private Sort.Direction direction = Sort.Direction.ASC;

    @NotBlank(message = "정렬 기준 필드는 필수입니다.")
    private String sortBy = "id"; // 기본 정렬 필드

    public Pageable toPageable() {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction safeDirection = direction == null ? Sort.Direction.ASC : direction;
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        return PageRequest.of(safePage - 1, safeSize, safeDirection, safeSortBy);
    }


}
