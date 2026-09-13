package com.campusmarket.service;

import com.campusmarket.config.StorageProperties;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.vo.UploadFileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreValidPngImage() {
        FileStorageService service = createService();
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "item.png", "image/png", png);

        UploadFileVO result = service.storeImage(file);

        assertTrue(result.getUrl().startsWith("/api/files/images/"));
        assertTrue(result.getUrl().endsWith(".png"));
        assertTrue(Files.isRegularFile(service.getImageRoot()
                .resolve(result.getUrl().substring(result.getUrl().lastIndexOf('/') + 1))));
    }

    @Test
    void shouldRejectFileWhoseContentDoesNotMatchType() {
        FileStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not-an-image".getBytes());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.storeImage(file));

        assertEquals("仅支持 JPG、PNG、WebP 或 GIF 图片", exception.getMessage());
    }

    private FileStorageService createService() {
        StorageProperties properties = new StorageProperties();
        properties.setUploadDir(tempDir.resolve("uploads").toString());
        properties.setMaxImageSize(1024);
        return new FileStorageService(properties);
    }
}
