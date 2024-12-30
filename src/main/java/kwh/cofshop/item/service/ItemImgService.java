package kwh.cofshop.item.service;

import kwh.cofshop.file.domain.FileStore;
import kwh.cofshop.file.domain.UploadFile;
import kwh.cofshop.item.domain.ImgType;
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

@RequiredArgsConstructor
@Service
public class ItemImgService {

    private final FileStore fileStore; // 파일 저장 클래스
    private final ItemImgRepository itemImgRepository;

    @Transactional
    public List<ItemImg> saveItemImages(Item item, ItemImgRequestDto imgRequestDto) throws IOException {
        // 파일 저장
        List<UploadFile> uploadFiles = storeFiles(imgRequestDto);

        // ItemImg 생성
        List<ItemImg> itemImgs = uploadFiles.stream()
                .map(uploadFile -> ItemImg.createImg(
                        uploadFile.getStoreFileName(),
                        uploadFile.getUploadFileName(),
                        "/images/" + uploadFile.getStoreFileName(),
                        determineImgType(uploadFile, imgRequestDto), // 대표/서브 여부 결정
                        item
                ))
                .toList();

        // 저장 및 반환
        return itemImgRepository.saveAll(itemImgs);
    }

    private List<UploadFile> storeFiles(ItemImgRequestDto imgRequestDto) throws IOException {
        List<UploadFile> uploadFiles = new ArrayList<>();

        // 대표 이미지 처리
        MultipartFile repImage = imgRequestDto.getRepImage();
        if (repImage != null && !repImage.isEmpty()) {
            uploadFiles.add(fileStore.storeFile(repImage));
        }

        // 서브 이미지 처리
        List<MultipartFile> subImages = imgRequestDto.getSubImages();
        if (subImages != null && !subImages.isEmpty()) {
            uploadFiles.addAll(fileStore.storeFiles(subImages));
        }

        return uploadFiles;
    }

    private ImgType determineImgType(UploadFile uploadFile, ItemImgRequestDto imgRequestDto) {
        return imgRequestDto.getRepImage() != null &&
                uploadFile.getUploadFileName().equals(imgRequestDto.getRepImage().getOriginalFilename())
                ? ImgType.REPRESENTATIVE
                : ImgType.SUB;
    }

}