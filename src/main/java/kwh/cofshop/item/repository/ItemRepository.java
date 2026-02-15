package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.repository.custom.ItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {

    @Query(value = """
            SELECT * FROM item
            WHERE MATCH(item_name) AGAINST (:keyword IN NATURAL LANGUAGE MODE)
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Item> fullTextSearch(@Param("keyword") String keyword,
                              @Param("limit") int limit,
                              @Param("offset") int offset);
}
