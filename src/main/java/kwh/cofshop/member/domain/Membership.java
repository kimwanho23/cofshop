package kwh.cofshop.member.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Membership {
    @Id
    @GeneratedValue
    private Long id;

    private Long memberId;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private MembershipTier tier; // 멤버십 등급

    public boolean isActive() {
        return LocalDate.now().isBefore(endDate);
    }
}