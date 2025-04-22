package kwh.cofshop.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.security.dto.TokenDto;
import kwh.cofshop.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    public ResponseEntity<TokenDto> reissue(HttpServletRequest request) {
        return authService.reissue(request);
    }



/*    // 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인 기능입니다.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginDto loginDto) {
        LoginResponseDto loginResponseDto = authService.login(loginDto);

        return ResponseEntity.ok()
                .body(ApiResponse.OK(loginResponseDto));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.OK("로그아웃 완료"));
    }*/
}
