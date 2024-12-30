package kwh.cofshop.item.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ItemImgServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUploadItem() throws Exception {

        MockMultipartFile repImage = new MockMultipartFile(
                "repImage",
                "repImage.jpg",
                "image/jpeg",
                "Mock Representative Image".getBytes()
        );

        MockMultipartFile subImage1 = new MockMultipartFile(
                "subImages",
                "subImage1.jpg",
                "image/jpeg",
                "Mock Sub Image 1".getBytes()
        );

        MockMultipartFile subImage2 = new MockMultipartFile(
                "subImages",
                "subImage2.jpg",
                "image/jpeg",
                "Mock Sub Image 2".getBytes()
        );

        mockMvc.perform(multipart("/api/item/upload")
                        .file(repImage)
                        .file(subImage1)
                        .file(subImage2)
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk());
    }

}