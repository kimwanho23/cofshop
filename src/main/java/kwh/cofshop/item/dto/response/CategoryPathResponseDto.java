package kwh.cofshop.item.dto.response;

public record CategoryPathResponseDto(
        Long id,
        String name,
        Long parentCategoryId
) {
}
