package kwh.cofshop.item.dto.request;

public interface CategoryPathRequestDto {
    Long getId();
    String getName();
    Long getParentCategoryId();
}
