package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.security.LoginUser;
import com.campusmarket.service.FileStorageService;
import com.campusmarket.vo.UploadFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping("/api/files")
@Tag(name = "文件", description = "商品图片上传与访问")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传商品图片", description = "支持 JPG、PNG、WebP、GIF，单张不超过 5MB")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UploadFileVO> uploadImage(
            @Parameter(description = "图片文件")
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(fileStorageService.storeImage(file));
    }

    @GetMapping("/images/{filename:.+}")
    @Operation(summary = "读取商品图片")
    public ResponseEntity<Resource> getImage(
            @PathVariable
            @Pattern(regexp = "^[a-f0-9]{32}\\.(jpg|png|webp|gif)$",
                    message = "图片地址不合法")
            String filename) {
        Resource resource = fileStorageService.loadImage(filename);
        return ResponseEntity.ok()
                .contentType(mediaType(filename))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    private MediaType mediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.IMAGE_JPEG;
    }
}
