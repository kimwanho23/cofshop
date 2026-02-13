package kwh.cofshop.global.exception.errorcodes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeUniquenessTest {

    @Test
    @DisplayName("모든 에러 코드는 유일해야 한다")
    void allErrorCodesShouldBeUnique() {
        List<String> codes = Stream.of(
                        Arrays.stream(BusinessErrorCode.values()),
                        Arrays.stream(BadRequestErrorCode.values()),
                        Arrays.stream(UnauthorizedErrorCode.values()),
                        Arrays.stream(ForbiddenErrorCode.values()),
                        Arrays.stream(DataIntegrityViolationErrorCode.values()),
                        Arrays.stream(InternalServerErrorCode.values())
                )
                .flatMap(stream -> stream)
                .map(ErrorCode::getCode)
                .toList();

        Set<String> uniqueCodes = new HashSet<>(codes);

        assertThat(uniqueCodes).hasSize(codes.size());
    }
}
