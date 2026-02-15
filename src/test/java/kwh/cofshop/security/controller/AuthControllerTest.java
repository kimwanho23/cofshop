package kwh.cofshop.security.controller;

import kwh.cofshop.security.dto.TokenResponseDto;
import kwh.cofshop.security.service.AuthService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcFactory.build(authController);
    }

    @Test
    @DisplayName("Reissue returns 200")
    void reissueToken() throws Exception {
        TokenResponseDto tokenDto = TokenResponseDto.builder()
                .grantType("Bearer")
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(authService.reissue(any(), any())).thenReturn(tokenDto);

        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isOk());
    }
}
