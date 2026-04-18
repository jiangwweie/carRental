package com.carrental.infrastructure.storage;

import com.carrental.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地图片存储服务实现
 *
 * 存储策略：
 * - 目录结构: uploads/vehicles/YYYY/MM/
 * - 文件命名: v{vehicleId}_{timestamp}_{random6}.{ext}
 * - URL 格式: /uploads/vehicles/YYYY/MM/filename.ext
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "upload.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    @Value("${upload.local.base-path:uploads}")
    private String basePath;

    @Value("${upload.local.url-prefix:/uploads}")
    private String urlPrefix;

    private final ImageValidator imageValidator;
    private final SecureRandom random = new SecureRandom();

    public LocalImageStorageService(ImageValidator imageValidator) {
        this.imageValidator = imageValidator;
    }

    @Override
    public StorageResult store(MultipartFile file, Long vehicleId) throws StorageException {
        long startTime = System.currentTimeMillis();

        // 1. 校验文件
        imageValidator.validate(file);

        // 2. 生成文件名
        String extension = imageValidator.getExtension(file.getOriginalFilename());
        String filename = generateFilename(vehicleId, extension);

        // 3. 生成年月目录路径
        String yearMonthPath = generateYearMonthPath();
        Path targetDir = Paths.get(basePath, "vehicles", yearMonthPath).toAbsolutePath();

        // 4. 创建目录（如果不存在）
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("[IMAGE_UPLOAD_FAILED] 创建目录失败: {}", targetDir, e);
            throw new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "创建存储目录失败", e);
        }

        // 5. 保存文件
        Path targetFile = targetDir.resolve(filename);
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            log.error("[IMAGE_UPLOAD_FAILED] 保存文件失败: {}", targetFile, e);
            throw new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "保存文件失败", e);
        }

        // 6. 生成 URL
        String url = urlPrefix + "/vehicles/" + yearMonthPath + "/" + filename;

        // 7. 记录日志
        long duration = System.currentTimeMillis() - startTime;
        log.info("[IMAGE_UPLOAD_SUCCESS] vehicleId={}, url={}, size={}, duration={}ms",
                vehicleId, url, file.getSize(), duration);

        return new StorageResult(url, filename, file.getSize(), file.getContentType());
    }

    @Override
    public boolean delete(String url) throws StorageException {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // 从 URL 提取文件路径
        // URL 格式: /uploads/vehicles/2026/04/xxx.jpg
        if (!url.startsWith(urlPrefix)) {
            throw new StorageException(ErrorCode.PARAM_ERROR, "无效的图片 URL");
        }

        String relativePath = url.substring(urlPrefix.length() + 1); // 移除 "/uploads/"
        Path filePath = Paths.get(basePath, relativePath);

        // 安全检查：防止路径遍历攻击
        if (!filePath.normalize().startsWith(Paths.get(basePath).normalize())) {
            log.warn("[IMAGE_DELETE_SECURITY] 检测到路径遍历攻击尝试: {}", url);
            throw new StorageException(ErrorCode.FORBIDDEN, "非法访问");
        }

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("[IMAGE_DELETE_SUCCESS] url={}", url);
            }
            return deleted;
        } catch (IOException e) {
            log.error("[IMAGE_DELETE_FAILED] url={}, error={}", url, e.getMessage(), e);
            throw new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "删除文件失败", e);
        }
    }

    @Override
    public boolean exists(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        if (!url.startsWith(urlPrefix)) {
            return false;
        }

        String relativePath = url.substring(urlPrefix.length() + 1);
        Path filePath = Paths.get(basePath, relativePath);

        return Files.exists(filePath);
    }

    /**
     * 生成文件名
     * 格式: v{vehicleId}_{timestamp}_{random6}.{ext}
     */
    private String generateFilename(Long vehicleId, String extension) {
        long vid = (vehicleId != null && vehicleId > 0) ? vehicleId : 0;
        long timestamp = System.currentTimeMillis();
        String randomStr = generateRandomString(6);
        return String.format("v%d_%d_%s.%s", vid, timestamp, randomStr, extension);
    }

    /**
     * 生成年月目录路径
     * 格式: YYYY/MM
     */
    private String generateYearMonthPath() {
        LocalDate now = LocalDate.now();
        return String.format("%d/%02d", now.getYear(), now.getMonthValue());
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
