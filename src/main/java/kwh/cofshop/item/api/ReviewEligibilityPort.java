package kwh.cofshop.item.api;

public interface ReviewEligibilityPort {

    boolean hasCompletedPurchase(Long memberId, Long itemId);
}
