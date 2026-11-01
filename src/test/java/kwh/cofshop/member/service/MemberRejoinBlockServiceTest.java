package kwh.cofshop.member.service;

import kwh.cofshop.member.domain.MemberRejoinBlock;
import kwh.cofshop.member.repository.MemberRejoinBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberRejoinBlockServiceTest {

    @Mock
    private MemberRejoinBlockRepository repository;

    @InjectMocks
    private MemberRejoinBlockService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "blockDays", 30);
        ReflectionTestUtils.setField(service, "hashSecret", "test-secret");
    }

    @Test
    @DisplayName("차단: 이메일과 전화번호 원문을 저장하지 않고 30일 뒤 만료")
    void block_savesHashes() {
        when(repository.findByIdentifierHash(any())).thenReturn(Optional.empty());

        service.block("User@Example.com", "010-1234-5678");

        ArgumentCaptor<MemberRejoinBlock> captor = ArgumentCaptor.forClass(MemberRejoinBlock.class);
        verify(repository, times(2)).save(captor.capture());
        for (MemberRejoinBlock block : captor.getAllValues()) {
            assertThat(block.getIdentifierHash()).hasSize(64).doesNotContain("example").doesNotContain("1234");
            assertThat(block.getExpiresAt()).isCloseTo(LocalDateTime.now().plusDays(30), within(1, ChronoUnit.MINUTES));
        }
    }

    @Test
    @DisplayName("차단: 이미 있는 해시는 기간만 늘림")
    void block_extendsExisting() {
        MemberRejoinBlock existing = new MemberRejoinBlock("hash", LocalDateTime.now().minusDays(1));
        when(repository.findByIdentifierHash(any())).thenReturn(Optional.of(existing));

        service.block("user@example.com", null);

        assertThat(existing.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
        verify(repository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("확인: 대소문자와 하이픈이 달라도 같은 값으로 판단")
    void isBlocked_normalizes() {
        when(repository.existsByIdentifierHashInAndExpiresAtAfter(anyCollection(), any())).thenReturn(true);

        assertThat(service.isBlocked(" USER@example.com ", "010-1234-5678")).isTrue();
        assertThat(service.isBlocked("user@example.com", "01012345678")).isTrue();

        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(repository, times(2)).existsByIdentifierHashInAndExpiresAtAfter(captor.capture(), any());
        List<Collection<String>> calls = captor.getAllValues();
        assertThat(calls.get(0)).hasSize(2).containsExactlyElementsOf(calls.get(1));
    }

    @Test
    @DisplayName("확인: 비밀키가 다르면 해시도 달라짐")
    void hash_dependsOnSecret() {
        when(repository.findByIdentifierHash(any())).thenReturn(Optional.empty());
        ArgumentCaptor<MemberRejoinBlock> captor = ArgumentCaptor.forClass(MemberRejoinBlock.class);

        service.block("user@example.com", null);
        ReflectionTestUtils.setField(service, "hashSecret", "other-secret");
        service.block("user@example.com", null);

        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getIdentifierHash())
                .isNotEqualTo(captor.getAllValues().get(1).getIdentifierHash());
    }

    @Test
    @DisplayName("확인: 값이 없으면 조회하지 않음")
    void isBlocked_noIdentifier() {
        assertThat(service.isBlocked(" ", null)).isFalse();

        verify(repository, never()).existsByIdentifierHashInAndExpiresAtAfter(anyCollection(), any());
    }
}
