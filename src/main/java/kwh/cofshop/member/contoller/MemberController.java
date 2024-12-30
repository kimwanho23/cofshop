package kwh.cofshop.member.contoller;

import jakarta.validation.Valid;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.member.dto.MemberRequestDto;
import kwh.cofshop.member.dto.MemberResponseDto;
import kwh.cofshop.member.service.MemberService;
/*import kwh.cofshop.security.CustomUserDetails;*/
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
/*import org.springframework.security.core.Authentication;*/
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping(value = "/m/signup")
    public ResponseEntity<ApiResponse<MemberRequestDto>> save(@Valid @RequestBody MemberRequestDto memberSaveDto) {
        memberService.save(memberSaveDto);
        return ResponseEntity.ok()
                .body(ApiResponse.Created(memberSaveDto)); // 메서드 체이닝 패턴
    }

    @GetMapping("/m/{id}")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(ApiResponse.OK(memberService.findMember(   id)));
    }

/*    @GetMapping("/m/info")
    public ResponseEntity<MemberResponseDto> getUserInfo(Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        MemberResponseDto memberInfo = memberService.findMember(userDetails.getUsername());

        return ResponseEntity.ok(memberInfo);
    }*/

    @GetMapping(value="/allUsers")
    public ResponseEntity<ApiResponse<List<MemberResponseDto>>> memberList(){ // 멤버 리스트 조회
        return ResponseEntity.ok()
                .body(ApiResponse.OK(memberService.memberLists()));
    }

    /* 이 아래로 테스트용 코드 */////////////////////////////////////////////////////////////////////////////////////

    // 인증된 사용자만 접근 가능한 페이지
    @GetMapping(value = "/protected")
    public ResponseEntity<ApiResponse<String>> protectedEndpoint() {
        return ResponseEntity.ok()
                .body(ApiResponse.OK("보호된 페이지에 접근 성공!"));
    }
}
