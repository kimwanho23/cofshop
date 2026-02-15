package kwh.cofshop.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Column(name = "member_pwd", nullable = false, length = 255)
    private String memberPwd;

    @Column(nullable = false)
    private String tel;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_state", nullable = false)
    private MemberState memberState;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_password_change", nullable = false)
    private LocalDateTime lastPasswordChange;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Builder
    public Member(Long id, String email, String memberName, String memberPwd, String tel) {
        this.id = id;
        this.email = email;
        this.memberName = memberName;
        this.memberPwd = memberPwd;
        this.tel = tel;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.lastPasswordChange = this.createdAt;
        this.memberState = this.memberState == null ? MemberState.ACTIVE : this.memberState;
        this.role = this.role == null ? Role.MEMBER : this.role;
        this.point = this.point == null ? 0 : this.point;
    }

    public void changePassword(String newPassword) {
        this.memberPwd = newPassword;
        this.lastPasswordChange = LocalDateTime.now();
    }

    public void usePoint(int amount) {
        if (amount <= 0) {
            throw new BusinessException(BusinessErrorCode.INVALID_POINT_OPERATION);
        }
        int currentPoint = this.point == null ? 0 : this.point;
        if (currentPoint < amount) {
            throw new BusinessException(BusinessErrorCode.INSUFFICIENT_POINT);
        }
        this.point = currentPoint - amount;
    }

    public void restorePoint(int amount) {
        if (amount <= 0) {
            throw new BusinessException(BusinessErrorCode.INVALID_POINT_OPERATION);
        }
        this.point += amount;
    }

    public void updatePoint(int amount) {
        this.point += amount;
    }

    public void changeMemberState(MemberState newState) {
        this.memberState = newState;
    }
}