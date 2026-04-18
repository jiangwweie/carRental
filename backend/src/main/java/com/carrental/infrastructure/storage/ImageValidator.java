package com.carrental.infrastructure.storage;

import com.carrental.common.result.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 图片校验器
 *
 * 校验规则：
 * - 文件大小：最大 5MB
 * - 文件类型：jpg, jpeg, png, webp（扩展名 + MIME 双重校验）
 * - 图片尺寸：100x100 ~ 4096x4096
 */
@Component
public class ImageValidator {

    @Value("${upload.limits.max-size:5242880}")
    private long maxSize;

    @Value("${upload.limits.allowed-types:jpg,jpeg,png,webp}")
    private String allowedTypes;

    @Value("${upload.limits.min-dimension:100}")
    private int minDimension;

    @Value("${upload.limits.max-dimension:4096}")
    private int maxDimension;

    // 允许的 MIME 类型
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/octet-stream"  // 某些系统会将webp识别为octet-stream
    ));

    /**
     * 校验上传的图片文件
     *
     * @param file 上传的文件
     * @throws StorageException 校验失败时抛出
     */
    public void validate(MultipartFile file) throws StorageException {
        // 1. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new StorageException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        // 2. 检查文件大小
        if (file.getSize() > maxSize) {
            throw new StorageException(ErrorCode.FILE_SIZE_EXCEEDED,
                    String.format("文件大小超过限制: 最大 %dMB", maxSize / 1024 / 1024));
        }

        // 3. 获取并校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new StorageException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "无法获取文件名");
        }
        String extension = getExtension(originalFilename);
        if (!isAllowedExtension(extension)) {
            throw new StorageException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    String.format("不支持的文件类型: %s，仅支持: %s", extension, allowedTypes));
        }

        // 4. 检查 MIME 类型
        String contentType = file.getContentType();
        if (!isValidMimeType(contentType)) {
            throw new StorageException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    String.format("不支持的 MIME 类型: %s", contentType));
        }

        // 5. 检查图片尺寸（需要实际读取图片）
        // 注意: Java标准库不支持webp,跳过webp的尺寸校验
        if (!"webp".equals(extension)) {
            try {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                    throw new StorageException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "无法读取图片，可能不是有效的图片文件");
                }
                int width = image.getWidth();
                int height = image.getHeight();
                if (width < minDimension || height < minDimension ||
                        width > maxDimension || height > maxDimension) {
                    throw new StorageException(ErrorCode.IMAGE_DIMENSION_INVALID,
                            String.format("图片尺寸 %dx%d 不符合要求，需要在 %dx%d 到 %dx%d 之间",
                                    width, height, minDimension, minDimension, maxDimension, maxDimension));
                }
            } catch (IOException e) {
                throw new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "读取图片失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 获取文件扩展名（小写）
     */
    public String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 检查扩展名是否允许
     */
    private boolean isAllowedExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String[] allowed = allowedTypes.split(",");
        for (String type : allowed) {
            if (type.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 MIME 类型是否有效
     */
    private boolean isValidMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase());
    }
}
