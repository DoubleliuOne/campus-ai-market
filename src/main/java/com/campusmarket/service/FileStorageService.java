package com.campusmarket.service;

import com.campusmarket.config.StorageProperties;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.vo.UploadFileVO;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final StorageProperties storageProperties;
    private final Path imageRoot;

    public FileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.imageRoot = Path.of(storageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    public UploadFileVO storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > storageProperties.getMaxImageSize()) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new BusinessException("图片文件名不合法");
        }

        byte[] header;
        try {
            header = file.getInputStream().readNBytes(16);
        } catch (IOException ex) {
            throw new BusinessException("图片读取失败");
        }

        String detectedExtension = detectImageExtension(header);
        String declaredExtension = CONTENT_TYPE_EXTENSIONS.get(
                file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT));
        if (detectedExtension == null || declaredExtension == null) {
            throw new BusinessException("仅支持 JPG、PNG、WebP 或 GIF 图片");
        }
        if (!extensionsMatch(detectedExtension, declaredExtension)) {
            throw new BusinessException("图片内容与文件类型不一致");
        }

        String storedName = UUID.randomUUID().toString().replace("-", "")
                + "." + detectedExtension;
        Path target = resolveInsideRoot(storedName);

        try {
            Files.createDirectories(imageRoot);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException("图片保存失败，请稍后重试");
        }

        return new UploadFileVO(
                "/api/files/images/" + storedName,
                originalName,
                file.getSize()
        );
    }

    public Resource loadImage(String filename) {
        if (!StringUtils.hasText(filename) || filename.contains("..") || filename.contains("/")) {
            throw new BusinessException("图片不存在");
        }

        Path imagePath = resolveInsideRoot(filename);
        if (!Files.isRegularFile(imagePath)) {
            throw new BusinessException("图片不存在");
        }

        try {
            return new UrlResource(imagePath.toUri());
        } catch (IOException ex) {
            throw new BusinessException("图片读取失败");
        }
    }

    public Path getImageRoot() {
        return imageRoot;
    }

    private Path resolveInsideRoot(String filename) {
        Path resolved = imageRoot.resolve(filename).normalize();
        if (!resolved.startsWith(imageRoot)) {
            throw new BusinessException("图片路径不合法");
        }
        return resolved;
    }

    private String detectImageExtension(byte[] header) {
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47) {
            return "png";
        }
        if (header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return "webp";
        }
        if (header.length >= 6
                && header[0] == 'G'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == '8') {
            return "gif";
        }
        return null;
    }

    private boolean extensionsMatch(String detected, String declared) {
        if ("jpg".equals(detected)) {
            return "jpg".equals(declared);
        }
        return detected.equals(declared);
    }
}
