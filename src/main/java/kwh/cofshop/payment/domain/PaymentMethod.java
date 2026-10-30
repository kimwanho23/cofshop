package kwh.cofshop.payment.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum PaymentMethod {
    CARD("card"),
    VBANK("vbank"),
    TRANS("trans"),
    PHONE("phone"),
    SAMSUNG("samsung"),
    POINT("point"),
    SSGPAY("ssgpay"),
    LPAY("lpay"),
    KPAY("kpay"),
    NAVERPAY("naverpay"),
    PAYCO("payco"),
    KAKAOPAY("kakaopay"),
    TOSSPAY("tosspay");

    private final String code;
    private static final Set<String> SUPPORTED_CODES = Arrays.stream(values())
            .map(PaymentMethod::getCode)
            .collect(Collectors.toUnmodifiableSet());

    PaymentMethod(String code) {
        this.code = code;
    }

    public static boolean isSupported(String rawCode) {
        if (rawCode == null) {
            return false;
        }
        return SUPPORTED_CODES.contains(rawCode.toLowerCase(Locale.ROOT));
    }

    public static String normalize(String rawCode) {
        return rawCode == null ? null : rawCode.toLowerCase(Locale.ROOT);
    }
}
