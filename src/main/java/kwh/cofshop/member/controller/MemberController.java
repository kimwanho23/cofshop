package kwh.cofshop.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.member.dto.request.MemberPasswordUpdateRequestDto;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.member.service.MemberLoginHistoryService;
import kwh.cofshop.member.service.MemberService;
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
    private final MemberLoginHistoryService memberLoginHistoryService;

    //////////// @GET

    // 회원 정보
    @Operation(summary = "회원 정보 열람", description = "회원의 정보를 열람합니다.")
    @PreAuthorize("hasRole('ADMIN') or #memberId == authentication.principal.id")
    @GetMapping("/{memberId}")
    public MemberResponseDto getMemberById(@PathVariable Long memberId) {
        return memberService.findMember(memberId);
    }

    // 모든 멤버 리스트 조회 (관리자 권한)
    @Operation(summary = "전체 멤버 조회", description = "관리자 전용, 모든 멤버를 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<MemberResponseDto> getAllMembers() {
        return memberService.memberLists();
    }

    // 멤버의 로그인 기록 열람
    @Operation(summary = "멤버 로그인 기록 조회", description = "조회 결과")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/history/{memberId}")
    public List<MemberLoginEvent> getLoginMemberHistory(@PathVariable Long memberId) {
        return memberLoginHistoryService.getUserLoginHistory(memberId);
    }


    //////////// @POST
    // 회원가입
    @Operation(summary = "회원가입", description = "회원가입 기능입니다.")
    @PostMapping(value = "/signup")
    public ResponseEntity<MemberResponseDto> signup(@Valid @RequestBody MemberRequestDto memberSaveDto) {
        MemberResponseDto responseDto = memberService.signUp(memberSaveDto);
        return ResponseEntity.created(URI.create("/api/members/" + responseDto.getMemberId()))
                .body(responseDto);
    }

    //////////// @PUT, PATCH


    // 관리자의 멤버 상태 변경
    @Operation(summary = "멤버 상태 변경", description = "관리자 전용, 멤버 상태 변경 기능입니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{memberId}/state")
    public ResponseEntity<Void> updateMemberStateByAdmin(
            @PathVariable Long memberId,
            @RequestParam MemberState memberState) {
        memberService.changeMemberState(memberId, memberState);
        return ResponseEntity.noContent().build();
    }

    // 회원 탈퇴
    @Operation(summary = "회원탈퇴", description = "회원탈퇴 기능입니다.")
    @PatchMapping("/me/state")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> quitMember(@LoginMember Long memberId) {
        memberService.changeMemberState(memberId, MemberState.QUIT);
        return ResponseEntity.noContent().build();
    }

    // 회원 비밀 번호 변경
    @Operation(summary = "비밀번호 변경", description = "회원의 비밀번호를 변경합니다.")
    @PatchMapping("/me/password")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> changePassword(
            @LoginMember Long memberId,
            @Valid @RequestBody MemberPasswordUpdateRequestDto requestDto) {
        memberService.updateMemberPassword(memberId, requestDto.getPassword());
        return ResponseEntity.noContent().build();
    }

    // 포인트 변경
    @Operation(summary = "포인트 변경", description = "회원의 포인트를 변경합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{memberId}/point")
    public Integer updatePoint(
            @PathVariable Long memberId,
            @RequestParam int amount // 양수 = 적립, 음수 = 차감
    ) {
        return memberService.updatePoint(memberId, amount);
    }

    // @DELETE

}
