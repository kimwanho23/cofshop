package kwh.cofshop.member.api;

public interface MemberPointPort {

    void restorePoint(Long memberId, int point);
}
