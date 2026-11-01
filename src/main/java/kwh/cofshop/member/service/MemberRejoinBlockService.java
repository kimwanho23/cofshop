package kwh.cofshop.member.service;

import kwh.cofshop.member.domain.MemberRejoinBlock;
import kwh.cofshop.member.repository.MemberRejoinBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

// 탈퇴 후 재가입으로 쿠폰 중복 발급 제한을 우회하지 못하게 한다.
// 이메일과 전화번호는 경우의 수가 적어 단순 해시는 역산되므로 비밀키로 HMAC을 만든다
@Service
@RequiredArgsConstructor
public class MemberRejoinBlockService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final MemberRejoinBlockRepository memberRejoinBlockRepository;

    @Value("${member.rejoin-block.days:30}")
    private int blockDays;

    @Value("${member.rejoin-block.hash-secret:${jwt.secret-key}}")
    private String hashSecret;

    @Transactional
    public void block(String email, String tel) {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(blockDays);
        for (String hash : hashes(email, tel)) {
            memberRejoinBlockRepository.findByIdentifierHash(hash).ifPresentOrElse(
                    block -> block.extendUntil(expiresAt),
                    () -> memberRejoinBlockRepository.save(new MemberRejoinBlock(hash, expiresAt))
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(String email, String tel) {
        List<String> hashes = hashes(email, tel);
        return !hashes.isEmpty()
                && memberRejoinBlockRepository.existsByIdentifierHashInAndExpiresAtAfter(hashes, LocalDateTime.now());
    }

    @Transactional
    public int deleteExpired() {
        return memberRejoinBlockRepository.deleteExpired(LocalDateTime.now());
    }

    private List<String> hashes(String email, String tel) {
        List<String> hashes = new ArrayList<>();
        if (email != null && !email.isBlank()) {
            hashes.add(hmac("email:" + email.trim().toLowerCase(Locale.ROOT)));
        }
        String telDigits = tel == null ? "" : tel.replaceAll("\\D", "");
        if (!telDigits.isEmpty()) {
            hashes.add(hmac("tel:" + telDigits));
        }
        return hashes;
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to hash rejoin identifier", e);
        }
    }
}
