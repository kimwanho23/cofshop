
package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemCreateRequestDto;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.response.ItemCreateResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.mapper.ItemCreateMapper;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    private final ItemMapper itemMapper; // 매퍼 클래스
    private final ItemCreateMapper itemCreateMapper;

    // 연관관계 서비스
    private final ItemImgService itemImgService; // 이미지 서비스
    private final ItemOptionService itemOptionService; // 옵션 서비스

    @Transactional
    public ItemCreateResponseDto save(ItemCreateRequestDto itemCreateRequestDto) throws IOException {
       Member seller = getMember();

       Item savedItem = getSavedItem(itemCreateRequestDto.getItemRequestDto(), seller);

       // 이미지 저장
       itemImgService.saveItemImages(savedItem,itemCreateRequestDto.getItemImgRequestDto());

       // 옵션 저장
       itemOptionService.saveItemOptions(savedItem, itemCreateRequestDto.getItemOptionRequestDto());

       // Response DTO 반환
       return itemCreateMapper.toResponseDto(savedItem);
   }

    @Transactional
    private Item getSavedItem(ItemRequestDto itemRequestDto, Member seller) {
        Item item = itemMapper.toEntity(itemRequestDto); // DTO 엔티티 변환
        item.setSeller(seller); // 연관관계 편의 메서드
        return itemRepository.save(item);
    }

    private Member getMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("인증 성공: {}", authentication.getName());

        return memberRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
    }

}

