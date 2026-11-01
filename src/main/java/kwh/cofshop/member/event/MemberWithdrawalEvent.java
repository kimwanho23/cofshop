package kwh.cofshop.member.event;

// 회원 정보를 지우기 직전에 같은 트랜잭션에서 발행한다. 리스너가 예외를 던지면 탈퇴가 취소된다
public record MemberWithdrawalEvent(Long memberId, String email, String memberName, String tel) {

    @Override
    public String toString() {
        return "MemberWithdrawalEvent[memberId=" + memberId + "]";
    }
}
