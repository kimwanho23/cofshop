package kwh.cofshop.member.contoller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.request.LoginDto;
import kwh.cofshop.member.dto.response.LoginResponseDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.service.MemberService;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class MemberController {

    private final MemberService memberService;

    // 회원가입
    @PostMapping(value = "/signup")
    @Operation(summary = "회원가입", description = "회원가입 기능입니다.")
    public ResponseEntity<ApiResponse<MemberResponseDto>> save(@Valid @RequestBody MemberRequestDto memberSaveDto) {
        MemberResponseDto memberSaveResponseDto = memberService.save(memberSaveDto);

        return ResponseEntity.ok()
                .body(ApiResponse.Created(memberSaveResponseDto)); // 메서드 체이닝 패턴
    }

    // 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인 기능입니다.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginDto loginDto) {
        LoginResponseDto loginResponseDto = memberService.login(loginDto);

        return ResponseEntity.ok()
                .body(ApiResponse.OK(loginResponseDto));
    }

    // 관리자의 멤버 상태 변경
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PatchMapping("/{id}/state")
    @Operation(summary = "멤버 상태 변경", description = "관리자 전용, 멤버 상태 변경 기능입니다.")
    public ResponseEntity<ApiResponse<String>> updateMemberStateByAdmin(
            @PathVariable Long id,
            @RequestParam MemberState memberState) {
        memberService.changeMemberState(id, memberState);
        return ResponseEntity.ok(ApiResponse.OK("회원 상태를 변경했습니다."));
    }

    // 회원 정보
    @GetMapping("/{id}")
    @Operation(summary = "회원 정보 열람", description = "회원의 정보를 열람합니다.")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(ApiResponse.OK(memberService.findMember(   id)));
    }

    // 모든 멤버 리스트 조회 (관리자 권한)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(value="/allUsers")
    @Operation(summary = "전체 멤버 조회", description = "관리자 전용, 모든 멤버를 조회합니다.")
    public ResponseEntity<ApiResponse<List<MemberResponseDto>>> memberList(){
        return ResponseEntity.ok()
                .body(ApiResponse.OK(memberService.memberLists()));
    }

    // 회원 탈퇴
    @PatchMapping("/quit")
    @Operation(summary = "회원탈퇴", description = "회원탈퇴 기능입니다.")
    public ResponseEntity<ApiResponse<String>> quitMember(
            @Parameter(hidden = true) @LoginMember CustomUserDetails customUserDetails) {
        memberService.changeMemberState(customUserDetails.getId(), MemberState.QUIT);
        return ResponseEntity.ok()
                .body(ApiResponse.OK("회원 탈퇴가 완료되었습니다."));
    }

    // 회원 비밀 번호 변경
    @PatchMapping("/password")
    @Operation(summary = "비밀번호 변경", description = "회원의 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<String>> memberPasswordChange(
            @Parameter(hidden = true) @LoginMember CustomUserDetails customUserDetails,
            @RequestParam String password) {
        memberService.updateMemberPassword(customUserDetails.getId(), password);
        return ResponseEntity.ok()
                .body(ApiResponse.OK("비밀번호가 변경되었습니다."));
    }

    // 포인트 변경
    @PostMapping("/updatePoint")
    public ResponseEntity<ApiResponse<Integer>> updatePoint(
            @LoginMember CustomUserDetails customUserDetails,
            @RequestParam int amount
    ){
        Integer currentPoint = memberService.updatePoint(customUserDetails.getId(), amount);
        return ResponseEntity.ok()
                .body(ApiResponse.OK(currentPoint));
    }
}
