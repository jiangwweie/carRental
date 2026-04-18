package com.carrental.infrastructure.storage;

import com.carrental.common.result.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图片存储服务单元测试
 */
class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalImageStorageService storageService;
    private ImageValidator imageValidator;

    // 1x1 透明 PNG 图片 (base64)
    private static final String MINIMAL_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @BeforeEach
    void setUp() {
        // 创建 ImageValidator 并设置属性
        imageValidator = new ImageValidator();
        ReflectionTestUtils.setField(imageValidator, "maxSize", 5242880L); // 5MB
        ReflectionTestUtils.setField(imageValidator, "allowedTypes", "jpg,jpeg,png,webp");
        ReflectionTestUtils.setField(imageValidator, "minDimension", 100);
        ReflectionTestUtils.setField(imageValidator, "maxDimension", 4096);

        // 创建 LocalImageStorageService
        storageService = new LocalImageStorageService(imageValidator);
        ReflectionTestUtils.setField(storageService, "basePath", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "urlPrefix", "/uploads");
    }

    // ==================== ImageValidator Tests ====================

    @Nested
    @DisplayName("ImageValidator 校验测试")
    class ImageValidatorTests {

        @Test
        @DisplayName("正常 PNG 图片校验通过")
        void validPngImage_shouldPass() throws IOException {
            // 创建一个有效的 100x100 PNG 图片
            byte[] imageBytes = createTestPngImage(100, 100);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", imageBytes);

            assertDoesNotThrow(() -> imageValidator.validate(file));
        }

        @Test
        @DisplayName("文件为空时抛出 PARAM_ERROR")
        void emptyFile_shouldThrowParamError() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", new byte[0]);

            StorageException ex = assertThrows(StorageException.class,
                    () -> imageValidator.validate(file));
            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("文件类型不支持时抛出 FILE_TYPE_NOT_SUPPORTED")
        void invalidFileType_shouldThrowFileTypeNotSupported() throws IOException {
            byte[] imageBytes = createTestPngImage(100, 100);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.exe", "application/octet-stream", imageBytes);

            StorageException ex = assertThrows(StorageException.class,
                    () -> imageValidator.validate(file));
            assertEquals(ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("文件大小超限时抛出 FILE_SIZE_EXCEEDED")
        void fileTooLarge_shouldThrowFileSizeExceeded() throws IOException {
            // 创建一个超过 5MB 的文件 (模拟)
            byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", largeContent);

            StorageException ex = assertThrows(StorageException.class,
                    () -> imageValidator.validate(file));
            assertEquals(ErrorCode.FILE_SIZE_EXCEEDED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("图片尺寸过小时抛出 IMAGE_DIMENSION_INVALID")
        void imageTooSmall_shouldThrowDimensionInvalid() throws IOException {
            // 创建一个 50x50 的图片（小于最小 100x100）
            byte[] imageBytes = createTestPngImage(50, 50);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", imageBytes);

            StorageException ex = assertThrows(StorageException.class,
                    () -> imageValidator.validate(file));
            assertEquals(ErrorCode.IMAGE_DIMENSION_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("MIME 类型不支持时抛出 FILE_TYPE_NOT_SUPPORTED")
        void invalidMimeType_shouldThrowFileTypeNotSupported() throws IOException {
            byte[] imageBytes = createTestPngImage(100, 100);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "application/pdf", imageBytes);

            StorageException ex = assertThrows(StorageException.class,
                    () -> imageValidator.validate(file));
            assertEquals(ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode(), ex.getCode());
        }
    }

    // ==================== LocalImageStorageService Tests ====================

    @Nested
    @DisplayName("LocalImageStorageService 存储测试")
    class LocalImageStorageTests {

        @Test
        @DisplayName("成功存储图片并返回正确的 URL")
        void storeImage_success() throws IOException {
            byte[] imageBytes = createTestPngImage(100, 100);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", imageBytes);

            StorageResult result = storageService.store(file, 1L);

            assertNotNull(result);
            assertNotNull(result.getUrl());
            assertTrue(result.getUrl().startsWith("/uploads/vehicles/"));
            assertTrue(result.getUrl().endsWith(".png"));
            assertTrue(result.getFilename().startsWith("v1_"));
            assertEquals(imageBytes.length, result.getSize());
            assertEquals("image/png", result.getContentType());

            // 验证文件实际存在
            assertTrue(storageService.exists(result.getUrl()));
        }

        @Test
        @DisplayName("未关联车辆时 vehicleId 为 0")
        void storeImage_withoutVehicleId() throws IOException {
            byte[] imageBytes = createTestPngImage(100, 100);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", imageBytes);

            StorageResult result = storageService.store(file, null);

            assertTrue(result.getFilename().startsWith("v0_"));
        }

        @Test
        @DisplayName("文件按年月目录存储")
        void storeImage_organizedByYearMonth() throws IOException {
            byte[] imageBytes = createTestPngImage(100, 100);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", imageBytes);

            StorageResult result = storageService.store(file, 1L);

            // URL 格式: /uploads/vehicles/2026/04/filename.jpg
            assertTrue(result.getUrl().matches("/uploads/vehicles/\\d{4}/\\d{2}/.*\\.jpg"));
        }

        @Test
        @DisplayName("删除存在的文件")
        void deleteExistingFile_success() throws IOException {
            byte[] imageBytes = createTestPngImage(100, 100);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", imageBytes);

            StorageResult result = storageService.store(file, 1L);

            assertTrue(storageService.exists(result.getUrl()));
            assertTrue(storageService.delete(result.getUrl()));
            assertFalse(storageService.exists(result.getUrl()));
        }

        @Test
        @DisplayName("删除不存在的文件返回 false")
        void deleteNonExistingFile_returnsFalse() {
            assertFalse(storageService.delete("/uploads/vehicles/2026/04/nonexistent.png"));
        }

        @Test
        @DisplayName("exists 对不存在的文件返回 false")
        void exists_forNonExistingFile_returnsFalse() {
            assertFalse(storageService.exists("/uploads/vehicles/2026/04/nonexistent.png"));
        }

        @Test
        @DisplayName("exists 对无效 URL 返回 false")
        void exists_forInvalidUrl_returnsFalse() {
            assertFalse(storageService.exists(null));
            assertFalse(storageService.exists(""));
            assertFalse(storageService.exists("/invalid/path"));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 创建测试用的 PNG 图片字节数组
     * 使用简单的 PNG 结构生成指定尺寸的图片
     */
    private byte[] createTestPngImage(int width, int height) throws IOException {
        // 使用 Java 内置的 BufferedImage 创建测试图片
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);

        // 填充白色背景
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0xFFFFFFFF);
            }
        }

        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean written = javax.imageio.ImageIO.write(image, "png", baos);
        if (!written) {
            throw new IOException("Failed to write test image");
        }
        return baos.toByteArray();
    }
}
