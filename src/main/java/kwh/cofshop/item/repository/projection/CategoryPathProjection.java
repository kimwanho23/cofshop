package kwh.cofshop.item.repository.projection;

public interface CategoryPathProjection {
    Long getId();

    String getName();

    Long getParentCategoryId();
}
