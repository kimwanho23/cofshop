package kwh.cofshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.item.domain.ImgType;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.service.ItemService;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.CustomUserDetails;
import kwh.cofshop.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public abstract class TestSettingUtils {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected CouponRepository couponRepository;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected ItemRepository itemRepository;


    protected Authentication createTestAuthentication(Member member) {
        Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + member.getRole())
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                member.getId(),
                member.getEmail(),
                "",
                authorities,
                member.getMemberState(),
                member.getLastPasswordChange()
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    protected String getToken() {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Authentication testAuthentication = createTestAuthentication(member);
        return jwtTokenProvider.createAuthToken(testAuthentication).getAccessToken();
    }
    protected Item createTestItem() throws Exception {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        ItemRequestDto requestDto = getItemRequestDto();
        requestDto.setItemImgRequestDto(getImgRequestDto());
        requestDto.setItemOptionRequestDto(getItemOptionRequestDto());
        requestDto.setCategoryIds(new ArrayList<>());

        List<MultipartFile> imageFiles = generateImageFiles();

        ItemResponseDto responseDto = itemService.saveItem(requestDto, member.getId(), imageFiles);

        return itemRepository.findById(responseDto.getId()).orElseThrow();
    }

    protected Member createMember() {
        Member customer = Member.builder()
                .email("testEmail123@test.com")
                .memberName("테스트용")
                .memberPwd("password1234")
                .tel("010-1234-5678")
                .build();
        return memberRepository.save(customer);
    }

    protected Coupon makeCoupon(){
        Coupon coupon = Coupon.builder()
                .name("할인 쿠폰")
                .couponCount(null)
                .couponCreatedAt(LocalDate.now())
                .discountValue(15)
                .maxDiscount(0)
                .minOrderPrice(null)
                .state(CouponState.AVAILABLE)
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusDays(100))
                .build();
        return couponRepository.save(coupon);
    }


    // 이미지 파일
    private List<MultipartFile> generateImageFiles() {
        List<MultipartFile> images = new ArrayList<>();
        images.add(new MockMultipartFile("images", "test.jpg", "image/jpeg", "test data".getBytes()));
        images.add(new MockMultipartFile("images", "test1.jpg", "image/jpeg", "test data 1".getBytes()));
        images.add(new MockMultipartFile("images", "test2.jpg", "image/jpeg", "test data 2".getBytes()));
        return images;
    }

    // 이미지 DTO
    private static List<ItemImgRequestDto> getImgRequestDto() {
        List<ItemImgRequestDto> imgRequestDto = new ArrayList<>();

        ItemImgRequestDto repDto = new ItemImgRequestDto();
        repDto.setImgType(ImgType.REPRESENTATIVE);
        imgRequestDto.add(repDto);

        ItemImgRequestDto subDto1 = new ItemImgRequestDto();
        subDto1.setImgType(ImgType.SUB);
        imgRequestDto.add(subDto1);

        ItemImgRequestDto subDto2 = new ItemImgRequestDto();
        subDto2.setImgType(ImgType.SUB);
        imgRequestDto.add(subDto2);

        return imgRequestDto;
    }


    // 옵션 DTO
    private List<ItemOptionRequestDto> getItemOptionRequestDto() {
        return List.of(
                createOption("Small Size", 0, 100),
                createOption("Large Size", 500, 50)
        );
    }

    // 옵션 만들기
    private ItemOptionRequestDto createOption(String description, int additionalPrice, int stock) {
        ItemOptionRequestDto option = new ItemOptionRequestDto();
        option.setDescription(description);
        option.setAdditionalPrice(additionalPrice);
        option.setStock(stock);
        return option;
    }

    private ItemRequestDto getItemRequestDto() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setItemName("커피 원두");
        requestDto.setPrice(20000);
        requestDto.setOrigin("아르헨티나");
        requestDto.setDiscount(0); // 할인율
        requestDto.setDeliveryFee(1000); // 배송비
        requestDto.setItemLimit(5); // 수량 제한
        return requestDto;
    }

    protected List<CartItemRequestDto> getCartItemRequestDto(Item item) {
        List<CartItemRequestDto> cartItemRequestDtoList = new ArrayList<>();

        CartItemRequestDto cartItemRequestDto1 = new CartItemRequestDto();
        cartItemRequestDto1.setItemId(item.getId());
        cartItemRequestDto1.setOptionId(3L);
        cartItemRequestDto1.setQuantity(1);
        cartItemRequestDtoList.add(cartItemRequestDto1);

        CartItemRequestDto cartItemRequestDto2 = new CartItemRequestDto();
        cartItemRequestDto2.setItemId(item.getId());
        cartItemRequestDto2.setOptionId(3L);
        cartItemRequestDto2.setQuantity(2);
        cartItemRequestDtoList.add(cartItemRequestDto2);

        CartItemRequestDto cartItemRequestDto3 = new CartItemRequestDto();
        cartItemRequestDto3.setItemId(item.getId());
        cartItemRequestDto3.setOptionId(4L);
        cartItemRequestDto3.setQuantity(2);
        cartItemRequestDtoList.add(cartItemRequestDto3);
        return cartItemRequestDtoList;
    }

}