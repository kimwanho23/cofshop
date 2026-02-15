package kwh.cofshop.payment.domain;

import lombok.Getter;

@Getter
public enum PGProvider {
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

    PGProvider(String code) {
        this.code = code;
    }
}
