package kwh.cofshop.item.service;

import kwh.cofshop.file.storage.FileStore;
import kwh.cofshop.file.storage.TempUploadFileService;
import kwh.cofshop.file.storage.UploadFile;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.item.domain.ImgType;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemImgRepository;
import kwh.cofshop.item.vo.ItemImgUploadVO;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemImgServiceTest {

    @Mock
    private FileStore fileStore;

    @Mock
    private ItemImgRepository itemImgRepository;

    @Mock
    private TempUploadFileService tempUploadFileService;

    @InjectMocks
    private ItemImgService itemImgService;

    @Test
    @DisplayName("이미지 저장")
    void saveItemImages() throws IOException {
        Item item = createItem();
        ItemImgRequestDto dto = new ItemImgRequestDto(null, ImgType.REPRESENTATIVE);
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "data".getBytes());

        when(fileStore.storeFiles(List.of(file))).thenReturn(List.of(new UploadFile("test.jpg", "store.jpg")));
        when(itemImgRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(ItemImg.builder().build()));

        List<ItemImg> result = itemImgService.saveItemImages(item, List.of(new ItemImgUploadVO(dto, file)));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("이미지 삭제: 빈 리스트")
    void deleteItemImages_empty() {
        Item item = createItem();
        itemImgService.deleteItemImages(item, null);

        verify(itemImgRepository, never()).findByItemIdAndItemImgId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("이미지 삭제: 성공")
    void deleteItemImages_success() {
        Item item = createItem();
        ReflectionTestUtils.setField(item, "id", 1L);

        ItemImg img = ItemImg.builder().imgName("store.jpg").imgUrl("/images/store.jpg").item(item).build();
        when(itemImgRepository.findByItemIdAndItemImgId(1L, List.of(1L))).thenReturn(List.of(img));

        itemImgService.deleteItemImages(item, List.of(1L));

        verify(fileStore).deleteFile("store.jpg");
        verify(itemImgRepository).deleteAll(List.of(img));
    }

    @Test
    @DisplayName("이미지 추가: 파일 수 불일치")
    void addItemImages_mismatch() {
        Item item = createItem();
        ItemImgRequestDto dto = new ItemImgRequestDto(null, ImgType.REPRESENTATIVE);
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> itemImgService.addItemImages(item, List.of(dto), List.of(file, file), 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("이미지 추가: 성공")
    void addItemImages_success() throws IOException {
        Item item = createItem();
        ItemImgRequestDto dto = new ItemImgRequestDto(null, ImgType.REPRESENTATIVE);
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "data".getBytes());

        when(fileStore.storeFiles(List.of(file))).thenReturn(List.of(new UploadFile("test.jpg", "store.jpg")));

        itemImgService.addItemImages(item, List.of(dto), List.of(file), 1L);

        verify(itemImgRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("이미지 업데이트")
    void updateItemImages() throws IOException {
        Item item = createItem();
        ReflectionTestUtils.setField(item, "id", 1L);

        ItemImg img = ItemImg.builder().imgName("store.jpg").imgUrl("/images/store.jpg").item(item).build();
        when(itemImgRepository.findByItemIdAndItemImgId(1L, List.of(1L))).thenReturn(List.of(img));

        ItemUpdateRequestDto dto = new ItemUpdateRequestDto();
        dto.setDeleteImgIds(List.of(1L));
        dto.setAddItemImgs(List.of(new ItemImgRequestDto(null, ImgType.REPRESENTATIVE)));

        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "data".getBytes());
        when(fileStore.storeFiles(List.of(file))).thenReturn(List.of(new UploadFile("test.jpg", "store.jpg")));

        itemImgService.updateItemImages(item, dto, List.of(file), 1L);

        verify(itemImgRepository).deleteAll(List.of(img));
        verify(itemImgRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private Item createItem() {
        return Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(Member.builder()
                        .id(1L)
                        .email("seller@example.com")
                        .memberName("판매자")
                        .memberPwd("pw")
                        .tel("01012341234")
                        .build())
                .build();
    }
}
