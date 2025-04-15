package kwh.cofshop.item.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // 카테고리 이름

    private int depth; // 깊이

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<ItemCategory> itemCategories = new ArrayList<>(); // 연결 테이블

    // 자기 자신과의 연관관계 (부모 카테고리)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")  // 부모를 가리키는 외래키
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Category> children = new ArrayList<>();

    @Builder
    public Category(String name, Category parent) {
        this.name = name;
        if (parent != null) {
            parent.addChildCategory(this);  // 연관관계 메서드 활용
        } else {
            this.depth = 1;  // 루트 카테고리
        }
    }

    public void addChildCategory(Category child) {
        this.children.add(child);
        child.parent = this;  // 부모 설정
        child.depth = this.depth + 1; // 깊이 자동 설정
    }
}