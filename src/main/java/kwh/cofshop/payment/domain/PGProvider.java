package kwh.cofshop.payment.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum PGProvider {
    KAKAOPAY("kakaopay"),
    TOSSPAY("tosspay"),
    KGINICIS("html5_inicis"), // KG이니시스
    NHNKCP("kcp"), // NHN KCP
    KICC("kicc"), // 이지페이(KICC)
    SETTLE("settle"), // 헥토파이낸셜
    DAOU("daou"),
    TOSSPAYMENTS("tosspayments"),
    SMARTRO("smartro_v2"),
    NICE("nice"),
    NICE_V2("nice_v2"),
    WELCOME("welcome");

    private final String code;
    private static final Set<String> SUPPORTED_CODES = Arrays.stream(values())
            .map(PGProvider::getCode)
            .collect(Collectors.toUnmodifiableSet());

    PGProvider(String code) {
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
