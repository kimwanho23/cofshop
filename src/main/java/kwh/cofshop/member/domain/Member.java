package kwh.cofshop.member.domain;

import jakarta.persistence.*;
import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", uniqueConstraints = {@UniqueConstraint(columnNames = "email")}) // 이메일의 유니크 제약 조건
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id; // 기본 키

    @Column(nullable = false, unique = true)
    private String email;  // 이메일(Unique)

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;  // 이름

    @Column(name = "member_pwd", nullable = false, length = 255)
    private String memberPwd;  // 비밀번호

    @Column(nullable = false)
    private String tel;  // 전화번호

    @Enumerated(EnumType.STRING)
    @Column(name = "member_state", nullable = false)
    private MemberState memberState;  // 회원 상태 (0: 탈퇴, 1: 가입, 2: 정지), 회원가입 시 1로 저장해야 한다.

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer point;  // 최초 회원 가입 시 포인트는 0으로 설정

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // 유저 권한 (0: 관리자, 1: 일반 유저)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 가입일자

    @Column(name = "last_password_change", nullable = false)
    private LocalDateTime lastPasswordChange;  // 비밀번호 마지막 변경 일자.. 최초 생성 시 가입일자와 동일하게

    @Column(name = "last_login")
    private LocalDateTime lastLogin;  // 마지막 로그인 일자, 로그인 시 변해야 한다.

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> itemList = new ArrayList<>();  // 판매자가 등록한 상품 목록

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToOne(mappedBy = "member", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cart cart;

    @Builder
    public Member(Long id, String email, String memberName, String memberPwd,
                  String tel) {
        this.id = id;
        this.email = email;
        this.memberName = memberName;
        this.memberPwd = memberPwd;
        this.tel = tel;
        this.cart = new Cart(this);
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();  // 가입일자
        this.lastPasswordChange = this.createdAt;  // 비밀번호 변경일자
        this.memberState = this.memberState == null ? MemberState.ACTIVE : this.memberState;
        this.role = this.role == null ? Role.MEMBER : this.role;
        this.point = this.point == null ? 0 : this.point;
    }

    public Cart createCart() {
        return new Cart(this);  // Cart 생성 시 연관관계 설정
    }

    public void changePassword(String newPassword) {
        this.memberPwd = newPassword;
        this.lastPasswordChange = LocalDateTime.now();
    }

    // 포인트 사용
    public void usePoint(int amount) {
        if (amount < 0 && this.point + amount < 0) {
            throw new IllegalStateException("보유한 포인트가 부족합니다.");
        }
        this.point -= amount;
    }

    // 포인트 복구
    public void restorePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("복구할 포인트는 0보다 커야 합니다.");
        }
        this.point += amount;
    }

    public void updatePoint(int amount) {
        this.point += amount;
    }

    public void changeMemberState(MemberState newState) { // 멤버 상태 변경
        this.memberState = newState;
    }

}
