package kwh.cofshop.item.service;

import kwh.cofshop.file.domain.FileStore;
import kwh.cofshop.file.domain.UploadFile;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.repository.ItemImgRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ItemImgService {

    private final FileStore fileStore;
    private final ItemImgRepository itemImgRepository;

    @Transactional
    public List<ItemImg> saveItemImages(Item item, Map<ItemImgRequestDto, MultipartFile> imgMap) throws IOException {
        List<ItemImg> itemImgs = new ArrayList<>();

        for (Map.Entry<ItemImgRequestDto, MultipartFile> entry : imgMap.entrySet()) {
            ItemImgRequestDto imgRequestDto = entry.getKey();
            MultipartFile file = entry.getValue();

            if (file != null && !file.isEmpty()) {
                // 파일 저장
                UploadFile uploadFile = fileStore.storeFile(file);

                // 이미지 엔티티 생성 및 추가
                itemImgs.add(ItemImg.createImg(
                        uploadFile.getStoreFileName(),
                        uploadFile.getUploadFileName(),
                        "/images/" + uploadFile.getStoreFileName(),
                        imgRequestDto.getImgType(),
                        item
                ));
            }
        }

        // DB 저장
        return itemImgRepository.saveAll(itemImgs);
    }




}