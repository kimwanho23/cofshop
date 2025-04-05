package kwh.cofshop.member.contoller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.service.MemberService;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class MemberController {

    private final MemberService memberService;

    //////////// @GET

    // 회원 정보
    @Operation(summary = "회원 정보 열람", description = "회원의 정보를 열람합니다.")
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberById(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.OK(memberService.findMember(memberId)));
    }

    // 모든 멤버 리스트 조회 (관리자 권한)
    @Operation(summary = "전체 멤버 조회", description = "관리자 전용, 모든 멤버를 조회합니다.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberResponseDto>>> getAllMembers() {
        return ResponseEntity.ok(ApiResponse.OK(memberService.memberLists()));
    }


    //////////// @POST

    // 회원가입
    @Operation(summary = "회원가입", description = "회원가입 기능입니다.")
    @PostMapping(value = "/signup")
    public ResponseEntity<ApiResponse<MemberResponseDto>> signup(@Valid @RequestBody MemberRequestDto memberSaveDto) {
        MemberResponseDto responseDto = memberService.save(memberSaveDto);
        return ResponseEntity.created(URI.create("/api/members/" + responseDto.getMemberId()))
                .body(ApiResponse.Created(responseDto));
    }

    //////////// @PUT, PATCH


    // 관리자의 멤버 상태 변경
    @Operation(summary = "멤버 상태 변경", description = "관리자 전용, 멤버 상태 변경 기능입니다.")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PatchMapping("/{memberId}/state")
    public ResponseEntity<ApiResponse<String>> updateMemberStateByAdmin(
            @PathVariable Long memberId,
            @RequestParam MemberState memberState) {
        memberService.changeMemberState(memberId, memberState);
        return ResponseEntity.ok(ApiResponse.OK("회원 상태를 변경했습니다."));
    }

    // 회원 탈퇴
    @Operation(summary = "회원탈퇴", description = "회원탈퇴 기능입니다.")
    @PatchMapping("/me/state")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ApiResponse<String>> quitMember(@LoginMember CustomUserDetails user) {
        memberService.changeMemberState(user.getId(), MemberState.QUIT);
        return ResponseEntity.ok(ApiResponse.OK("회원 탈퇴가 완료되었습니다."));
    }

    // 회원 비밀 번호 변경
    @Operation(summary = "비밀번호 변경", description = "회원의 비밀번호를 변경합니다.")
    @PatchMapping("/me/password")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @LoginMember CustomUserDetails user,
            @RequestParam String password) {
        memberService.updateMemberPassword(user.getId(), password);
        return ResponseEntity.ok(ApiResponse.OK("비밀번호가 변경되었습니다."));
    }

    // 포인트 변경
    @PatchMapping("/{memberId}/point")
    public ResponseEntity<ApiResponse<Integer>> updatePoint(
            @PathVariable Long memberId,
            @RequestParam int amount // 양수 = 적립, 음수 = 차감
    ) {
        Integer currentPoint = memberService.updatePoint(memberId, amount);
        return ResponseEntity.ok(ApiResponse.OK(currentPoint));
    }

    // @DELETE

}
