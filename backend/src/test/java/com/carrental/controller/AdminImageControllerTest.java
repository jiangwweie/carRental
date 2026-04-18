package com.carrental.controller;

import com.carrental.infrastructure.storage.ImageStorageService;
import com.carrental.infrastructure.storage.StorageResult;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * AdminImageController 单元测试
 * 使用纯 Mockito 测试，避免 Spring 上下文加载问题
 */
class AdminImageControllerTest {

    private ImageStorageService imageStorageService;
    private AdminImageController controller;

    @BeforeEach
    void setUp() {
        imageStorageService = Mockito.mock(ImageStorageService.class);
        controller = new AdminImageController(imageStorageService);
    }

    @Test
    @DisplayName("上传图片成功")
    void uploadImage_success() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes());

        StorageResult mockResult = new StorageResult(
                "/uploads/vehicles/2026/04/v1_1234567890_abc123.jpg",
                "v1_1234567890_abc123.jpg",
                19L,
                "image/jpeg"
        );

        Mockito.when(imageStorageService.store(any(), eq(1L))).thenReturn(mockResult);

        // Act
        var response = controller.upload(file, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("/uploads/vehicles/2026/04/v1_1234567890_abc123.jpg", response.getData().getUrl());
        assertEquals("v1_1234567890_abc123.jpg", response.getData().getFilename());
        assertEquals(19L, response.getData().getSize());
        assertEquals("image/jpeg", response.getData().getContentType());
    }

    @Test
    @DisplayName("上传图片成功 - 不带 vehicle_id")
    void uploadImage_withoutVehicleId() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "test image".getBytes());

        StorageResult mockResult = new StorageResult(
                "/uploads/vehicles/2026/04/v0_1234567890_xyz789.png",
                "v0_1234567890_xyz789.png",
                10L,
                "image/png"
        );

        Mockito.when(imageStorageService.store(any(), any())).thenReturn(mockResult);

        // Act
        var response = controller.upload(file, null);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertTrue(response.getData().getFilename().startsWith("v0_"));
    }

    @Test
    @DisplayName("上传成功后返回正确的响应结构")
    void uploadImage_correctResponseStructure() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.webp", "image/webp", "webp content".getBytes());

        StorageResult mockResult = new StorageResult(
                "/uploads/vehicles/2026/04/v1_1111111111_aaa111.webp",
                "v1_1111111111_aaa111.webp",
                12L,
                "image/webp"
        );

        Mockito.when(imageStorageService.store(any(), eq(1L))).thenReturn(mockResult);

        // Act
        var response = controller.upload(file, 1L);

        // Assert
        assertEquals("success", response.getMessage());
        assertNotNull(response.getData().getUrl());
        assertNotNull(response.getData().getFilename());
        assertTrue(response.getData().getSize() > 0);
        assertNotNull(response.getData().getContentType());
    }

    @Test
    @DisplayName("验证 store 方法被正确调用")
    void verifyStoreMethodCalled() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());

        StorageResult mockResult = new StorageResult(
                "/uploads/vehicles/2026/04/v1_test.jpg",
                "v1_test.jpg",
                7L,
                "image/jpeg"
        );

        Mockito.when(imageStorageService.store(any(), eq(5L))).thenReturn(mockResult);

        // Act
        controller.upload(file, 5L);

        // Verify
        Mockito.verify(imageStorageService).store(file, 5L);
    }
}
