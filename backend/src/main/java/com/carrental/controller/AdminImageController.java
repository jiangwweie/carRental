package com.carrental.controller;

import com.carrental.common.result.ApiResponse;
import com.carrental.infrastructure.storage.StorageException;
import com.carrental.infrastructure.storage.StorageResult;
import com.carrental.infrastructure.storage.ImageStorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理员图片上传控制器
 *
 * 接口：
 * - POST /api/v1/admin/images/upload - 上传图片
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/images")
@RequiredArgsConstructor
public class AdminImageController {

    private final ImageStorageService imageStorageService;

    /**
     * 上传图片
     *
     * @param file      图片文件
     * @param vehicleId 关联车辆 ID（可选，用于目录组织）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ApiResponse<UploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId) {

        log.info("[IMAGE_UPLOAD_REQUEST] vehicleId={}, filename={}, size={}, contentType={}",
                vehicleId, file.getOriginalFilename(), file.getSize(), file.getContentType());

        StorageResult result = imageStorageService.store(file, vehicleId);

        UploadResult uploadResult = new UploadResult();
        uploadResult.setUrl(result.getUrl());
        uploadResult.setFilename(result.getFilename());
        uploadResult.setSize(result.getSize());
        uploadResult.setContentType(result.getContentType());

        return ApiResponse.success(uploadResult);
    }

    /**
     * 上传结果
     */
    @Data
    public static class UploadResult {
        /**
         * 图片相对 URL
         */
        private String url;

        /**
         * 文件名
         */
        private String filename;

        /**
         * 文件大小（字节）
         */
        private long size;

        /**
         * MIME 类型
         */
        private String contentType;
    }
}
