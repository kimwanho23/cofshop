package kwh.cofshop.file.controller;

import kwh.cofshop.file.dto.response.TempFileUploadResponseDto;
import kwh.cofshop.file.storage.TempUploadFileService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TempUploadFileService tempUploadFileService;

    @InjectMocks
    private FileController fileController;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcFactory.build(
                fileController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("임시 이미지 업로드")
    void uploadTempImages() throws Exception {
        when(tempUploadFileService.uploadTempFiles(anyLong(), anyList()))
                .thenReturn(List.of(new TempFileUploadResponseDto(
                        1L,
                        "temp.jpg",
                        "/images/temp.jpg",
                        LocalDateTime.now().plusHours(3)
                )));

        MockMultipartFile imageFile = new MockMultipartFile(
                "images",
                "temp.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "temp-data".getBytes()
        );

        mockMvc.perform(multipart("/api/files/temp")
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("임시 이미지 삭제")
    void deleteTempImage() throws Exception {
        mockMvc.perform(delete("/api/files/temp/1"))
                .andExpect(status().isNoContent());
    }
}
